package grid.vista;

import grid.Posicion;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map;

import javax.swing.JPanel;

/** Cuadrícula ajustada al tamaño de la ventana; no conoce JADE ni la API. */
public final class PanelMapa extends JPanel {
    public static final Color FONDO = new Color(0x101B2A);
    public static final Color DESCONOCIDA = new Color(0x233247);
    public static final Color LIBRE = new Color(0xD9E7ED);
    public static final Color MURO = new Color(0x8B2635);
    public static final Color AGENTE = new Color(0x53E0BD);
    public static final Color OBJETIVO = new Color(0xFFBF69);
    private final MapaExplorado mapa;

    public PanelMapa(MapaExplorado mapa) {
        this.mapa = mapa;
        setBackground(FONDO);
        setPreferredSize(new Dimension(760, 650));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (mapa.agente() == null) {
                g.setColor(LIBRE);
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("El mapa aparecerá con la primera percepción", 30, getHeight() / 2);
                return;
            }
            var b = mapa.limites();
            double celda =
                    Math.min(
                            Math.max(1, getWidth() - 64) / (double) b.columnas(),
                            Math.max(1, getHeight() - 64) / (double) b.filas());
            double ox = (getWidth() - b.columnas() * celda) / 2;
            double oy = (getHeight() - b.filas() * celda) / 2;
            Map<Posicion, MapaExplorado.Celda> conocidas = mapa.celdas();
            for (int y = b.minY(); y <= b.maxY(); y++) {
                for (int x = b.minX(); x <= b.maxX(); x++) {
                    var tipo = conocidas.get(new Posicion(x, y));
                    g.setColor(
                            tipo == null
                                    ? DESCONOCIDA
                                    : tipo == MapaExplorado.Celda.MURO ? MURO : LIBRE);
                    double px = ox + (x - b.minX()) * celda, py = oy + (y - b.minY()) * celda;
                    double separacion = celda >= 5 ? 1 : 0;
                    g.fill(new Rectangle2D.Double(px, py, celda - separacion, celda - separacion));
                }
            }
            // El objetivo se dibuja primero: sigue visible como aro si coincide con el agente.
            marcador(g, mapa.objetivo(), b, celda, ox, oy, false);
            marcador(g, mapa.agente(), b, celda, ox, oy, true);
            if (celda >= 18) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(LIBRE);
                int salto = celda >= 30 ? 1 : 5;
                for (int x = b.minX(); x <= b.maxX(); x++)
                    if (Math.floorMod(x, salto) == 0)
                        g.drawString(
                                "" + x,
                                (float) (ox + (x - b.minX() + .3) * celda),
                                (float) (oy - 9));
                for (int y = b.minY(); y <= b.maxY(); y++)
                    if (Math.floorMod(y, salto) == 0)
                        g.drawString(
                                "" + y,
                                (float) (ox - 25),
                                (float) (oy + (y - b.minY() + .6) * celda));
            }
        } finally {
            g.dispose();
        }
    }

    private void marcador(
            Graphics2D g,
            Posicion p,
            MapaExplorado.Limites b,
            double c,
            double ox,
            double oy,
            boolean agente) {
        double x = ox + (p.x() - b.minX()) * c, y = oy + (p.y() - b.minY()) * c;
        g.setColor(agente ? AGENTE : OBJETIVO);
        if (agente) {
            g.fill(new Ellipse2D.Double(x + c * .23, y + c * .23, c * .54, c * .54));
            g.setColor(FONDO);
            g.setStroke(new BasicStroke((float) Math.max(1, c * .04)));
            g.draw(new Ellipse2D.Double(x + c * .23, y + c * .23, c * .54, c * .54));
        } else {
            g.setStroke(new BasicStroke((float) Math.max(1, c * .09)));
            g.draw(new Rectangle2D.Double(x + c * .12, y + c * .12, c * .76, c * .76));
        }
    }
}
