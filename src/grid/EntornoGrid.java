package grid;

import base.ClienteHttpDba;
import base.Configuracion;
import base.Entorno;
import base.InfoReto;
import base.LecturaSensor;
import base.RespuestaAccion;
import base.Visualizador;

import grid.control.ControladorGrid;
import grid.vista.FabricaPresentacionGrid;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Entorno específico para los retos del bloque Grid (2D). Gestiona el descubrimiento del mapa, la
 * validación de capacidades anunciadas, el arranque del run, la lectura de sensores (posición,
 * bumpers, objetivo y energía) y el envío de movimientos cardinales a DBA Lab. Delega la
 * presentación y la selección interactiva de mapas en el {@link Visualizador}.
 */
public class EntornoGrid extends Entorno<SensoresGrid, Direccion> {

    // Identificadores oficiales de sensores en DBA Lab
    public static final String SENSOR_POSITION = "grid.position";
    public static final String SENSOR_BUMPERS = "grid.bumpers";
    public static final String SENSOR_TARGET = "grid.target";
    public static final String SENSOR_ENERGY = "grid.energy";

    // Traducción de acciones semánticas y sensores a protocolos de red de DBA Lab
    private static final Map<Direccion, String> MAPA_ACCIONES =
            Map.of(
                    Direccion.NORTE, "grid.move.north",
                    Direccion.ESTE, "grid.move.east",
                    Direccion.SUR, "grid.move.south",
                    Direccion.OESTE, "grid.move.west");

    private static final Map<Direccion, String> MAPA_BUMPERS =
            Map.of(
                    Direccion.NORTE, "north",
                    Direccion.ESTE, "east",
                    Direccion.SUR, "south",
                    Direccion.OESTE, "west");

    // Objetivo cacheado durante el run (invariante para un mapa determinado)
    private Posicion objetivo;
    private SensoresGrid ultimosSensores;
    private boolean finNotificado;
    private final Optional<Long> challengeIdPreferido;
    // Solo existe en Swing: coordina botones y espera de nuevas ejecuciones.
    private ControladorGrid control;

    /** Recibe la presentación creada por iniciar(), mediante construcción Java ordinaria. */
    public EntornoGrid(FabricaPresentacionGrid.Presentacion presentacion) {
        this(new Configuracion(), presentacion);
    }

    private EntornoGrid(Configuracion config, FabricaPresentacionGrid.Presentacion presentacion) {
        this(
                new ClienteHttpDba(config),
                presentacion.vista(),
                presentacion.control() == null ? config.getChallengeId() : Optional.empty());
        this.control = presentacion.control();
    }

    // Permite componer/probar el entorno con contratos ordinarios, nunca con argumentos JADE.
    public EntornoGrid(
            ClienteHttpDba cliente, ControladorGrid.Vista vista, ControladorGrid control) {
        this(cliente, vista, Optional.empty());
        this.control = control;
    }

    public boolean esInteractivo() {
        return control != null;
    }

    public boolean paradaSolicitada() {
        return control != null && control.paradaSolicitada();
    }

    public boolean salidaSolicitada() {
        return control != null && control.salidaSolicitada();
    }

    public void solicitarSalida() {
        if (control != null) control.finalizar();
    }

    public void bloquearReinicio(String motivo) {
        if (control != null) control.bloquear(motivo);
    }

    public void cerrarPresentacion(Throwable fallo) {
        if (control != null) control.cerrarVentana(fallo);
    }

    /** Cierra y notifica el fin en un solo lugar, también si el run ya era terminal. */
    public void terminarEjecucion() throws IOException, InterruptedException {
        cerrarRun();
        if (isActivo()) throw new IOException("El run sigue activo tras solicitar el cierre");
        if (!finNotificado && estadoActual != null) {
            visualizador.mostrarFin(estadoActual);
            finNotificado = true;
        }
        if (control != null) control.enEspera("Ejecución terminada. Puedes reiniciar o finalizar.");
    }

    /**
     * Constructor completo con inyección de cliente, visualizador y reto opcional.
     *
     * @param cliente cliente HTTP configurado
     * @param visualizador visualizador de eventos del entorno (CLI o GUI)
     * @param challengeIdPreferido identificador del reto a ejecutar
     */
    public EntornoGrid(
            ClienteHttpDba cliente,
            Visualizador<SensoresGrid, Direccion> visualizador,
            Optional<Long> challengeIdPreferido) {
        super(cliente, visualizador);
        this.challengeIdPreferido =
                Objects.requireNonNull(
                        challengeIdPreferido, "El challengeIdPreferido no puede ser nulo");
    }

