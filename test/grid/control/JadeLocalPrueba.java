package grid.control;

import base.InfoReto;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.net.ServerSocket;
import java.util.List;

/** Comprueba parada y cierre de una plataforma JADE real con agentes locales sin HTTP. */
public final class JadeLocalPrueba {
    public static class AgenteLocal extends Agent {
        private SesionGrid sesion;
        @Override protected void setup() {
            sesion = (SesionGrid) getArguments()[1];
            addBehaviour(new TickerBehaviour(this, 20) {
                @Override protected void onTick() { if (sesion.detenerSolicitado()) doDelete(); }
            });
        }
        @Override protected void takeDown() { sesion.completar(null); }
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket puerto = new ServerSocket(0)) {
            System.setProperty("dba.jade.port", "" + puerto.getLocalPort());
        }
        MotorJadeGrid motor = new MotorJadeGrid(AgenteLocal.class.getName());
        try {
            for (int i = 0; i < 2; i++) {
                SesionGrid sesion = motor.iniciar(new InfoReto(i, "GRID_2D", List.of(), List.of()), null);
                sesion.solicitarParada(); sesion.esperarFin();
            }
        } finally { motor.cerrarPlataforma(); }
        System.out.println("JADE local: dos sesiones detenidas y plataforma cerrada, sin DBA Lab.");
        // Igual que el callback final del lanzador Swing: cerrar también los hilos residuales de JADE.
        System.exit(0);
    }
}
