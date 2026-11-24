package grid.vista;

import base.Visualizador;

import grid.Direccion;
import grid.SensoresGrid;
import grid.VisualizadorGridCli;
import grid.control.ControladorGrid;

import java.awt.GraphicsEnvironment;
import java.util.Locale;

import javax.swing.JOptionPane;

/** Construye la presentación elegida por el agente sin exponerle componentes Swing. */
public final class FabricaPresentacionGrid {
    public enum Tipo {
        SWING,
        CONSOLA
    }

    public record Presentacion(
            Visualizador<SensoresGrid, Direccion> vista, ControladorGrid control) {}

    private FabricaPresentacionGrid() {}

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
        return VisualizadorGridSwing.consultarEnEdt(() -> {
            Object[] opciones = {"Ventana Swing", "Consola"};
            int elegida =
                    JOptionPane.showOptionDialog(
                            null,
                            "¿Qué interfaz quieres utilizar?",
                            "Interfaz de Grid",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            opciones,
                            opciones[0]);
            return elegida == 0 ? Tipo.SWING : elegida == 1 ? Tipo.CONSOLA : null;
        });
    }

    public static Presentacion crear(Tipo tipo) {
        java.util.Objects.requireNonNull(tipo, "Elige un tipo de interfaz antes de crearla");
        if (tipo == Tipo.CONSOLA) {
            return new Presentacion(new VisualizadorGridCli(), null);
        }
        VisualizadorGridSwing vista = new VisualizadorGridSwing();
        ControladorGrid control = new ControladorGrid(vista);
        vista.conectarAcciones(control::reiniciar, control::finalizar);
        return new Presentacion(vista, control);
    }
}
