package lanzadores;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Punto de entrada del bloque Grid: arranca JADE y crea el agente grid.AgenteGrid.
 */
public class LanzadorGrid {

    public static void main(String[] args) {
        try {
            // Iniciar JADE con su interfaz gráfica
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "true");

            AgentContainer contenedor = runtime.createMainContainer(perfil);

            // Crear e iniciar el agente de navegación Grid
            AgentController agente = contenedor.createNewAgent(
                    "AgenteGrid",
                    "grid.AgenteGrid",
                    null
            );

            agente.start();

        } catch (Exception e) {
            System.err.println("Error durante el arranque de JADE para Grid: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
