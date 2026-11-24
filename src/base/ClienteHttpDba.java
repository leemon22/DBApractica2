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

/**
 * Comunicación con DBA Lab: autenticación, retardo y traducción de JSON a datos Java. Las
 * peticiones comparten un timeout de 15 s y no se reintentan automáticamente: repetir un POST
 * podría crear otro run o aplicar dos movimientos.
 */
public class ClienteHttpDba {

    /** Retardo predeterminado de 50 ms entre peticiones consecutivas para proteger el servidor */
    public static final long DELAY_PREDETERMINADO_MS = 50;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String token;
    private long tiempoEsperaMs;

    /** Constructor por defecto: lee la URL y el token desde "config.properties". */
    public ClienteHttpDba() {
        this(new Configuracion());
    }

    /** Usa la URL y la credencial ya cargadas por Configuracion. */
    public ClienteHttpDba(Configuracion config) {
        this(config.getUrlApi(), config.getToken());
    }

    /** La URL debe incluir /api/v1. El token se envía como credencial Bearer. */
    public ClienteHttpDba(String baseUrl, String token) {
        Objects.requireNonNull(baseUrl, "La URL base no puede ser nula");
        Objects.requireNonNull(token, "El token no puede ser nulo");

        // Normalizamos la URL eliminando posibles barras finales accidentales
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token.trim();
        this.tiempoEsperaMs = DELAY_PREDETERMINADO_MS;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Pausa antes de cada petición; un valor no positivo desactiva la espera. */
    public void setTiempoEsperaMs(long tiempoEsperaMs) {
        this.tiempoEsperaMs = tiempoEsperaMs;
    }

    /** Retardo configurado en milisegundos. */
    public long getTiempoEsperaMs() {
        return tiempoEsperaMs;
    }

    /** GET /challenges: devuelve los retos anunciados y sus capacidades, en una lista inmutable. */
    public List<InfoReto> getRetos() throws IOException, InterruptedException {
        JsonObject json = enviarGet("/challenges");
        JsonArray array =
                json.has("challenges") ? json.getAsJsonArray("challenges") : new JsonArray();
        List<InfoReto> retos = new ArrayList<>();

        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                long id = obj.get("id").getAsLong();
                String envType = obj.get("environmentType").getAsString();

                retos.add(
                        new InfoReto(
                                id,
                                envType,
                                leerCodigos(obj, "sensorCodes"),
                                leerCodigos(obj, "actionCodes")));
            }
        }
        return List.copyOf(retos);
    }

    /** POST /runs: crea una ejecución para el reto seleccionado. */
    public EstadoControl iniciarRun(long challengeId) throws IOException, InterruptedException {
        String jsonBody = String.format("{\"challengeId\":%d}", challengeId);
        JsonObject respuesta = enviarPost("/runs", jsonBody);
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    /** POST sin cuerpo a /runs/{runId}/perceptions/{sensorId}: separa valores y control. */
    public LecturaSensor consultarSensor(String runId, String sensorId)
            throws IOException, InterruptedException {
        String path = String.format("/runs/%s/perceptions/%s", runId, sensorId);
        JsonObject respuesta = enviarPost(path, "");
        Map<String, Object> valores = parsearMapa(respuesta.getAsJsonObject("value"));
        EstadoControl control = parsearControl(respuesta.getAsJsonObject("control"));
        return new LecturaSensor(sensorId, valores, control);
    }

    /** POST /runs/{runId}/actions: devuelve el resultado y el estado oficial del run. */
    public RespuestaAccion enviarAccion(String runId, String actionCode)
            throws IOException, InterruptedException {
        String path = String.format("/runs/%s/actions", runId);
        String jsonBody = String.format("{\"actionCode\":\"%s\"}", actionCode);
        JsonObject respuesta = enviarPost(path, jsonBody);

        String status = respuesta.get("actionStatus").getAsString();
        String reason = leerTexto(respuesta, "reasonCode");
        EstadoControl control = parsearControl(respuesta.getAsJsonObject("control"));

        return new RespuestaAccion(status, reason, control);
    }

    /** GET /runs/{runId}: consulta el estado sin solicitar sensores ni acciones. */
    public EstadoControl consultarEstado(String runId) throws IOException, InterruptedException {
        JsonObject respuesta = enviarGet("/runs/" + runId);
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    /** POST sin cuerpo a /runs/{runId}/close: abandona la ejecución. */
    public EstadoControl cerrarRun(String runId) throws IOException, InterruptedException {
        String path = String.format("/runs/%s/close", runId);
        JsonObject respuesta = enviarPost(path, "");
        return parsearControl(respuesta.getAsJsonObject("control"));
    }

    /** Si falta el bloque de control o su estado no es conocido, se usa UNKNOWN para detenerse. */
    private EstadoControl parsearControl(JsonObject control) {
        if (control == null) {
            return new EstadoControl(null, EstadoControl.RunStatus.UNKNOWN, null);
        }
        return new EstadoControl(
                leerTexto(control, "runId"),
                EstadoControl.parsearEstado(leerTexto(control, "runStatus")),
                leerTexto(control, "reasonCode"));
    }

    private String leerTexto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        return valor == null || valor.isJsonNull() ? null : valor.getAsString();
    }

    private List<String> leerCodigos(JsonObject objeto, String campo) {
        List<String> codigos = new ArrayList<>();
        if (objeto.has(campo)) {
            for (JsonElement codigo : objeto.getAsJsonArray(campo)) {
                codigos.add(codigo.getAsString());
            }
        }
        return codigos;
    }

    /** Convierte los valores JSON a datos Java. Omite null para admitir mapas inmutables. */
    private Map<String, Object> parsearMapa(JsonObject json) {
        Map<String, Object> mapa = new HashMap<>();
        if (json == null) {
            return mapa;
        }
        for (Map.Entry<String, JsonElement> entrada : json.entrySet()) {
            JsonElement valor = entrada.getValue();
            // Los mapas inmutables no admiten null; las propiedades nulas se omiten.
            if (valor != null && !valor.isJsonNull()) {
                mapa.put(entrada.getKey(), convertirValor(valor));
            }
        }
        return mapa;
    }

    private Object convertirValor(JsonElement valor) {
        if (!valor.isJsonPrimitive()) return valor.toString();
        JsonPrimitive primitivo = valor.getAsJsonPrimitive();
        if (primitivo.isBoolean()) return primitivo.getAsBoolean();
        if (primitivo.isNumber()) return primitivo.getAsNumber();
        return primitivo.getAsString();
    }

    /** Evita saturar el servidor compartido del laboratorio. */
    private void esperarEntrePeticiones() throws InterruptedException {
        if (tiempoEsperaMs > 0) {
            Thread.sleep(tiempoEsperaMs);
        }
    }

    /** Prepara un GET autenticado; enviar() se encarga de ejecutarlo y validar la respuesta. */
    private JsonObject enviarGet(String path) throws IOException, InterruptedException {
        return enviar(crearPeticion(path).GET().build(), path);
    }

    /** Cabeceras y timeout comunes a GET y POST. No envía todavía la petición. */
    private HttpRequest.Builder crearPeticion(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    /** Aplica el mismo retardo y validación a cada petición, sin reintentos automáticos. */
    private JsonObject enviar(HttpRequest peticion, String path)
            throws IOException, InterruptedException {
        esperarEntrePeticiones();
        HttpResponse<String> respuesta =
                httpClient.send(peticion, HttpResponse.BodyHandlers.ofString());
        validarRespuesta(respuesta, path);
        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    /** Solo añade Content-Type si hay JSON; las lecturas y el cierre usan POST sin cuerpo. */
    private JsonObject enviarPost(String path, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = crearPeticion(path);

        if (jsonBody != null && !jsonBody.isBlank()) {
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        return enviar(builder.build(), path);
    }

    /**
     * Acepta estados 2xx; los demás producen un error con el diagnóstico devuelto por el servidor.
     */
    private void validarRespuesta(HttpResponse<String> respuesta, String path) {
        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            String detalle = respuesta.body();
            try {
                JsonObject jsonError = JsonParser.parseString(respuesta.body()).getAsJsonObject();
                if (jsonError.has("code") && jsonError.has("message")) {
                    detalle =
                            jsonError.get("code").getAsString()
                                    + ": "
                                    + jsonError.get("message").getAsString();
                }
            } catch (Exception ignored) {
                // Si no es un JSON estructurado con code/message, se mantiene el cuerpo original
            }
            throw new IllegalStateException(
                    "Error HTTP " + respuesta.statusCode() + " en " + path + " -> " + detalle);
        }
    }
}
