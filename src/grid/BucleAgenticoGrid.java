package grid;

import base.BucleAgentico;

import jade.core.Agent;

/** Ciclo de Grid: navegación, parada y reinicio entre runs. No depende de Swing. */
public final class BucleAgenticoGrid extends BucleAgentico<SensoresGrid, Direccion> {
    private final EntornoGrid mundo;
    private final PoliticaGridV2 navegacion;

    public BucleAgenticoGrid(Agent agente, EntornoGrid mundo, PoliticaGridV2 navegacion) {
        super(agente, mundo, navegacion);
        this.mundo = mundo;
        this.navegacion = navegacion;
    }

    @Override
    protected void antes() {
        if (mundo.paradaSolicitada() || !mundo.isActivo() || mundo.isEnObjetivo()) {
            terminado = true;
        }
    }

    @Override
    protected void manejarError(Exception error) {
        if (error instanceof PoliticaGridV2.ParadaNavegacion) {
            System.err.println("Navegación Grid detenida: " + error.getMessage());
        } else {
            super.manejarError(error);
        }
    }

    @Override
    public void action() {
        if (terminado && mundo.esInteractivo() && !prepararSiguienteRun()) {
            return;
        }
        super.action();
    }

    private boolean prepararSiguienteRun() {
        try {
            mundo.terminarEjecucion();
        } catch (Exception e) {
            // Con un cierre pendiente no se permite abrir otro run; sí se permite Finalizar.
            mundo.bloquearReinicio(e.getMessage());
        }
        try {
            mundo.iniciarRun(); // Espera una orden de la ventana sin bloquear el EDT.
            navegacion.reiniciar();
            terminado = false;
            return true;
        } catch (Exception e) {
            manejarError(e);
            mundo.solicitarSalida();
            return false;
        }
    }

    @Override
    public boolean done() {
        // Terminar un run no equivale a cerrar la aplicación gráfica.
        return terminado && (!mundo.esInteractivo() || mundo.salidaSolicitada());
    }

    @Override
    public int onEnd() {
        if (!mundo.esInteractivo()) return super.onEnd();
        // takeDown cierra el run antes de notificar su estado final a la ventana.
        myAgent.doDelete();
        return 0;
    }
}
