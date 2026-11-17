package grid;

import base.ClienteHttpDba;
import base.Configuracion;
import base.Entorno;
import base.InfoReto;
import base.LecturaSensor;
import base.RespuestaAccion;
import base.Visualizador;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Entorno específico para los retos del bloque Grid (2D).
 * Gestiona el descubrimiento del mapa, la validación de sensores y acciones,
 * el arranque del run, la lectura de sensores (posición, bumpers, objetivo y energía)
 * y el envío de movimientos cardinales a DBA Lab.
 * Delega la presentación y la selección interactiva de mapas en el {@link Visualizador}.
 */
public class EntornoGrid extends Entorno<SensoresGrid, Direccion> {

    // Identificadores oficiales de sensores en DBA Lab
    public static final String SENSOR_POSITION = "grid.position";
    public static final String SENSOR_BUMPERS = "grid.bumpers";
    public static final String SENSOR_TARGET = "grid.target";
    public static final String SENSOR_ENERGY = "grid.energy";

    // Objetivo cacheado durante el run (invariante para un mapa determinado)
    private Posicion objetivo;
    private SensoresGrid ultimosSensores;
    private final Optional<Long> challengeIdPreferido;

    // --- Constructores ---

    /**
     * Constructor por defecto: lee configuración de "config.properties" y usa el visualizador CLI.
     */
    public EntornoGrid() {
        this(new ClienteHttpDba(), new VisualizadorGridCli(), new Configuracion().getChallengeId());
    }

    /**
     * Constructor con reto numérico específico.
     *
     * @param challengeId identificador del reto de Grid seleccionado
     */
    public EntornoGrid(long challengeId) {
        this(new ClienteHttpDba(), new VisualizadorGridCli(), Optional.of(challengeId));
    }

    /**
     * Constructor con cliente HTTP explícito y visualizador CLI predeterminado.
     *
     * @param cliente cliente HTTP configurado
     */
    public EntornoGrid(ClienteHttpDba cliente) {
        this(cliente, new VisualizadorGridCli(), Optional.empty());
    }

    /**
     * Constructor completo con inyección de cliente, visualizador y reto opcional.
     *
     * @param cliente              cliente HTTP configurado
     * @param visualizador         visualizador de eventos del entorno (CLI o GUI)
     * @param challengeIdPreferido identificador del reto a ejecutar
     */
    public EntornoGrid(
            ClienteHttpDba cliente,
            Visualizador<SensoresGrid, Direccion> visualizador,
            Optional<Long> challengeIdPreferido) {
        super(cliente, visualizador);
        this.challengeIdPreferido = Objects.requireNonNull(challengeIdPreferido, "El challengeIdPreferido no puede ser nulo");
    }

    // --- Implementación de Entorno ---

    @Override
    public void iniciarRun() throws IOException, InterruptedException {
        InfoReto reto = seleccionarRetoGrid();
        validarCapacidades(reto);

        visualizador.mostrarRetoEncontrado(reto);
        this.estadoActual = cliente.iniciarRun(reto.id());
        this.runId = estadoActual.runId();

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
            if (lecturaBumpers.getBoolean(d.campoBumper())) {
                bloqueadas.add(d);
            }
        }

        // 3. Energía restante y modo
        LecturaSensor lecturaEnergia = cliente.consultarSensor(runId, SENSOR_ENERGY);
        this.estadoActual = lecturaEnergia.control();
        String modoEnergia = lecturaEnergia.getString("mode");
        int energiaRestante = lecturaEnergia.getInt("remaining");

        SensoresGrid sensores = new SensoresGrid(
                posActual,
                objetivo,
                bloqueadas,
                modoEnergia,
                energiaRestante
        );

        this.ultimosSensores = sensores;
        visualizador.mostrarPercepcion(sensores);
        return sensores;
    }

    @Override
    public void ejecutarAccion(Direccion accion) throws IOException, InterruptedException {
        RespuestaAccion respuesta = cliente.enviarAccion(runId, accion.codigoAccion());
        this.estadoActual = respuesta.control();

        visualizador.mostrarAccion(accion, respuesta.actionStatus(), estadoActual);

        if (!respuesta.isAplicada()) {
            String motivo = respuesta.motivo().orElse("Desconocido");
            throw new IllegalStateException("Movimiento rechazado por DBA Lab: " + motivo);
        }
    }

    /**
     * Comprueba si el agente ha alcanzado la celda objetivo en la última percepción.
     */
    public boolean isEnObjetivo() {
        return ultimosSensores != null && ultimosSensores.enObjetivo();
    }

    // --- Selección y Validación de Retos Grid ---

    private InfoReto seleccionarRetoGrid() throws IOException, InterruptedException {
        List<InfoReto> retos = cliente.getRetos();
        List<InfoReto> retosGrid = retos.stream()
                .filter(r -> "GRID_2D".equalsIgnoreCase(r.environmentType()))
                .toList();

        if (retosGrid.isEmpty()) {
            throw new IllegalStateException("No se han encontrado retos de tipo GRID_2D disponibles en DBA Lab.");
        }

        // Si se configuró un ID preferido (p. ej. en config.properties), intentamos usarlo
        if (challengeIdPreferido.isPresent()) {
            long preferido = challengeIdPreferido.get();
            return retosGrid.stream()
                    .filter(r -> r.id() == preferido)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "El reto GRID_2D configurado (" + preferido + ") no está disponible en el servidor."));
        }

        // Si solo hay un mapa disponible, se selecciona directamente
        if (retosGrid.size() == 1) {
            return retosGrid.get(0);
        }

        // Si hay varios mapas y no hay reto fijado en la configuración, delegamos la selección en el visualizador
        return visualizador.seleccionarReto(retosGrid);
    }

    private void validarCapacidades(InfoReto reto) {
        List<String> sensoresNecesarios = List.of(
                SENSOR_POSITION,
                SENSOR_BUMPERS,
                SENSOR_TARGET,
                SENSOR_ENERGY
        );

        for (String sensor : sensoresNecesarios) {
            if (!reto.contieneSensor(sensor)) {
                throw new IllegalStateException("Sensor requerido no disponible en el reto: " + sensor);
            }
        }

        for (Direccion dir : Direccion.values()) {
            if (!reto.contieneAccion(dir.codigoAccion())) {
                throw new IllegalStateException("Acción requerida no disponible en el reto: " + dir.codigoAccion());
            }
        }
    }
}
