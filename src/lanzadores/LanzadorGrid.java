package lanzadores;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/** Arranca un agente sin argumentos. El entorno es propietario de la presentación. */
public class LanzadorGrid {
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);
            // Sin la ventana RMA de JADE; el puerto se puede cambiar con -Ddba.jade.port.
            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "false");
            perfil.setParameter(Profile.MAIN_PORT, System.getProperty("dba.jade.port", "1099"));
            AgentContainer contenedor = runtime.createMainContainer(perfil);
            if (contenedor == null)
                throw new IllegalStateException("No se pudo iniciar JADE; comprueba el puerto");
            AgentController agente =
                    contenedor.createNewAgent("AgenteGrid", "grid.AgenteGrid", null);
            agente.start();
            // JADE no se cierra solo al borrar el agente. Este hilo espera a que desaparezca
            // (getState() lanza StaleProxyException) y entonces apaga el contenedor.
            Thread cierre =
                    new Thread(
                            () -> {
                                try {
                                    while (true) {
                                        try {
                                            agente.getState();
                                        } catch (StaleProxyException terminado) {
                                            break;
                                        }
                                        Thread.sleep(100);
                                    }
                                    contenedor.kill();
                                    runtime.shutDown();
                                } catch (Exception e) {
                                    System.err.println("No se pudo cerrar JADE: " + e.getMessage());
                                }
                            },
                            "cierre-grid");
            cierre.start();
        } catch (Exception e) {
            System.err.println("Error al arrancar Grid: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
