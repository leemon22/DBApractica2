package termostato;

import base.ClienteHttpDba;
import base.Entorno;
import base.EstadoControl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;

/**
 * Entorno del termostato: descubre el reto, inicia el run, lee sensores y envía acciones a DBA Lab.
 */
public class EntornoTermostato implements Entorno<SensoresTermostato, AccionTermostato> {

    private final ClienteHttpDba cliente;
    private EstadoControl estadoActual;
    private String runId;

    // Parámetros invariantes del reto (se leen una sola vez al inicio para optimizar la red)
    private double limiteInferior;
    private double limiteSuperior;
    private double temperaturaObjetivo;

    // --- Constructores ---

    /**
     * Constructor por defecto: inicializa el cliente HTTP a partir de config.properties.
     */
    public EntornoTermostato() {
        this(new ClienteHttpDba());
    }

    /**
     * Constructor con cliente HTTP explícito.
     *
     * @param cliente cliente HTTP configurado
     */
    public EntornoTermostato(ClienteHttpDba cliente) {
        this.cliente = cliente;
    }

    // --- Validación de Capacidades y Consultas Internas ---

    /**
     * Valida que el reto de termostato anunciado por DBA Lab incluya todos los sensores y acciones requeridos.
     */
    private void validarCapacidades(JsonObject reto) {
        JsonArray sensores = reto.getAsJsonArray("sensorCodes");
        JsonArray acciones = reto.getAsJsonArray("actionCodes");

        String[] sensoresNecesarios = {
            "thermostat.temperature",
            "thermostat.limits",
            "thermostat.target"
        };

        String[] accionesNecesarias = {
            "thermostat.refrigeration.on",
            "thermostat.refrigeration.off"
        };

        for (String sensor : sensoresNecesarios) {
            boolean disponible = false;
            for (JsonElement elemento : sensores) {
                if (sensor.equals(elemento.getAsString())) {
                    disponible = true;
                    break;
                }
            }
            if (!disponible) {
                throw new IllegalStateException("Sensor no disponible: " + sensor);
            }
        }

        for (String accion : accionesNecesarias) {
            boolean disponible = false;
            for (JsonElement elemento : acciones) {
                if (accion.equals(elemento.getAsString())) {
                    disponible = true;
                    break;
                }
            }
            if (!disponible) {
                throw new IllegalStateException("Accion no disponible: " + accion);
            }
        }
    }

    /**
     * Consulta una percepción individual en DBA Lab, actualiza el estado oficial y retorna el valor.
     */
    private JsonObject consultarSensor(String sensorId) throws IOException, InterruptedException {
        JsonObject respuesta = cliente.enviarPercepcion(runId, sensorId);
        estadoActual = new EstadoControl(respuesta.getAsJsonObject("control"));
        return respuesta.getAsJsonObject("value");
    }

    // --- Implementación de Entorno ---

    @Override
    public void iniciarRun() throws IOException, InterruptedException {
        JsonObject json = cliente.getRetos();
        JsonArray retos = json.getAsJsonArray("challenges");

        boolean encontrado = false;

        for (JsonElement elemento : retos) {
            JsonObject reto = elemento.getAsJsonObject();
            String tipo = reto.get("environmentType").getAsString();

            if ("THERMOSTAT".equals(tipo)) {
                validarCapacidades(reto);

                long challengeId = reto.get("id").getAsLong();
                System.out.println("Reto termostato encontrado: " + challengeId);

                JsonObject respuestaInicio = cliente.iniciarRun(challengeId);
                estadoActual = new EstadoControl(respuestaInicio.getAsJsonObject("control"));
                runId = estadoActual.runId();

                System.out.println("Run iniciado: " + runId);
                System.out.println("Estado: " + estadoActual.runStatus());

                // Leemos los parámetros invariantes del reto una sola vez al arrancar
                JsonObject limites = consultarSensor("thermostat.limits");
                JsonObject objetivo = consultarSensor("thermostat.target");
                this.limiteInferior = limites.get("lower").getAsDouble();
                this.limiteSuperior = limites.get("upper").getAsDouble();
                this.temperaturaObjetivo = objetivo.get("celsius").getAsDouble();

                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            throw new IllegalStateException("No hay retos de termostato disponibles");
        }
    }

    @Override
    public SensoresTermostato leerSensores() throws IOException, InterruptedException {
        JsonObject temperatura = consultarSensor("thermostat.temperature");
        double tempActual = temperatura.get("celsius").getAsDouble();

        SensoresTermostato sensores = new SensoresTermostato(
                tempActual,
                limiteInferior,
                limiteSuperior,
                temperaturaObjetivo
        );

        System.out.println("\n--- PERCEPCION DEL AGENTE ---");
        System.out.println("Temperatura actual: " + sensores.temperaturaActual() + " °C");
        System.out.println("Limites permitidos: [" + sensores.limiteInferior() + ", " + sensores.limiteSuperior() + "] °C");
        System.out.println("Temperatura objetivo: " + sensores.temperaturaObjetivo() + " °C");

        return sensores;
    }

    @Override
    public EstadoControl ejecutarAccion(AccionTermostato accion) throws IOException, InterruptedException {
        JsonObject respuesta = cliente.enviarAccion(runId, accion.getCodigoAccion());
        String resultado = respuesta.get("actionStatus").getAsString();

        estadoActual = new EstadoControl(respuesta.getAsJsonObject("control"));

        System.out.println(
                "Accion: " + accion
                + " | Resultado: " + resultado
                + " | Estado: " + estadoActual.runStatus()
        );

        if ("REJECTED".equals(resultado)) {
            String motivo = respuesta.has("reasonCode") && !respuesta.get("reasonCode").isJsonNull()
                    ? respuesta.get("reasonCode").getAsString()
                    : "Desconocido";
            throw new IllegalStateException("Accion rechazada por DBA Lab: " + motivo);
        }

        return estadoActual;
    }

    @Override
    public EstadoControl getEstadoActual() {
        return estadoActual;
    }

    @Override
    public void cerrarRun() throws IOException, InterruptedException {
        if (runId != null && estadoActual != null && estadoActual.isActivo()) {
            System.out.println("Cerrando run activo en DBA Lab: " + runId);
            cliente.cerrarRun(runId);
        }
    }
}
