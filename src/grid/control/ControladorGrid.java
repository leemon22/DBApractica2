package grid.control;

import base.Visualizador;

import grid.Direccion;
import grid.SensoresGrid;

/**
 * Comunica los botones de Swing con el agente. No crea agentes ni hace peticiones HTTP. Los métodos
 * sincronizados protegen el estado compartido por ambos hilos.
 */
public final class ControladorGrid {
    public interface Vista extends Visualizador<SensoresGrid, Direccion> {
        void mostrarOperacion(String mensaje, boolean ocupado, boolean finalizando);

        void cancelarSeleccion();

        void cerrarVentana(String aviso);
    }

    public static final class SeleccionCancelada extends RuntimeException {}

    private enum Estado {
        PREPARANDO, // eligiendo nivel o arrancando un run
        EJECUTANDO, // el agente está navegando
        ESPERANDO, // run terminado: se puede Reiniciar o Finalizar
        BLOQUEADO, // no se pudo cerrar el run anterior: solo se permite salir
        SALIENDO // se ha pulsado Finalizar
    }

    private final Vista vista;
    private Estado estado = Estado.PREPARANDO;
    // true cuando toca elegir nivel: al arrancar y tras pulsar Reiniciar.
    private boolean inicioPendiente = true;

    public ControladorGrid(Vista vista) {
        this.vista = vista;
    }

    /** Botón Reiniciar (hilo de Swing): despierta al agente para elegir otro nivel. */
    public synchronized void reiniciar() {
        if (estado != Estado.EJECUTANDO && estado != Estado.ESPERANDO) {
            return;
        }
        estado = Estado.PREPARANDO;
        inicioPendiente = true;
        vista.mostrarOperacion("Deteniendo la ejecución antes de elegir nivel…", true, false);
        notifyAll();
    }

    /** Botón Finalizar o la X (hilo de Swing): pide al agente cerrar el run y salir. */
    public synchronized void finalizar() {
        if (estado == Estado.SALIENDO) {
            return;
        }
        estado = Estado.SALIENDO;
        vista.mostrarOperacion("Finalizando: esperando el cierre de la ejecución…", true, true);
        vista.cancelarSeleccion();
        notifyAll();
    }

    /** Espera en el hilo del agente; wait libera el monitor para que respondan los botones. */
    public synchronized boolean esperarInicio() throws InterruptedException {
        while (!inicioPendiente && estado != Estado.SALIENDO) {
            wait();
        }
        if (estado == Estado.SALIENDO) {
            return false;
        }
        inicioPendiente = false;
        vista.mostrarOperacion("Buscando niveles disponibles…", true, false);
        return true;
    }

    /** El agente lo consulta en cada paso para dejar de moverse. */
    public synchronized boolean paradaSolicitada() {
        return estado == Estado.PREPARANDO
                || estado == Estado.BLOQUEADO
                || estado == Estado.SALIENDO;
    }

    public synchronized boolean salidaSolicitada() {
        return estado == Estado.SALIENDO;
    }

    /** El agente avisa de que el run ha empezado. */
    public synchronized void iniciada() {
        if (estado == Estado.SALIENDO) {
            return;
        }
        estado = Estado.EJECUTANDO;
        vista.mostrarOperacion("Ejecución iniciada", false, false);
    }

    /** El agente avisa de que el run terminó, salvo que ya se haya pedido otro nivel. */
    public synchronized void enEspera(String mensaje) {
        if (estado == Estado.SALIENDO || inicioPendiente) {
            return;
        }
        estado = Estado.ESPERANDO;
        vista.mostrarOperacion(mensaje, false, false);
    }

    /** Si no se cierra el run anterior, solo se permite salir; nunca abrir otro. */
    public synchronized void bloquear(String mensaje) {
        if (estado != Estado.SALIENDO) {
            estado = Estado.BLOQUEADO;
        }
        inicioPendiente = false;
        vista.mostrarOperacion(
                mensaje + ". Revisa DBA Lab antes de iniciar otro run.",
                true,
                estado == Estado.SALIENDO);
    }

    public void cerrarVentana(Throwable fallo) {
        String aviso =
                fallo == null
                        ? null
                        : "No se pudo confirmar el cierre del run: " + fallo.getMessage();
        vista.cerrarVentana(aviso);
    }
}
