package grid.vista;

import base.Visualizador;
import grid.Direccion;
import grid.SensoresGrid;
import grid.VisualizadorGridCli;
import grid.control.ControladorGrid;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.concurrent.FutureTask;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Construye la presentación elegida por el agente sin exponerle componentes Swing. */
public final class FabricaPresentacionGrid {
    public enum Tipo { SWING, CONSOLA }
    public record Presentacion(Visualizador<SensoresGrid, Direccion> vista, ControladorGrid control) { }
    private FabricaPresentacionGrid() { }
    /** null significa que el usuario canceló antes de crear cualquier run. */
    public static Tipo elegirTipo() {
        String configurado = System.getProperty("dba.grid.ui");
        if (configurado != null && !configurado.isBlank()) {
            return switch (configurado.trim().toLowerCase(Locale.ROOT)) {
                case "swing" -> Tipo.SWING;
                case "cli" -> Tipo.CONSOLA;
                default -> throw new IllegalArgumentException("dba.grid.ui debe ser swing o cli");
            };
        }
        if (GraphicsEnvironment.isHeadless()) return Tipo.CONSOLA;
        FutureTask<Tipo> pregunta = new FutureTask<>(() -> {
            Object[] opciones = {"Ventana Swing", "Consola"};
            int elegida = JOptionPane.showOptionDialog(null, "¿Qué interfaz quieres utilizar?", "Interfaz de Grid",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            return elegida == 0 ? Tipo.SWING : elegida == 1 ? Tipo.CONSOLA : null;
        });
        if (SwingUtilities.isEventDispatchThread()) pregunta.run(); else SwingUtilities.invokeLater(pregunta);
        try { return pregunta.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Selección interrumpida", e); }
        catch (java.util.concurrent.ExecutionException e) { throw new IllegalStateException("No se pudo elegir interfaz", e.getCause()); }
    }

    public static Presentacion crear() {
        Tipo tipo = elegirTipo();
        if (tipo == null) throw new IllegalStateException("Selección de interfaz cancelada");
        return crear(tipo);
    }

    public static Presentacion crear(Tipo tipo) {
        if (tipo == Tipo.CONSOLA) {
            return new Presentacion(new VisualizadorGridCli(), null);
        }
        VisualizadorGridSwing vista = new VisualizadorGridSwing();
        ControladorGrid control = new ControladorGrid(vista);
        vista.conectarAcciones(control.acciones());
        return new Presentacion(vista, control);
    }
}
