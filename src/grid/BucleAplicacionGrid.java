package grid;

import jade.core.Agent;

/** Mantiene un agente de aplicación entre runs; no depende de Swing ni de argumentos JADE. */
public final class BucleAplicacionGrid extends BucleAgenticoGrid {
    private final EntornoGrid mundo;
    private final PoliticaGridV2 navegacion;

    public BucleAplicacionGrid(Agent agente, EntornoGrid mundo, PoliticaGridV2 navegacion) {
        super(agente, mundo, navegacion);
        this.mundo = mundo;
        this.navegacion = navegacion;
    }
    @Override protected void antes() {
        if (mundo.paradaSolicitada()) { terminado = true; return; }
        super.antes();
    }
    @Override public void action() {
        if (terminado && mundo.esInteractivo()) {
            try { mundo.terminarEjecucion(); }
            catch (Exception e) { mundo.bloquearReinicio(e.getMessage()); }
            try {
                mundo.iniciarRun();
                navegacion.reiniciar();
                terminado = false;
            } catch (Exception e) {
                manejarError(e); terminado = true; mundo.solicitarSalida(); return;
            }
        }
        super.action();
    }
    @Override public boolean done() {
        return terminado && (!mundo.esInteractivo() || mundo.salidaSolicitada());
    }
}
