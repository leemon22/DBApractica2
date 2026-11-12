package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP encargado de la comunicación con la API REST de DBA Lab.
 * Centraliza la autenticación Bearer, el retardo entre peticiones para no saturar
 * el servidor compartido y la validación de respuestas JSON.
 */
public class ClienteHttpDba {

    /** Retardo predeterminado de 500 ms entre peticiones consecutivas */
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
        this(new Configuracion("config.properties"));
    }

    /**
     * Construye el cliente a partir de una instancia de Configuracion.
     *
     * @param config configuración con la URL y el token
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
        this.baseUrl = baseUrl;
        this.token = token;
        this.tiempoEsperaMs = DELAY_PREDETERMINADO_MS;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // --- Configuración del retardo ---

    /**
     * Ajusta el tiempo de espera mínimo en milisegundos entre peticiones HTTP.
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
     * Consulta los retos disponibles y sus capacidades en el servidor.
     *
     * @return JsonObject con la respuesta completa de /challenges
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject getRetos() throws IOException, InterruptedException {
        return enviarGet("/challenges");
    }

    /**
     * Inicia una nueva ejecución (run) para un reto determinado.
     *
     * @param challengeId identificador numérico del reto
     * @return JsonObject con el bloque de control oficial del nuevo run
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject iniciarRun(long challengeId) throws IOException, InterruptedException {
        String jsonBody = String.format("{\"challengeId\":%d}", challengeId);
        return enviarPost("/runs", jsonBody);
    }

    /**
     * Consulta la percepción de un sensor concreto durante un run activo.
     *
     * @param runId    identificador de la ejecución
     * @param sensorId código oficial del sensor (p. ej. thermostat.temperature)
     * @return JsonObject con el valor de la percepción y el estado de control
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject enviarPercepcion(String runId, String sensorId) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/perceptions/%s", runId, sensorId);
        return enviarPost(path, "");
    }

    /**
     * Envía una acción física al entorno para su ejecución.
     *
     * @param runId      identificador de la ejecución
     * @param actionCode código oficial de la acción (p. ej. thermostat.refrigeration.on)
     * @return JsonObject con el resultado (actionStatus) y el nuevo estado de control
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject enviarAccion(String runId, String actionCode) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/actions", runId);
        String jsonBody = String.format("{\"actionCode\":\"%s\"}", actionCode);
        return enviarPost(path, jsonBody);
    }

    /**
     * Consulta el estado de control de un run sin solicitar percepciones.
     *
     * @param runId identificador de la ejecución
     * @return JsonObject con el estado actual del run
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject consultarEstado(String runId) throws IOException, InterruptedException {
        return enviarGet("/runs/" + runId);
    }

    /**
     * Abandona y cierra un run activo en DBA Lab (pasa a INTERRUPTED).
     *
     * @param runId identificador de la ejecución a cerrar
     * @return JsonObject con el estado de cierre devuelto por el servidor
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la espera
     */
    public JsonObject cerrarRun(String runId) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/close", runId);
        return enviarPost(path, "");
    }

    // --- Infraestructura HTTP y Control de Peticiones ---

    /**
     * Aplica una pausa para respetar el retardo configurado y no saturar el servidor.
     */
    private void esperarEntrePeticiones() throws InterruptedException {
        if (tiempoEsperaMs > 0) {
            Thread.sleep(tiempoEsperaMs);
        }
    }

    /**
     * Ejecuta una petición HTTP GET autenticada con Bearer token.
     *
     * @param path ruta relativa que se concatena a baseUrl
     * @return cuerpo de la respuesta parseado como JsonObject
     */
    private JsonObject enviarGet(String path) throws IOException, InterruptedException {
        esperarEntrePeticiones();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> respuesta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        validarRespuesta(respuesta, path);

        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    /**
     * Ejecuta una petición HTTP POST con cabecera Content-Type application/json.
     *
     * @param path     ruta relativa que se concatena a baseUrl
     * @param jsonBody cuerpo de la petición en formato JSON
     * @return cuerpo de la respuesta parseado como JsonObject
     */
    private JsonObject enviarPost(String path, String jsonBody) throws IOException, InterruptedException {
        esperarEntrePeticiones();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");

        if (jsonBody != null && !jsonBody.isBlank()) {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> respuesta = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        validarRespuesta(respuesta, path);

        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    /**
     * Valida que el código HTTP esté en el rango 200-299; de lo contrario lanza IllegalStateException.
     */
    private void validarRespuesta(HttpResponse<String> respuesta, String path) {
        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IllegalStateException("Error HTTP " + respuesta.statusCode()
                    + " en " + path + ": " + respuesta.body());
        }
    }
}
