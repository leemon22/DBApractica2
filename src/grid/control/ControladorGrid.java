package grid.control;

import base.InfoReto;
import base.Visualizador;
import grid.Direccion;
import grid.SensoresGrid;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordina las órdenes de ventana fuera del hilo gráfico, una operación cada vez. */
public final class ControladorGrid {
    public interface Acciones {
        void reiniciar();
        void finalizar();
    }

    public interface Vista extends Visualizador<SensoresGrid, Direccion> {
        void mostrarOperacion(String mensaje, boolean ocupado, boolean finalizando);
        void cancelarSeleccion();
        void cerrarVentana(String aviso);
    }

    public interface Motor {
        List<InfoReto> descubrirRetos() throws Exception;
        SesionGrid iniciar(InfoReto reto, Visualizador<SensoresGrid, Direccion> vista) throws Exception;
        void cerrarPlataforma() throws Exception;
    }

    public static final class SeleccionCancelada extends RuntimeException { }

    private final Vista vista;
    private final Motor motor;
    private final Runnable salirAplicacion;
    private final ExecutorService tareas = Executors.newSingleThreadExecutor();
    private final AtomicBoolean ocupado = new AtomicBoolean();
    private final AtomicBoolean finalizando = new AtomicBoolean();
    private volatile SesionGrid sesion;

    public ControladorGrid(Vista vista, Motor motor) {
        this(vista, motor, () -> { });
    }

    public ControladorGrid(Vista vista, Motor motor, Runnable salirAplicacion) {
        this.vista = vista; this.motor = motor; this.salirAplicacion = salirAplicacion;
    }

    public Acciones acciones() {
        return new Acciones() {
            @Override public void reiniciar() { ControladorGrid.this.reiniciar(); }
            @Override public void finalizar() { ControladorGrid.this.finalizar(); }
        };
    }

    /** Sirve también para la primera selección. El motor todavía no crea ningún run. */
    public void reiniciar() {
        if (finalizando.get() || !ocupado.compareAndSet(false, true)) return;
        vista.mostrarOperacion("Deteniendo la ejecución antes de elegir nivel…", true, false);
        SesionGrid anterior = sesion;
        if (anterior != null) anterior.solicitarParada();
        tareas.execute(() -> {
            String mensaje = "Ejecución iniciada";
            try {
                if (anterior != null) anterior.esperarFin();
                if (finalizando.get()) return;
                vista.mostrarOperacion("Buscando niveles disponibles…", true, false);
                List<InfoReto> retos = motor.descubrirRetos();
                if (finalizando.get()) return;
                if (retos.isEmpty()) throw new IllegalStateException("No hay niveles Grid disponibles");
                InfoReto elegido = vista.seleccionarReto(retos);
                if (finalizando.get()) return;
                sesion = motor.iniciar(elegido, vista);
                // Finalizar puede haberse pulsado mientras el motor creaba el agente.
                if (finalizando.get()) sesion.solicitarParada();
            } catch (SeleccionCancelada e) {
                mensaje = "Selección cancelada. Pulsa Reiniciar para elegir un nivel.";
            } catch (Exception e) {
                mensaje = "No se pudo reiniciar: " + detalle(e);
            } finally {
                ocupado.set(false);
                if (!finalizando.get()) vista.mostrarOperacion(mensaje, false, false);
            }
        });
    }

    public void finalizar() {
        if (!finalizando.compareAndSet(false, true)) return;
        vista.mostrarOperacion("Finalizando: esperando el cierre de la ejecución…", true, true);
        vista.cancelarSeleccion();
        SesionGrid actual = sesion;
        if (actual != null) actual.solicitarParada();
        tareas.execute(() -> {
            String aviso = null;
            try {
                if (sesion != null) {
                    sesion.solicitarParada();
                    sesion.esperarFin();
                }
            } catch (Exception e) {
                aviso = "No se pudo confirmar el cierre del run: " + detalle(e)
                        + ". Revisa DBA Lab; la ventana se cerrará.";
            } finally {
                try { motor.cerrarPlataforma(); }
                catch (Exception e) { aviso = (aviso == null ? "" : aviso + " ") + "Error al cerrar JADE: " + detalle(e); }
                try { vista.cerrarVentana(aviso); }
                finally { tareas.shutdown(); salirAplicacion.run(); }
            }
        });
    }

    private static String detalle(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }
}
