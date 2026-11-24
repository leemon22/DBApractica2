package grid.control;

import base.EstadoControl;
import base.InfoReto;
import base.Visualizador;
import grid.Direccion;
import grid.SensoresGrid;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Pruebas del reinicio/cierre con ejecuciones simuladas, sin VPN ni token. */
public final class ControladorGridPrueba {
    private static final InfoReto A = new InfoReto(2, "GRID_2D", List.of(), List.of());
    private static final InfoReto B = new InfoReto(3, "GRID_2D", List.of(), List.of());

    public static void main(String[] args) throws Exception {
        probarReinicioOrdenado();
        probarCancelarYVolver();
        probarCierreFallido();
        probarFinalizarSeleccion();
        probarFinalizarDuranteArranque();
        probarDescubrimientoFallido(false);
        probarDescubrimientoFallido(true);
        System.out.println("Control de ventana: 7 escenarios superados sin VPN ni token.");
    }

    private static void probarReinicioOrdenado() throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor();
        CountDownLatch salio = new CountDownLatch(1);
        ControladorGrid control = new ControladorGrid(vista, motor, () -> {
            exigir(vista.cerrada.getCount() == 0 && motor.cierres.get() == 1, "Salida antes del cierre coordinado");
            salio.countDown();
        });
        vista.selecciones.add(A); vista.selecciones.add(B);
        try {
            control.reiniciar(); SesionGrid primera = tomar(motor.inicios); vista.esperarDisponible();
            control.reiniciar(); control.reiniciar();
            exigir(primera.detenerSolicitado(), "Reiniciar no solicita detener el run actual");
            exigir(motor.inicios.poll(100, TimeUnit.MILLISECONDS) == null, "Se solaparon dos ejecuciones");
            primera.completar(null);
            SesionGrid segunda = tomar(motor.inicios); vista.esperarDisponible();
            exigir(segunda.retoId() == B.id() && motor.descubrimientos.get() == 2, "No volvió a descubrir/elegir nivel");
            control.finalizar(); control.finalizar();
            exigir(segunda.detenerSolicitado(), "Finalizar no solicita parada");
            exigir(!motor.cerrado.await(100, TimeUnit.MILLISECONDS), "Se cerró JADE antes del agente");
            segunda.completar(null); esperar(vista.cerrada);
            esperar(salio);
            exigir(motor.cierres.get() == 1 && vista.aviso == null, "Cierre duplicado o falso error");
            control.reiniciar(); exigir(motor.inicios.isEmpty(), "Se reinició una ventana cerrada");
        } finally { completarTodo(motor); control.finalizar(); esperar(vista.cerrada); }
    }

    private static void probarCancelarYVolver() throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor(); ControladorGrid control = new ControladorGrid(vista, motor);
        try {
            vista.selecciones.add(new ControladorGrid.SeleccionCancelada());
            control.reiniciar(); vista.esperarDisponible();
            exigir(motor.inicios.isEmpty(), "Cancelar la selección creó una ejecución");
            vista.selecciones.add(A); control.reiniciar();
            SesionGrid sesion = tomar(motor.inicios); vista.esperarDisponible();
            sesion.completar(null); // Final normal antes de pulsar Finalizar.
            control.finalizar(); esperar(vista.cerrada);
            exigir(motor.cierres.get() == 1, "No cerró tras un fin natural");
        } finally { completarTodo(motor); control.finalizar(); esperar(vista.cerrada); }
    }

    private static void probarCierreFallido() throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor(); ControladorGrid control = new ControladorGrid(vista, motor);
        try {
            vista.selecciones.add(A); control.reiniciar();
            SesionGrid sesion = tomar(motor.inicios); vista.esperarDisponible();
            sesion.completar(new IOException("Cierre remoto fallido"));
            control.reiniciar(); vista.esperarDisponible();
            exigir(motor.inicios.isEmpty() && motor.descubrimientos.get() == 1, "Reinició sin confirmar el cierre anterior");
            exigir(vista.mensajes.stream().anyMatch(s -> s.contains("Cierre remoto fallido")), "No mostró el error");
            control.finalizar(); esperar(vista.cerrada);
            exigir(vista.aviso != null && vista.aviso.contains("Cierre remoto fallido"), "Ocultó el error al salir");
        } finally { completarTodo(motor); control.finalizar(); esperar(vista.cerrada); }
    }

    private static void probarFinalizarSeleccion() throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor(); ControladorGrid control = new ControladorGrid(vista, motor);
        try {
            control.reiniciar(); esperar(vista.seleccionAbierta);
            control.finalizar(); esperar(vista.cerrada);
            exigir(motor.inicios.isEmpty(), "Finalizar durante selección creó un run");
        } finally { control.finalizar(); esperar(vista.cerrada); }
    }

    private static void probarFinalizarDuranteArranque() throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor(); motor.bloquearInicio = true;
        ControladorGrid control = new ControladorGrid(vista, motor);
        try {
            vista.selecciones.add(A); control.reiniciar(); esperar(motor.entrando);
            control.finalizar(); motor.permitirInicio.countDown();
            SesionGrid sesion = tomar(motor.inicios);
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!sesion.detenerSolicitado() && System.nanoTime() < limite) {
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            exigir(sesion.detenerSolicitado(), "Quedó un agente tardío sin solicitud de parada");
            sesion.completar(null); esperar(vista.cerrada);
        } finally { motor.permitirInicio.countDown(); completarTodo(motor); control.finalizar(); esperar(vista.cerrada); }
    }

    private static void probarDescubrimientoFallido(boolean error) throws Exception {
        Vista vista = new Vista(); Motor motor = new Motor(); motor.sinRetos = !error; motor.errorDescubrimiento = error;
        ControladorGrid control = new ControladorGrid(vista, motor);
        try {
            control.reiniciar(); vista.esperarDisponible();
            exigir(motor.inicios.isEmpty(), "Se inició sin retos disponibles");
            exigir(vista.mensajes.stream().anyMatch(s -> s.startsWith("No se pudo reiniciar")), "No informó del fallo");
            control.finalizar(); esperar(vista.cerrada);
        } finally { control.finalizar(); esperar(vista.cerrada); }
    }

    private static final class Motor implements ControladorGrid.Motor {
        final BlockingQueue<SesionGrid> inicios = new LinkedBlockingQueue<>();
        final List<SesionGrid> sesiones = new CopyOnWriteArrayList<>();
        final AtomicInteger descubrimientos = new AtomicInteger(), cierres = new AtomicInteger();
        final CountDownLatch cerrado = new CountDownLatch(1), entrando = new CountDownLatch(1), permitirInicio = new CountDownLatch(1);
        boolean bloquearInicio, sinRetos, errorDescubrimiento;
        @Override public List<InfoReto> descubrirRetos() throws Exception {
            descubrimientos.incrementAndGet();
            if (errorDescubrimiento) throw new IOException("Red simulada no disponible");
            return sinRetos ? List.of() : List.of(A, B);
        }
        @Override public SesionGrid iniciar(InfoReto reto, Visualizador<SensoresGrid, Direccion> vista) throws Exception {
            entrando.countDown(); if (bloquearInicio) esperar(permitirInicio);
            SesionGrid sesion = new SesionGrid(reto.id()); sesiones.add(sesion); inicios.add(sesion); return sesion;
        }
        @Override public void cerrarPlataforma() { cierres.incrementAndGet(); cerrado.countDown(); }
    }

    private static final class Vista implements ControladorGrid.Vista {
        final BlockingQueue<Object> selecciones = new LinkedBlockingQueue<>();
        final BlockingQueue<String> disponibles = new LinkedBlockingQueue<>();
        final List<String> mensajes = new CopyOnWriteArrayList<>();
        final CountDownLatch cerrada = new CountDownLatch(1), seleccionAbierta = new CountDownLatch(1);
        volatile String aviso;
        void esperarDisponible() throws Exception { tomar(disponibles); }
        @Override public InfoReto seleccionarReto(List<InfoReto> retos) {
            seleccionAbierta.countDown();
            try {
                Object valor = tomar(selecciones);
                if (valor instanceof ControladorGrid.SeleccionCancelada e) throw e;
                return (InfoReto) valor;
            } catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
        @Override public void mostrarOperacion(String texto, boolean ocupado, boolean finalizando) {
            mensajes.add(texto); if (!ocupado) disponibles.add(texto);
        }
        @Override public void cancelarSeleccion() { selecciones.add(new ControladorGrid.SeleccionCancelada()); }
        @Override public void cerrarVentana(String aviso) { this.aviso = aviso; cerrada.countDown(); }
        @Override public void mostrarRetoEncontrado(InfoReto reto) { }
        @Override public void mostrarInicio(String id, EstadoControl estado) { }
        @Override public void mostrarPercepcion(SensoresGrid sensores) { }
        @Override public void mostrarAccion(Direccion d, String resultado, EstadoControl estado) { }
        @Override public void mostrarCierre(String id) { }
        @Override public void mostrarFin(EstadoControl estado) { }
    }

    private static void completarTodo(Motor motor) { motor.sesiones.forEach(s -> s.completar(null)); }
    private static void esperar(CountDownLatch latch) throws Exception { exigir(latch.await(5, TimeUnit.SECONDS), "Tiempo de espera agotado"); }
    private static <T> T tomar(BlockingQueue<T> cola) throws Exception {
        T valor = cola.poll(5, TimeUnit.SECONDS); exigir(valor != null, "No se recibió el evento esperado"); return valor;
    }
    private static void exigir(boolean condicion, String mensaje) { if (!condicion) throw new AssertionError(mensaje); }
}
