package base;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Prueba la configuración y el protocolo HTTP con datos ficticios y un servidor local. */
public final class InfraestructuraPrueba {
    private static final String CONTROL =
            "\"control\":{\"runId\":7,\"runStatus\":\"ACTIVE\",\"reasonCode\":null}";

    public static void main(String[] args) throws Exception {
        probarConfiguracion();
        probarHttp();
        System.out.println("Configuración, HTTP y conversión de sensores: correctos.");
    }

    private static void probarConfiguracion() throws Exception {
        Path archivo = Files.createTempFile("dba-config-prueba-", ".properties");
        try {
            Files.writeString(
                    archivo,
                    "DBA_API_URL=http://local/api/v1  \n"
                            + "DBA_TOKEN=credencial-ficticia  \nDBA_CHALLENGE_ID=12\n");
            Configuracion config = new Configuracion(archivo.toString());
            exigir(config.getUrlApi().equals("http://local/api/v1"), "URL sin normalizar");
            exigir(config.getToken().equals("credencial-ficticia"), "Token sin normalizar");
            exigir(config.getChallengeId().orElseThrow() == 12, "Reto configurado incorrecto");
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    private static void probarHttp() throws Exception {
        List<String> peticiones = new CopyOnWriteArrayList<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/api/v1",
                intercambio -> {
                    String ruta = intercambio.getRequestURI().getPath();
                    String cuerpo =
                            new String(
                                    intercambio.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    peticiones.add(intercambio.getRequestMethod() + " " + ruta + " " + cuerpo);
                    boolean autenticada =
                            "Bearer ficticio"
                                    .equals(
                                            intercambio
                                                    .getRequestHeaders()
                                                    .getFirst("Authorization"));
                    int estado = autenticada ? 200 : 401;
                    String respuesta;
                    if (ruta.endsWith("/challenges")) {
                        respuesta =
                                "{\"challenges\":[{\"id\":2,\"environmentType\":\"GRID_2D\","
                                    + "\"sensorCodes\":[\"grid.position\"],\"actionCodes\":[\"grid.move.east\"]},"
                                    + "{\"id\":3,\"environmentType\":\"THERMOSTAT\"}]}";
                    } else if (ruta.endsWith("/perceptions/prueba")) {
                        respuesta =
                                "{\"value\":{\"x\":2,\"celsius\":21.5,\"north\":true,"
                                    + "\"mode\":\"FINITE\",\"ausente\":null,\"otro\":{\"a\":1}},"
                                        + CONTROL
                                        + "}";
                    } else if (ruta.endsWith("/actions")) {
                        respuesta =
                                "{\"actionStatus\":\"REJECTED\",\"reasonCode\":\"BLOCKED\","
                                        + CONTROL
                                        + "}";
                    } else if (ruta.endsWith("/runs/error")) {
                        estado = 409;
                        respuesta = "{\"code\":\"RUN_CLOSED\",\"message\":\"Ejecución terminada\"}";
                    } else if (ruta.endsWith("/runs/sin-json")) {
                        estado = 503;
                        respuesta = "Servicio no disponible";
                    } else if (ruta.endsWith("/runs/sin-control")) {
                        respuesta = "{}";
                    } else if (ruta.endsWith("/close")) {
                        respuesta =
                                "{\"control\":{\"runId\":7,\"runStatus\":\"INTERRUPTED\","
                                        + "\"reasonCode\":\"STUDENT_ABANDONED\"}}";
                    } else {
                        respuesta = "{" + CONTROL + "}";
                    }
                    byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
                    intercambio.sendResponseHeaders(estado, bytes.length);
                    try (var salida = intercambio.getResponseBody()) {
                        salida.write(bytes);
                    }
                });
        servidor.start();
        try {
            ClienteHttpDba cliente =
                    new ClienteHttpDba(
                            "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/v1/",
                            " ficticio ");
            cliente.setTiempoEsperaMs(0);
            List<InfoReto> retos = cliente.getRetos();
            exigir(
                    retos.size() == 2 && retos.get(0).contieneSensor("grid.position"),
                    "Descubrimiento");
            exigir(retos.get(0).contieneAccion("grid.move.east"), "Acciones anunciadas");
            exigir(retos.get(1).sensorCodes().isEmpty(), "Capacidades opcionales");
            exigir(cliente.iniciarRun(2).runId().equals("7"), "Inicio");

            LecturaSensor lectura = cliente.consultarSensor("7", "prueba");
            exigir(lectura.getInt("x") == 2 && lectura.getDouble("celsius") == 21.5, "Números");
            exigir(
                    lectura.getBoolean("north") && lectura.getString("mode").equals("FINITE"),
                    "Primitivos");
            exigir(lectura.getString("otro").equals("{\"a\":1}"), "Valor compuesto");
            exigir(!lectura.valores().containsKey("ausente"), "Propiedad nula");
            esperarError(() -> lectura.getInt("ausente"), "no existe");
            esperarError(() -> lectura.getBoolean("x"), "no es booleana");

            RespuestaAccion accion = cliente.enviarAccion("7", "grid.move.east");
            exigir(
                    !accion.isAplicada() && accion.motivo().orElseThrow().equals("BLOCKED"),
                    "Rechazo");
            exigir(cliente.consultarEstado("7").isActivo(), "Estado activo");
            exigir(
                    cliente.cerrarRun("7").runStatus() == EstadoControl.RunStatus.INTERRUPTED,
                    "Cierre");
            exigir(
                    cliente.consultarEstado("sin-control").runStatus()
                            == EstadoControl.RunStatus.UNKNOWN,
                    "Una respuesta sin control no debe permitir continuar");
            esperarError(() -> cliente.consultarEstado("error"), "RUN_CLOSED");
            esperarError(() -> cliente.consultarEstado("sin-json"), "Servicio no disponible");

            exigir(
                    peticiones.contains("POST /api/v1/runs {\"challengeId\":2}"),
                    "Cuerpo de inicio");
            exigir(
                    peticiones.contains(
                            "POST /api/v1/runs/7/actions {\"actionCode\":\"grid.move.east\"}"),
                    "Cuerpo de movimiento");
            exigir(
                    peticiones.contains("POST /api/v1/runs/7/perceptions/prueba "),
                    "Lectura sin cuerpo");
            exigir(peticiones.size() == 9, "Se han repetido peticiones");
        } finally {
            servidor.stop(0);
        }
    }

    private static void esperarError(Operacion operacion, String mensaje) throws Exception {
        try {
            operacion.ejecutar();
        } catch (IllegalArgumentException | IllegalStateException error) {
            exigir(error.getMessage().contains(mensaje), "Diagnóstico incorrecto");
            return;
        }
        throw new AssertionError("Se esperaba un error: " + mensaje);
    }

    private interface Operacion {
        void ejecutar() throws Exception;
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
