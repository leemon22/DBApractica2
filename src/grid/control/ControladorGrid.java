package grid.control;

import base.InfoReto;
import base.Visualizador;
import grid.Direccion;
import grid.SensoresGrid;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Buzón de órdenes de presentación. No crea agentes ni conserva referencias a JADE. */
public final class ControladorGrid {
    public interface Acciones { void reiniciar(); void finalizar(); }
    public interface Vista extends Visualizador<SensoresGrid, Direccion> {
        void mostrarOperacion(String mensaje, boolean ocupado, boolean finalizando);
        void cancelarSeleccion();
        void cerrarVentana(String aviso);
    }
    public static final class SeleccionCancelada extends RuntimeException { }
    private enum Orden { INICIAR, SALIR }
    private final Vista vista;
    private final LinkedBlockingQueue<Orden> ordenes = new LinkedBlockingQueue<>();
    private final AtomicBoolean ocupado = new AtomicBoolean(true);
    private final AtomicBoolean salir = new AtomicBoolean();
    private final AtomicBoolean parar = new AtomicBoolean();
    private volatile boolean bloqueado;

    public ControladorGrid(Vista vista) {
        this.vista = vista;
        ordenes.add(Orden.INICIAR);
    }
    public Acciones acciones() {
        return new Acciones() {
            @Override public void reiniciar() { ControladorGrid.this.reiniciar(); }
            @Override public void finalizar() { ControladorGrid.this.finalizar(); }
        };
    }
    public void reiniciar() {
        if (salir.get() || bloqueado || !ocupado.compareAndSet(false, true)) return;
        parar.set(true);
        vista.mostrarOperacion("Deteniendo la ejecución antes de elegir nivel…", true, false);
        ordenes.offer(Orden.INICIAR);
    }
    public void finalizar() {
        if (!salir.compareAndSet(false, true)) return;
        parar.set(true);
        vista.mostrarOperacion("Finalizando: esperando el cierre de la ejecución…", true, true);
        vista.cancelarSeleccion();
        ordenes.offer(Orden.SALIR);
    }
    /** Se espera en el hilo del agente; el EDT permanece disponible para los botones. */
    public boolean esperarInicio() throws InterruptedException {
        while (true) {
            if (salir.get()) return false;
            Orden orden = ordenes.take();
            if (orden == Orden.SALIR || salir.get()) return false;
            if (bloqueado) continue;
            parar.set(false);
            ocupado.set(true);
            vista.mostrarOperacion("Buscando niveles disponibles…", true, false);
            return true;
        }
    }
    public boolean paradaSolicitada() { return parar.get(); }
    public boolean salidaSolicitada() { return salir.get(); }
    public void iniciada() {
        ocupado.set(false);
        if (!salir.get()) vista.mostrarOperacion("Ejecución iniciada", false, false);
    }
    public void enEspera(String mensaje) {
        if (salir.get() || !ordenes.isEmpty()) return;
        ocupado.set(false);
        vista.mostrarOperacion(mensaje, false, false);
    }
    public void bloquear(String mensaje) {
        bloqueado = true;
        vista.mostrarOperacion(mensaje + ". Revisa DBA Lab antes de iniciar otro run.", true, salir.get());
    }
    public void cerrarVentana(Throwable fallo) {
        vista.cerrarVentana(fallo == null ? null : "No se pudo confirmar el cierre del run: " + fallo.getMessage());
    }
}
