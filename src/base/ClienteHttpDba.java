package base;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Cliente HTTP encargado de la comunicación con la API REST de DBA Lab.
 * <p>
 * Centraliza y oculta todos los detalles de bajo nivel:
 * <ul>
 *   <li>Autenticación Bearer mediante cabecera HTTP estándar.</li>
 *   <li>Protección de sobrecarga en el servidor aplicando un retardo configurable entre peticiones.</li>
 *   <li>Timeouts de red (10s de conexión y 15s por petición) para evitar bloqueos indefinidos.</li>
 *   <li>Serialización de peticiones y deserialización de respuestas JSON a objetos de datos Java puros.</li>
 * </ul>
 */
public class ClienteHttpDba {

    /** Retardo predeterminado de 500 ms entre peticiones consecutivas para proteger el servidor */
    public static final long DELAY_PREDETERMINADO_MS = 500;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String token;
    private long tiempoEsperaMs;

    // --- Constructores ---

    /**
     * Constructor por defecto: lee la URL y el token desde "config.properties".
     */
    public ClienteHttpDba() {
        this(new Configuracion());
    }

    /**
     * Construye el cliente a partir de una instancia de {@link Configuracion}.
     *
     * @param config configuración con la URL y el token cargados
     */
    public ClienteHttpDba(Configuracion config) {
        this(config.getUrlLaboratorio(), config.getToken());
    }

