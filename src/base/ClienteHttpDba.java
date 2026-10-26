package base;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Cliente HTTP para interactuar con la API de DBA
// Precondiciones: Se debe proporcionar la URL base de la API y un token de autenticación válido al
// crear una instancia de esta clase.
// Para ello existe la clase Configuración, que puede leer un archivo de configuración y extraer de
// él la URL y el token.
public class ClienteHttpDba {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String token;

    public ClienteHttpDba(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // --- Interacción con la API ---

    public HttpResponse<String> getRetos() throws Exception {
        return enviarGet("/challenges");
    }

    public HttpResponse<String> iniciarRun(long challengeId) throws Exception {
        String jsonBody = String.format("{\"challengeId\":%d}", challengeId);
        return enviarPost("/runs", jsonBody);
    }

    public HttpResponse<String> enviarPercepcion(String runId, String sensorId) throws Exception {
        String path = String.format("/runs/%s/perceptions/%s", runId, sensorId);
        return enviarPost(path, "");
    }

    public HttpResponse<String> enviarAccion(String runId, String actionCode) throws Exception {
        String path = String.format("/runs/%s/actions", runId);
        String jsonBody = String.format("{\"actionCode\":\"%s\"}", actionCode);
        return enviarPost(path, jsonBody);
    }

    public HttpResponse<String> consultarEstado(String runId) throws Exception {
        return enviarGet("/runs/" + runId);
    }

    // --- Métodos Privados de Infraestructura HTTP ---

    private HttpResponse<String> enviarGet(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviarPost(String path, String jsonBody) throws Exception {
        HttpRequest.Builder requestBuilder =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json");

        if (jsonBody != null && !jsonBody.isBlank()) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
