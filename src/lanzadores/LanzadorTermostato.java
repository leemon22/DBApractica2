package lanzadores;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Punto de entrada del bloque Termostato: arranca JADE y crea el agente termostato.AgenteTermostato.
 */
public class LanzadorTermostato {

    public static void main(String[] args) {
        try {
            // Iniciar JADE con su interfaz gráfica.
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "true");

            AgentContainer contenedor =
                    runtime.createMainContainer(perfil);

            // Crear e iniciar el agente del termostato.
            AgentController agente = contenedor.createNewAgent(
                    "AgenteTermostato",
                    "termostato.AgenteTermostato",
                    null
            );

            agente.start();

        } catch (Exception e) {
            System.err.println(
                    "Error al iniciar JADE: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}
