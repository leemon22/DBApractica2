package grid.control;

import base.ClienteHttpDba;
import base.InfoReto;
import base.Visualizador;
import grid.Direccion;
import grid.SensoresGrid;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import java.util.List;

/** Adaptador del controlador al ciclo de vida de JADE. La ventana no lo conoce. */
public final class MotorJadeGrid implements ControladorGrid.Motor {
    private AgentContainer contenedor;
    private final String claseAgente;
    private int numero;

    public MotorJadeGrid() { this("grid.AgenteGrid"); }
    MotorJadeGrid(String claseAgente) { this.claseAgente = claseAgente; }

    @Override public List<InfoReto> descubrirRetos() throws Exception {
        return new ClienteHttpDba().getRetos().stream()
                .filter(reto -> "GRID_2D".equalsIgnoreCase(reto.environmentType())).toList();
    }

    @Override public SesionGrid iniciar(InfoReto reto, Visualizador<SensoresGrid, Direccion> vista) throws Exception {
        if (contenedor == null) {
            jade.core.Runtime.instance().setCloseVM(false); // La ventana sigue disponible tras cada run.
            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "false");
            perfil.setParameter(Profile.MAIN_PORT, System.getProperty("dba.jade.port", "1099"));
            contenedor = jade.core.Runtime.instance().createMainContainer(perfil);
            if (contenedor == null) throw new IllegalStateException("No se pudo iniciar JADE; comprueba su puerto");
        }
        SesionGrid sesion = new SesionGrid(reto.id());
        contenedor.createNewAgent("AgenteGrid-" + (++numero), claseAgente, new Object[]{vista, sesion}).start();
        return sesion;
    }

    @Override public void cerrarPlataforma() throws Exception {
        if (contenedor != null) {
            // Se llama desde el controlador, después del takeDown del agente, nunca desde dentro del agente.
            try { contenedor.kill(); }
            finally { jade.core.Runtime.instance().shutDown(); contenedor = null; }
        }
    }
}
