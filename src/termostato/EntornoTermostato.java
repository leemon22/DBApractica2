package termostato;

import base.ClienteHttpDba;
import base.Configuracion;
import base.Entorno;
import base.InfoReto;
import base.LecturaSensor;
import base.RespuestaAccion;
import base.Visualizador;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entorno específico para el reto del termostato. Gestiona el descubrimiento del reto, el arranque
 * del run, la consulta de percepciones físicas de temperatura y la traducción de órdenes semánticas
 * a códigos de red de DBA Lab.
 */
public class EntornoTermostato extends Entorno<SensoresTermostato, AccionTermostato> {

    // Códigos oficiales de la API de DBA Lab
    private static final String SENSOR_TEMPERATURA = "thermostat.temperature";
    private static final String SENSOR_LIMITES = "thermostat.limits";
    private static final String SENSOR_OBJETIVO = "thermostat.target";
    private static final String ACCION_REFRIGERACION_ON = "thermostat.refrigeration.on";
    private static final String ACCION_REFRIGERACION_OFF = "thermostat.refrigeration.off";

    // Parámetros invariantes del reto agrupados en un record (leídos una sola vez al inicio)
    private LimitesTermostato limites;
    private final Optional<Long> challengeIdPreferido;

    /**
     * Constructor por defecto: lee configuración de "config.properties" y usa la vista de consola.
     */
    public EntornoTermostato() {
        this(new Configuracion());
    }

    private EntornoTermostato(Configuracion config) {
        this(new ClienteHttpDba(config), new VisualizadorTermostato(), config.getChallengeId());
    }

    /**
     * Constructor con cliente HTTP explícito y visualizador de consola predeterminado.
     *
     * @param cliente cliente HTTP configurado
     */
    public EntornoTermostato(ClienteHttpDba cliente) {
        this(cliente, new VisualizadorTermostato(), Optional.empty());
    }

    /**
     * Constructor completo con inyección de todas las dependencias.
     *
     * @param cliente cliente HTTP configurado
     * @param visualizador visualizador de eventos del entorno
     * @param challengeIdPreferido identificador de reto opcional
     */
    public EntornoTermostato(
            ClienteHttpDba cliente,
            Visualizador<SensoresTermostato, AccionTermostato> visualizador,
            Optional<Long> challengeIdPreferido) {
        super(cliente, visualizador);
        this.challengeIdPreferido =
                Objects.requireNonNull(
                        challengeIdPreferido, "El challengeIdPreferido no puede ser nulo");
    }

    @Override
    public void iniciarRun() throws IOException, InterruptedException {
        InfoReto reto =
                seleccionarReto("THERMOSTAT", challengeIdPreferido)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No hay retos de termostato compatibles disponibles en DBA"
                                                + " Lab"));

        validarCapacidades(reto);

        visualizador.mostrarRetoEncontrado(reto);
        this.estadoActual = cliente.iniciarRun(reto.id());
        this.runId = estadoActual.runId();

        visualizador.mostrarInicio(runId, estadoActual);

        // Lectura única de parámetros invariantes al inicio del reto
        LecturaSensor lecturaLimites = cliente.consultarSensor(runId, SENSOR_LIMITES);
        LecturaSensor lecturaObjetivo = cliente.consultarSensor(runId, SENSOR_OBJETIVO);

        this.limites =
                new LimitesTermostato(
                        lecturaLimites.getDouble("lower"),
                        lecturaLimites.getDouble("upper"),
                        lecturaObjetivo.getDouble("celsius"));
    }

    @Override
    public SensoresTermostato leerSensores() throws IOException, InterruptedException {
        LecturaSensor lecturaTemp = cliente.consultarSensor(runId, SENSOR_TEMPERATURA);
        this.estadoActual = lecturaTemp.control();

        SensoresTermostato sensores =
                new SensoresTermostato(lecturaTemp.getDouble("celsius"), limites);

        visualizador.mostrarPercepcion(sensores);
        return sensores;
    }

    @Override
    public void ejecutarAccion(AccionTermostato accion) throws IOException, InterruptedException {
        String codigoAccion = traducirAccion(accion);
        RespuestaAccion respuesta = cliente.enviarAccion(runId, codigoAccion);
        this.estadoActual = respuesta.control();

        visualizador.mostrarAccion(accion, respuesta.actionStatus(), estadoActual);

        if (!respuesta.isAplicada()) {
            String motivo = respuesta.motivo().orElse("Desconocido");
            throw new IllegalStateException("Acción rechazada por DBA Lab: " + motivo);
        }
    }

    private String traducirAccion(AccionTermostato accion) {
        return switch (accion) {
            case ON -> ACCION_REFRIGERACION_ON;
            case OFF -> ACCION_REFRIGERACION_OFF;
        };
    }

    private void validarCapacidades(InfoReto reto) {
        reto.validarCapacidades(
                List.of(SENSOR_TEMPERATURA, SENSOR_LIMITES, SENSOR_OBJETIVO),
                List.of(ACCION_REFRIGERACION_ON, ACCION_REFRIGERACION_OFF));
    }
}
