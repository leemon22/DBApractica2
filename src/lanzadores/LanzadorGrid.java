package lanzadores;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import grid.VisualizadorGridCli;
import grid.vista.VisualizadorGridSwing;
import grid.control.ControladorGrid;
import grid.control.MotorJadeGrid;

/**
 * Punto de entrada del bloque Grid: arranca JADE y crea el agente grid.AgenteGrid.
 */
public class LanzadorGrid {

    public static void main(String[] args) {
        if (!"cli".equalsIgnoreCase(System.getProperty("dba.grid.ui", "swing"))) {
            VisualizadorGridSwing vista = new VisualizadorGridSwing();
            // JADE conserva hilos internos: salir solo después del cierre coordinado.
            ControladorGrid controlador = new ControladorGrid(vista, new MotorJadeGrid(), () -> System.exit(0));
            vista.conectarAcciones(controlador.acciones());
            controlador.reiniciar();
            return;
        }
        try {
            // Iniciar JADE con su interfaz gráfica
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "false");
            // Permite ejecutar una plataforma de prueba sin interferir con otra ya abierta.
            perfil.setParameter(Profile.MAIN_PORT, System.getProperty("dba.jade.port", "1099"));

            AgentContainer contenedor = runtime.createMainContainer(perfil);

            // Crear e iniciar el agente de navegación Grid
            AgentController agente = contenedor.createNewAgent(
                    "AgenteGrid",
                    "grid.AgenteGrid",
                    new Object[]{new VisualizadorGridCli()}
            );

            agente.start();

        } catch (Exception e) {
            System.err.println("Error durante el arranque de JADE para Grid: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
