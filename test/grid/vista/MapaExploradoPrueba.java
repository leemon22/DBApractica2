package grid.vista;

import grid.Direccion;
import grid.Posicion;
import grid.SensoresGrid;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import java.util.concurrent.atomic.AtomicInteger;
import grid.control.ControladorGrid;

/** Verificación sin VPN, token ni runs. Ejecutable con Run File. */
public final class MapaExploradoPrueba {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapaExplorado mapa = new MapaExplorado();
            mapa.percibir(new SensoresGrid(new Posicion(0, 0), new Posicion(1, 0),
                    Set.of(Direccion.NORTE, Direccion.OESTE), "FINITE", 10));
            exigir(mapa.celdas().get(new Posicion(-1, 0)) == MapaExplorado.Celda.MURO, "Borde negativo");
            exigir(!mapa.celdas().containsKey(new Posicion(4, 4)), "No inventar celdas exploradas");
            mapa.aplicar(Direccion.ESTE);
            exigir(mapa.agente().equals(mapa.objetivo()), "Dibujar llegada sin percepción posterior");
            mapa.aplicar(Direccion.OESTE);
            exigir(mapa.movimientos() == 2, "Contar movimientos, no posiciones distintas");
            int pequeno = mapa.limites().columnas();
            mapa.percibir(new SensoresGrid(new Posicion(49, 49), new Posicion(0, 0), Set.of(), "FINITE", 9));
            exigir(mapa.limites().columnas() > pequeno, "Crecimiento");
            pintar(mapa, 1000, 720, "grid-grande.png");
            pintar(mapa, 380, 250, "grid-compacto.png");
            mapa.reiniciar();
            mapa.percibir(new SensoresGrid(new Posicion(0, 0), new Posicion(2, 1), Set.of(), "FINITE", 10));
            exigir(mapa.limites().columnas() < 10 && mapa.movimientos() == 0, "Decrecimiento y reinicio");
            pintar(mapa, 800, 560, "grid-pequeno.png");
        });
        final javax.swing.JPanel[] contenido = new javax.swing.JPanel[1];
        final VisualizadorGridSwing[] vista = new VisualizadorGridSwing[1];
        SwingUtilities.invokeAndWait(() -> {
            vista[0] = new VisualizadorGridSwing(false);
            contenido[0] = vista[0].crearContenido();
        });
        AtomicInteger reinicios = new AtomicInteger(), finales = new AtomicInteger();
        vista[0].conectarAcciones(new ControladorGrid.Acciones() {
            @Override public void reiniciar() { reinicios.incrementAndGet(); }
            @Override public void finalizar() { finales.incrementAndGet(); }
        });
        vista[0].mostrarOperacion("Deteniendo…", true, false);
        SwingUtilities.invokeAndWait(() -> {
            JButton reiniciar = boton(contenido[0], "Reiniciar");
            exigir(!reiniciar.isEnabled(), "Reinicio bloqueado durante cierre"); reiniciar.doClick();
            exigir(reinicios.get() == 0, "No debe despachar botones bloqueados");
        });
        vista[0].mostrarOperacion("Preparado", false, false);
        SwingUtilities.invokeAndWait(() -> {
            boton(contenido[0], "Reiniciar").doClick(); boton(contenido[0], "Finalizar").doClick();
            exigir(reinicios.get() == 1 && finales.get() == 1, "La vista debe delegar órdenes al controlador");
        });
        vista[0].mostrarRetoEncontrado(new base.InfoReto(2, "GRID_2D", java.util.List.of(), java.util.List.of()));
        vista[0].mostrarInicio("demo", new base.EstadoControl("demo", base.EstadoControl.RunStatus.ACTIVE, null));
        for (int x = 0; x < 8; x++) {
            vista[0].mostrarPercepcion(new SensoresGrid(new Posicion(x, 3), new Posicion(9, 1),
                    x < 5 ? Set.of(Direccion.NORTE) : Set.of(), "FINITE", 100-x));
            vista[0].mostrarAccion(Direccion.ESTE, "APPLIED",
                    new base.EstadoControl("demo", base.EstadoControl.RunStatus.ACTIVE, null));
        }
        SwingUtilities.invokeAndWait(() -> {
            var raiz = contenido[0]; raiz.setSize(1120, 800); disponer(raiz);
            BufferedImage imagen = new BufferedImage(1120, 800, BufferedImage.TYPE_INT_RGB);
            var g = imagen.createGraphics(); raiz.printAll(g); g.dispose();
            try { ImageIO.write(imagen, "png", Path.of("build/preview-grid/interfaz.png").toFile()); }
            catch (java.io.IOException e) { throw new IllegalStateException(e); }
        });
        System.out.println("Mapa Swing: bordes, memoria parcial, llegada, pasos, crecimiento y reinicio correctos.");
    }
    private static void disponer(java.awt.Container contenedor) {
        contenedor.doLayout();
        for (var hijo : contenedor.getComponents()) if (hijo instanceof java.awt.Container c) disponer(c);
    }
    private static JButton boton(java.awt.Container contenedor, String texto) {
        for (var hijo : contenedor.getComponents()) {
            if (hijo instanceof JButton b && texto.equals(b.getText())) return b;
            if (hijo instanceof java.awt.Container c) {
                JButton resultado = boton(c, texto); if (resultado != null) return resultado;
            }
        }
        return null;
    }
    private static void exigir(boolean condicion, String motivo) {
        if (!condicion) throw new AssertionError(motivo);
    }
    private static void pintar(MapaExplorado mapa, int ancho, int alto, String nombre) {
        PanelMapa panel = new PanelMapa(mapa); panel.setSize(ancho, alto);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics(); panel.paint(g); g.dispose();
        try {
            Path salida = Path.of("build", "preview-grid", nombre);
            java.nio.file.Files.createDirectories(salida.getParent());
            ImageIO.write(imagen, "png", salida.toFile());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
