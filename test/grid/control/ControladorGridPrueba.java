package grid.control;

import base.*;

import grid.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Prueba de la presentación/entorno con cliente local simulado, sin credenciales. */
public final class ControladorGridPrueba {
    public static void main(String[] args) throws Exception {
        probarReinicioYSalida();
        probarCancelacionYCierreFallido();
        probarSalidaAntesDelInicio();
        System.out.println("Reinicio, cancelación, cierre fallido y salida inicial: correctos.");
    }

    private static void probarReinicioYSalida() throws Exception {
        Vista vista = new Vista();
        ControladorGrid control = new ControladorGrid(vista);
        Cliente cliente = new Cliente();
        vista.opciones.addAll(List.of(2L, 3L));
        EntornoGrid entorno = new EntornoGrid(cliente, vista, control);
        entorno.iniciarRun();
        BucleAgenticoGrid bucle =
                new BucleAgenticoGrid(new jade.core.Agent(), entorno, new PoliticaGridV2());
        bucle.action();
        control.reiniciar();
        control.reiniciar();
        try {
            entorno.ejecutarAccion(Direccion.ESTE);
            throw new AssertionError("Acción tras parada");
        } catch (PoliticaGridV2.ParadaNavegacion ok) {
        }
        bucle.action();
        exigir(!bucle.done(), "El agente terminó antes de permitir reiniciar");
        bucle.action();
        exigir(
                cliente.eventos.equals(List.of("inicio2", "accion", "cierre", "inicio3", "accion")),
                "Solapamiento");
        exigir(vista.finalizaciones == 1, "El cierre se notificó más de una vez");
        control.finalizar();
        bucle.action();
        exigir(bucle.done(), "No termina al pulsar Finalizar");
        entorno.terminarEjecucion();
        entorno.terminarEjecucion();
        entorno.cerrarPresentacion(null);
        exigir(vista.finalizaciones == 2, "Cada run debe notificar su fin una sola vez");
        exigir(vista.cerrada && cliente.eventos.size() == 6, "No cerró");
    }

    private static void probarCancelacionYCierreFallido() throws Exception {
        Vista vista = new Vista();
        ControladorGrid control = new ControladorGrid(vista);
        Cliente cliente = new Cliente();
        vista.opciones.addAll(List.of(-1L, 2L));
        EntornoGrid cancel = new EntornoGrid(cliente, vista, control);
        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            Future<?> f =
                    hilo.submit(() -> {
                        try {
                            cancel.iniciarRun();
                        } catch (Exception x) {
                            throw new RuntimeException(x);
                        }
                    });
            exigir(vista.cancelada.await(5, TimeUnit.SECONDS), "Cancelación");
            exigir(cliente.eventos.isEmpty(), "Cancelar creó run");
            control.reiniciar();
            f.get(5, TimeUnit.SECONDS);
            cliente.fallar = true;
            try {
                cancel.terminarEjecucion();
                throw new AssertionError("Error oculto");
            } catch (IOException x) {
                cancel.bloquearReinicio(x.getMessage());
            }
            control.reiniciar();
            control.finalizar();
            cancel.iniciarRun();
            exigir(cliente.eventos.equals(List.of("inicio2")), "Reinició tras fallo");
        } finally {
            control.finalizar();
            hilo.shutdownNow();
        }
    }

    private static void probarSalidaAntesDelInicio() throws Exception {
        Vista vista = new Vista();
        ControladorGrid control = new ControladorGrid(vista);
        Cliente cliente = new Cliente();
        control.finalizar();
        new EntornoGrid(cliente, vista, control).iniciarRun();
        exigir(cliente.eventos.isEmpty(), "Inicio tras salir");
    }

    static void exigir(boolean b, String m) {
        if (!b) throw new AssertionError(m);
    }

    static class Vista implements ControladorGrid.Vista {
        Queue<Long> opciones = new ConcurrentLinkedQueue<>();
        CountDownLatch cancelada = new CountDownLatch(1);
        boolean cerrada;
        int finalizaciones;

        public InfoReto seleccionarReto(List<InfoReto> rs) {
            long id = opciones.remove();
            if (id < 0) throw new ControladorGrid.SeleccionCancelada();
            return rs.stream().filter(r -> r.id() == id).findFirst().orElseThrow();
        }

        public void mostrarOperacion(String s, boolean ocupado, boolean salir) {
            if (s.startsWith("Selección cancelada")) cancelada.countDown();
        }

        public void cancelarSeleccion() {}

        public void cerrarVentana(String s) {
            cerrada = true;
        }

        public void mostrarRetoEncontrado(InfoReto r) {}

        public void mostrarInicio(String r, EstadoControl entorno) {}

        public void mostrarPercepcion(SensoresGrid s) {}

        public void mostrarAccion(Direccion d, String r, EstadoControl entorno) {}

        public void mostrarCierre(String r) {}

        public void mostrarFin(EstadoControl entorno) {
            finalizaciones++;
        }
    }

    static class Cliente extends ClienteHttpDba {
        List<String> eventos = new CopyOnWriteArrayList<>();
        String id;
        boolean fallar;

        Cliente() {
            super("http://local-simulado", "prueba");
        }

        public List<InfoReto> getRetos() {
            return List.of(reto(2), reto(3));
        }

        InfoReto reto(long n) {
            return new InfoReto(
                    n,
                    "GRID_2D",
                    List.of("grid.target", "grid.position", "grid.bumpers", "grid.energy"),
                    List.of(
                            "grid.move.north",
                            "grid.move.east",
                            "grid.move.south",
                            "grid.move.west"));
        }

        EstadoControl estado(EstadoControl.RunStatus s) {
            return new EstadoControl(id, s, null);
        }

        public EstadoControl iniciarRun(long n) {
            id = "" + n;
            eventos.add("inicio" + n);
            return estado(EstadoControl.RunStatus.ACTIVE);
        }

        public LecturaSensor consultarSensor(String run, String sensor) {
            return new LecturaSensor(
                    sensor,
                    sensor.equals("grid.bumpers")
                            ? Map.of("north", false, "east", false, "south", false, "west", false)
                            : sensor.equals("grid.energy")
                                    ? Map.of("mode", "FINITE", "remaining", 100)
                                    : Map.of("x", sensor.equals("grid.target") ? 2 : 0, "y", 0),
                    estado(EstadoControl.RunStatus.ACTIVE));
        }

        public RespuestaAccion enviarAccion(String r, String a) {
            eventos.add("accion");
            return new RespuestaAccion("APPLIED", null, estado(EstadoControl.RunStatus.ACTIVE));
        }

        public EstadoControl cerrarRun(String r) throws IOException {
            if (fallar) throw new IOException("Fallo simulado");
            eventos.add("cierre");
            return estado(EstadoControl.RunStatus.INTERRUPTED);
        }
    }
}