    /** En Swing se puede cancelar y volver a elegir; en consola se inicia una sola vez. */
    @Override
    public void iniciarRun() throws IOException, InterruptedException {
        if (control == null) {
            iniciarReto();
            return;
        }
        while (control.esperarInicio()) {
            try {
                iniciarReto();
                control.iniciada();
                return;
            } catch (ControladorGrid.SeleccionCancelada cancelada) {
                control.enEspera("Selección cancelada. Puedes elegir otro nivel.");
            } catch (Exception e) {
                try {
                    cerrarRun();
                    control.enEspera("No se pudo iniciar: " + e.getMessage());
                } catch (Exception cierre) {
                    control.bloquear("No se pudo cerrar el run: " + cierre.getMessage());
                }
            }
        }
    }

    private void iniciarReto() throws IOException, InterruptedException {
        ultimosSensores = null;
        objetivo = null;
        InfoReto reto =
                seleccionarReto("GRID_2D", challengeIdPreferido)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No hay retos de tipo GRID_2D disponibles en DBA Lab"));

        validarCapacidades(reto);

        visualizador.mostrarRetoEncontrado(reto);
        this.estadoActual = cliente.iniciarRun(reto.id());
        this.runId = estadoActual.runId();
        finNotificado = false;

        visualizador.mostrarInicio(runId, estadoActual);

        // Lectura única del objetivo del mapa (invariante durante todo el run)
        LecturaSensor lecturaTarget = cliente.consultarSensor(runId, SENSOR_TARGET);
        this.objetivo = new Posicion(lecturaTarget.getInt("x"), lecturaTarget.getInt("y"));
    }

    @Override
    public SensoresGrid leerSensores() throws IOException, InterruptedException {
        // 1. Posición actual
        LecturaSensor lecturaPos = cliente.consultarSensor(runId, SENSOR_POSITION);
        this.estadoActual = lecturaPos.control();
        Posicion posActual = new Posicion(lecturaPos.getInt("x"), lecturaPos.getInt("y"));

        // 2. Bumpers de colisión en las 4 direcciones
        LecturaSensor lecturaBumpers = cliente.consultarSensor(runId, SENSOR_BUMPERS);
        this.estadoActual = lecturaBumpers.control();
        Set<Direccion> bloqueadas = new HashSet<>();
        for (Direccion d : Direccion.values()) {
            if (lecturaBumpers.getBoolean(MAPA_BUMPERS.get(d))) {
                bloqueadas.add(d);
            }
        }

        // 3. Energía restante y modo
        LecturaSensor lecturaEnergia = cliente.consultarSensor(runId, SENSOR_ENERGY);
        this.estadoActual = lecturaEnergia.control();
        String modoEnergia = lecturaEnergia.getString("mode");
        int energiaRestante = lecturaEnergia.getInt("remaining");

        SensoresGrid sensores =
                new SensoresGrid(posActual, objetivo, bloqueadas, modoEnergia, energiaRestante);

        this.ultimosSensores = sensores;
        visualizador.mostrarPercepcion(sensores);
        return sensores;
    }

    @Override
    public void ejecutarAccion(Direccion accion) throws IOException, InterruptedException {
        if (paradaSolicitada())
            throw new PoliticaGridV2.ParadaNavegacion("Parada solicitada desde la presentación");
        String codigoAccion = MAPA_ACCIONES.get(accion);
        RespuestaAccion respuesta = cliente.enviarAccion(runId, codigoAccion);
        this.estadoActual = respuesta.control();

        visualizador.mostrarAccion(accion, respuesta.actionStatus(), estadoActual);

        if (!respuesta.isAplicada()) {
            String motivo = respuesta.motivo().orElse("Desconocido");
            throw new IllegalStateException("Movimiento rechazado por DBA Lab: " + motivo);
        }
    }

    /** Comprueba si el agente ha alcanzado la celda objetivo en la última percepción. */
    public boolean isEnObjetivo() {
        return ultimosSensores != null && ultimosSensores.enObjetivo();
    }

    private void validarCapacidades(InfoReto reto) {
        reto.validarCapacidades(
                List.of(SENSOR_POSITION, SENSOR_BUMPERS, SENSOR_TARGET, SENSOR_ENERGY),
                MAPA_ACCIONES.values());
    }
}