    /**
     * Construye el cliente con una URL base y un token explícitos.
     *
     * @param baseUrl URL base de la API (debe terminar en /api/v1)
     * @param token   token personal de autenticación Bearer
     */
    public ClienteHttpDba(String baseUrl, String token) {
        Objects.requireNonNull(baseUrl, "La URL base no puede ser nula");
        Objects.requireNonNull(token, "El token no puede ser nulo");

        // Normalizamos la URL eliminando posibles barras finales accidentales
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token.trim();
        this.tiempoEsperaMs = DELAY_PREDETERMINADO_MS;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // --- Configuración del retardo ---

    /**
     * Ajusta el tiempo de espera mínimo en milisegundos entre peticiones HTTP consecutivas.
     *
     * @param tiempoEsperaMs milisegundos de espera
     */
    public void setTiempoEsperaMs(long tiempoEsperaMs) {
        this.tiempoEsperaMs = tiempoEsperaMs;
    }

    /**
     * Devuelve el tiempo de espera actual en milisegundos.
     *
     * @return milisegundos de espera
     */
    public long getTiempoEsperaMs() {
        return tiempoEsperaMs;
    }

    // --- Métodos de Interacción con DBA Lab ---

    /**
     * Realiza un GET a /challenges para obtener todos los retos disponibles y sus capacidades.
     * Mapea cada reto del array JSON a una instancia de {@link InfoReto}.
     *
     * @return lista inmutable de retos descubiertos
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public List<InfoReto> getRetos() throws IOException, InterruptedException {
        JsonObject json = enviarGet("/challenges");
        JsonArray array = json.has("challenges") ? json.getAsJsonArray("challenges") : new JsonArray();
        List<InfoReto> retos = new ArrayList<>();

        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                long id = obj.get("id").getAsLong();
                String envType = obj.get("environmentType").getAsString();

                List<String> sensors = new ArrayList<>();
                if (obj.has("sensorCodes")) {
                    for (JsonElement s : obj.getAsJsonArray("sensorCodes")) {
                        sensors.add(s.getAsString());
                    }
                }

                List<String> actions = new ArrayList<>();
                if (obj.has("actionCodes")) {
                    for (JsonElement a : obj.getAsJsonArray("actionCodes")) {
                        actions.add(a.getAsString());
                    }
                }

                retos.add(new InfoReto(id, envType, sensors, actions));
            }
        }
        return List.copyOf(retos);
    }

    /**
     * Busca un reto específico por su identificador numérico.
     *
     * @param challengeId ID del reto buscado
     * @return Optional con el InfoReto o vacío si no existe
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public Optional<InfoReto> buscarRetoPorId(long challengeId) throws IOException, InterruptedException {
        return getRetos().stream()
                .filter(r -> r.id() == challengeId)
                .findFirst();
    }

    /**
     * Inicia una nueva ejecución (run) para un reto determinado enviando un POST a /runs
     * con el cuerpo JSON: {"challengeId": &lt;id&gt;}.
     *
     * @param challengeId identificador numérico del reto anunciado por DBA Lab
     * @return estado de control oficial del nuevo run creado
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public EstadoControl iniciarRun(long challengeId) throws IOException, InterruptedException {
        String jsonBody = String.format("{\"challengeId\":%d}", challengeId);
        JsonObject respuesta = enviarPost("/runs", jsonBody);
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    /**
     * Consulta una percepción individual en DBA Lab enviando un POST sin cuerpo a:
     * /runs/{runId}/perceptions/{sensorId}.
     * Separa el bloque de valores del bloque oficial de control.
     *
     * @param runId    identificador de la ejecución activa
     * @param sensorId código oficial del sensor (p. ej. "thermostat.temperature" o "grid.position")
     * @return lectura tipada del sensor con sus valores en mapa y el control actualizado
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public LecturaSensor consultarSensor(String runId, String sensorId) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/perceptions/%s", runId, sensorId);
        JsonObject respuesta = enviarPost(path, "");
        Map<String, Object> valores = parsearMapa(respuesta.getAsJsonObject("value"));
        EstadoControl control = parsearControl(respuesta.getAsJsonObject("control"));
        return new LecturaSensor(sensorId, valores, control);
    }

    /**
     * Envía una acción física al entorno para avanzar un paso en la simulación.
     * Ejecuta un POST a /runs/{runId}/actions con el cuerpo: {"actionCode": "&lt;codigo&gt;"}.
     *
     * @param runId      identificador de la ejecución activa
     * @param actionCode código oficial de la acción en DBA Lab
     * @return respuesta que incluye si fue APPLIED/REJECTED, el motivo y el nuevo EstadoControl
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public RespuestaAccion enviarAccion(String runId, String actionCode) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/actions", runId);
        String jsonBody = String.format("{\"actionCode\":\"%s\"}", actionCode);
        JsonObject respuesta = enviarPost(path, jsonBody);

        String status = respuesta.get("actionStatus").getAsString();
        String reason = respuesta.has("reasonCode") && !respuesta.get("reasonCode").isJsonNull()
                ? respuesta.get("reasonCode").getAsString()
                : null;
        EstadoControl control = parsearControl(respuesta.getAsJsonObject("control"));

        return new RespuestaAccion(status, reason, control);
    }

    /**
     * Consulta el estado de control de un run mediante un GET a /runs/{runId}
     * sin consumir turnos ni solicitar lecturas de sensores.
     *
     * @param runId identificador de la ejecución
     * @return estado de control actual registrado en el servidor
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public EstadoControl consultarEstado(String runId) throws IOException, InterruptedException {
        JsonObject respuesta = enviarGet("/runs/" + runId);
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    /**
     * Abandona y cierra un run activo en DBA Lab enviando un POST a /runs/{runId}/close.
     * El servidor marcará el run como INTERRUPTED (STUDENT_ABANDONED), liberando la sesión del alumno.
     *
     * @param runId identificador de la ejecución a cerrar
     * @return estado de control de cierre devuelto por el servidor
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public EstadoControl cerrarRun(String runId) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/close", runId);
        JsonObject respuesta = enviarPost(path, "");
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    // --- Auxiliares de Mapeo JSON -> Objetos Java ---

    /**
     * Parsea de forma segura el bloque "control" de cualquier respuesta de DBA Lab a {@link EstadoControl}.
     * Maneja valores nulos o campos ausentes asignando UNKNOWN si el status no es reconocido.
     */
    private EstadoControl parsearControl(JsonObject control) {
        if (control == null) {
            return new EstadoControl(null, EstadoControl.RunStatus.UNKNOWN, null);
        }
        String runId = control.has("runId") && !control.get("runId").isJsonNull()
                ? control.get("runId").getAsString()
                : null;
        String statusStr = control.has("runStatus") && !control.get("runStatus").isJsonNull()
                ? control.get("runStatus").getAsString()
                : null;
        String reasonCode = control.has("reasonCode") && !control.get("reasonCode").isJsonNull()
                ? control.get("reasonCode").getAsString()
                : null;

        return new EstadoControl(runId, EstadoControl.parsearEstado(statusStr), reasonCode);
    }

    /**
     * Transforma un JsonObject de percepción ("value") a un Map&lt;String, Object&gt; puro de Java.
     * Convierte primitivos JSON a sus tipos equivalentes (Boolean, Number, String).
     * Omite explícitamente elementos nulos para prevenir fallos con Map.copyOf.
     */
    private Map<String, Object> parsearMapa(JsonObject json) {
        Map<String, Object> mapa = new HashMap<>();
        if (json == null) {
            return mapa;
        }
        for (Map.Entry<String, JsonElement> entrada : json.entrySet()) {
            JsonElement elem = entrada.getValue();
            if (elem != null && !elem.isJsonNull()) {
                if (elem.isJsonPrimitive()) {
                    JsonPrimitive prim = elem.getAsJsonPrimitive();
                    if (prim.isBoolean()) {
                        mapa.put(entrada.getKey(), prim.getAsBoolean());
                    } else if (prim.isNumber()) {
                        mapa.put(entrada.getKey(), prim.getAsNumber());
                    } else {
                        mapa.put(entrada.getKey(), prim.getAsString());
                    }
                } else {
                    mapa.put(entrada.getKey(), elem.toString());
                }
            }
        }
        return mapa;
    }

    // --- Infraestructura HTTP y Control de Peticiones ---

    /**
     * Pausa la ejecución si tiempoEsperaMs &gt; 0 para no saturar el servidor compartido de la universidad.
     */
    private void esperarEntrePeticiones() throws InterruptedException {
        if (tiempoEsperaMs > 0) {
            Thread.sleep(tiempoEsperaMs);
        }
    }

    /**
     * Ejecuta una petición HTTP GET autenticada con cabecera Authorization: Bearer &lt;token&gt;.
     * Valida el código de estado y deserializa el cuerpo a un JsonObject interno.
     */
    private JsonObject enviarGet(String path) throws IOException, InterruptedException {
        esperarEntrePeticiones();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> respuesta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validarRespuesta(respuesta, path);

        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    /**
     * Ejecuta una petición HTTP POST autenticada.
     * Si jsonBody tiene contenido, añade la cabecera Content-Type: application/json.
     * Si está vacío, envía POST sin cuerpo según especificación de DBA Lab (p. ej. en perceptions y close).
     */
    private JsonObject enviarPost(String path, String jsonBody) throws IOException, InterruptedException {
        esperarEntrePeticiones();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15));

        if (jsonBody != null && !jsonBody.isBlank()) {
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> respuesta = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        validarRespuesta(respuesta, path);

        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    /**
     * Comprueba que la respuesta HTTP esté en el rango 200..299.
     * Si no es exitosa, extrae el código de error y mensaje estructurado devuelto por DBA Lab
     * para facilitar la depuración, y lanza una IllegalStateException clara.
     */
    private void validarRespuesta(HttpResponse<String> respuesta, String path) {
        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            String detalle = respuesta.body();
            try {
                JsonObject jsonError = JsonParser.parseString(respuesta.body()).getAsJsonObject();
                if (jsonError.has("code") && jsonError.has("message")) {
                    detalle = jsonError.get("code").getAsString() + ": " + jsonError.get("message").getAsString();
                }
            } catch (Exception ignored) {
                // Si no es un JSON estructurado con code/message, se mantiene el cuerpo original
            }
            throw new IllegalStateException("Error HTTP " + respuesta.statusCode()
                    + " en " + path + " -> " + detalle);
        }
    }
}
