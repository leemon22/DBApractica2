package grid.control;

import base.EstadoControl;
import base.InfoReto;
import grid.Direccion;
import grid.SensoresGrid;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Integración con AgenteGrid/HTTP/JADE reales contra un servidor exclusivamente local.
 * Ejecutar en un directorio temporal vacío, nunca en la raíz del proyecto. */
public final class FlujoLocalPrueba {
    public static void main(String[] args) throws Exception {
        Path configuracion = Path.of("config.properties");
        if (Files.exists(configuracion)) throw new IllegalStateException("Esta prueba necesita un directorio temporal vacío");
        CountDownLatch[] accion = {new CountDownLatch(1), new CountDownLatch(1)};
        CountDownLatch[] liberar = {new CountDownLatch(1), new CountDownLatch(1)};
        AtomicInteger runs = new AtomicInteger();
        AtomicInteger[] movimientos = {new AtomicInteger(), new AtomicInteger()};
        List<String> eventos = new CopyOnWriteArrayList<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1", exchange -> {
            try {
                String ruta = exchange.getRequestURI().getPath().substring("/api/v1".length());
                exchange.getRequestBody().readAllBytes();
                String cuerpo;
                if (ruta.equals("/challenges")) {
                    cuerpo = "{\"challenges\":[" + reto(2) + "," + reto(3) + "]}";
                } else if (ruta.equals("/runs")) {
                    int n = runs.incrementAndGet(); eventos.add("inicio" + n); cuerpo = envuelto(n, "ACTIVE");
                } else {
                    int n = Integer.parseInt(ruta.split("/")[2]);
                    if (ruta.endsWith("/actions")) {
                        movimientos[n - 1].incrementAndGet(); eventos.add("accion" + n); accion[n - 1].countDown();
                        if (!liberar[n - 1].await(8, TimeUnit.SECONDS)) throw new IllegalStateException("Acción no liberada");
                        cuerpo = "{\"actionStatus\":\"APPLIED\",\"control\":" + control(n, "ACTIVE") + "}";
                    } else if (ruta.endsWith("/close")) {
                        eventos.add("cierre" + n); cuerpo = control(n, "INTERRUPTED");
                    } else {
                        String valor = ruta.endsWith("grid.target") ? "{\"x\":5,\"y\":0}"
                                : ruta.endsWith("grid.position") ? "{\"x\":" + movimientos[n - 1].get() + ",\"y\":0}"
                                : ruta.endsWith("grid.bumpers") ? "{\"north\":true,\"east\":false,\"south\":false,\"west\":false}"
                                : "{\"mode\":\"FINITE\",\"remaining\":99}";
                        cuerpo = "{\"value\":" + valor + ",\"control\":" + control(n, "ACTIVE") + "}";
                    }
                }
                responder(exchange, cuerpo);
            } catch (Exception e) {
                e.printStackTrace(); exchange.close();
            }
        });
        servidor.start();
        Files.writeString(configuracion, "DBA_API_URL=http://127.0.0.1:" + servidor.getAddress().getPort()
                + "/api/v1\nDBA_TOKEN=credencial-local-simulada\n");
        try (ServerSocket puerto = new ServerSocket(0)) {
            System.setProperty("dba.jade.port", "" + puerto.getLocalPort());
        }
        Vista vista = new Vista();
        CountDownLatch salida = new CountDownLatch(1);
        ControladorGrid controlador = new ControladorGrid(vista, new MotorJadeGrid(), salida::countDown);
        try {
            controlador.reiniciar(); esperar(accion[0]); vista.esperarDisponible();
            controlador.reiniciar();
            exigir(!eventos.contains("cierre1"), "Cierre mientras la acción seguía en curso");
            liberar[0].countDown(); esperar(accion[1]); vista.esperarDisponible();
            exigir(eventos.indexOf("cierre1") < eventos.indexOf("inicio2"), "Runs solapados");
            controlador.finalizar();
            exigir(vista.cerrada.getCount() == 1, "Ventana cerrada antes de detener el segundo run");
            liberar[1].countDown(); esperar(vista.cerrada); esperar(salida);
            exigir(vista.aviso == null, "Error inesperado: " + vista.aviso);
            exigir(runs.get() == 2 && movimientos[0].get() == 1 && movimientos[1].get() == 1,
                    "Acciones posteriores a la solicitud de parada");
            exigir(eventos.getLast().equals("cierre2"), "No cerró el segundo run");
            System.out.println("Flujo local correcto: " + eventos);
        } finally {
            liberar[0].countDown(); liberar[1].countDown(); controlador.finalizar();
            servidor.stop(0); Files.deleteIfExists(configuracion);
        }
        System.exit(0); // Mismo último paso que el lanzador Swing.
    }

    private static String reto(int id) {
        return "{\"id\":" + id + ",\"environmentType\":\"GRID_2D\",\"sensorCodes\":[\"grid.target\",\"grid.position\",\"grid.bumpers\",\"grid.energy\"],"
                + "\"actionCodes\":[\"grid.move.north\",\"grid.move.east\",\"grid.move.south\",\"grid.move.west\"]}";
    }
    private static String control(int n, String estado) {
        return "{\"runId\":" + n + ",\"runStatus\":\"" + estado + "\",\"reasonCode\":null}";
    }
    private static String envuelto(int n, String estado) { return "{\"control\":" + control(n, estado) + "}"; }
    private static void responder(HttpExchange e, String cuerpo) throws Exception {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(200, bytes.length); e.getResponseBody().write(bytes); e.close();
    }
    private static void esperar(CountDownLatch latch) throws Exception { exigir(latch.await(10, TimeUnit.SECONDS), "Tiempo agotado"); }
    private static void exigir(boolean condicion, String texto) { if (!condicion) throw new AssertionError(texto); }

    private static final class Vista implements ControladorGrid.Vista {
        final BlockingQueue<String> disponible = new LinkedBlockingQueue<>();
        final CountDownLatch cerrada = new CountDownLatch(1);
        volatile String aviso;
        int seleccion;
        void esperarDisponible() throws Exception { exigir(disponible.poll(5, TimeUnit.SECONDS) != null, "Controlador ocupado"); }
        @Override public InfoReto seleccionarReto(List<InfoReto> retos) { return retos.get(seleccion++); }
        @Override public void mostrarOperacion(String s, boolean ocupado, boolean finalizando) { if (!ocupado) disponible.add(s); }
        @Override public void cancelarSeleccion() { }
        @Override public void cerrarVentana(String aviso) { this.aviso = aviso; cerrada.countDown(); }
        @Override public void mostrarRetoEncontrado(InfoReto reto) { }
        @Override public void mostrarInicio(String id, EstadoControl estado) { }
        @Override public void mostrarPercepcion(SensoresGrid s) { }
        @Override public void mostrarAccion(Direccion d, String resultado, EstadoControl estado) { }
        @Override public void mostrarCierre(String id) { }
        @Override public void mostrarFin(EstadoControl estado) { }
    }
}
