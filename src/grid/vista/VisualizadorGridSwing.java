package grid.vista;

import base.EstadoControl;
import base.InfoReto;
import grid.control.ControladorGrid;
import grid.Direccion;
import grid.SensoresGrid;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.FutureTask;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Adaptador de presentación. Todos sus datos y widgets se actualizan en el EDT.
 * No recibe referencias al agente, a la política ni al cliente HTTP. */
public final class VisualizadorGridSwing implements ControladorGrid.Vista {
    private final MapaExplorado mapa = new MapaExplorado();
    private JFrame ventana;
    private PanelMapa panelMapa;
    private JLabel estado, reto, energia, pasos, posicion, objetivo, distancia, mensaje;
    private JButton reiniciar, finalizar;
    private ControladorGrid.Acciones acciones;
    private JDialog seleccion;
    private boolean finalizando;

    public VisualizadorGridSwing() {
        this(true);
    }

    // Permite verificar el panel sin crear ventanas ni conectar al laboratorio.
    VisualizadorGridSwing(boolean abrirVentana) {
        if (abrirVentana) SwingUtilities.invokeLater(this::crearVentana);
    }

    private void crearVentana() {
        ventana = new JFrame("DBA · Explorador Grid");
        ventana.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        ventana.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (acciones != null) acciones.finalizar();
                else ventana.dispose();
            }
        });
        ventana.setContentPane(crearContenido()); ventana.setMinimumSize(new Dimension(820, 560));
        ventana.setSize(1120, 800); ventana.setLocationByPlatform(true); ventana.setVisible(true);
    }

    JPanel crearContenido() {
        JPanel raiz = new JPanel(new BorderLayout(20, 20));
        raiz.setBackground(PanelMapa.FONDO); raiz.setBorder(new EmptyBorder(24, 24, 20, 24));
        JPanel cabecera = columna();
        JLabel titulo = texto("EXPLORADOR / GRID", 24, PanelMapa.LIBRE);
        cabecera.add(titulo);
        cabecera.add(texto("DBA · Navegación autónoma · UGR", 13, new Color(0x94A8BE)));
        JPanel superior = new JPanel(new BorderLayout(16, 0)); superior.setOpaque(false);
        superior.add(cabecera, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); botones.setOpaque(false);
        reiniciar = new JButton("Reiniciar"); finalizar = new JButton("Finalizar");
        reiniciar.setToolTipText("Cerrar la ejecución actual y volver a elegir nivel");
        finalizar.setToolTipText("Cerrar el run y salir de la aplicación");
        reiniciar.setEnabled(acciones != null); finalizar.setEnabled(acciones != null);
        reiniciar.addActionListener(e -> { if (acciones != null) acciones.reiniciar(); });
        finalizar.addActionListener(e -> { if (acciones != null) acciones.finalizar(); });
        for (JButton boton : new JButton[]{reiniciar, finalizar}) {
            boton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            boton.setFont(new Font("SansSerif", Font.BOLD, 13));
            boton.setForeground(PanelMapa.FONDO);
            boton.setBackground(boton == reiniciar ? PanelMapa.AGENTE : PanelMapa.OBJETIVO);
            boton.setMargin(new Insets(10, 18, 10, 18)); botones.add(boton);
        }
        superior.add(botones, BorderLayout.EAST);
        raiz.add(superior, BorderLayout.NORTH);
        panelMapa = new PanelMapa(mapa); raiz.add(panelMapa, BorderLayout.CENTER);
        JPanel lateral = columna(); lateral.setPreferredSize(new Dimension(230, 400));
        estado = dato(lateral, "ESTADO", "Esperando reto");
        reto = dato(lateral, "RETO / RUN", "—");
        energia = dato(lateral, "ÚLTIMA ENERGÍA PERCIBIDA", "—");
        pasos = dato(lateral, "MOVIMIENTOS APLICADOS", "0");
        posicion = dato(lateral, "POSICIÓN", "—");
        objetivo = dato(lateral, "OBJETIVO", "—");
        distancia = dato(lateral, "DISTANCIA MANHATTAN", "—");
        raiz.add(lateral, BorderLayout.EAST);
        JPanel pie = columna();
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        leyenda.setOpaque(false);
        leyenda.add(texto("■ Sin explorar", 12, PanelMapa.DESCONOCIDA.brighter()));
        leyenda.add(texto("■ Libre", 12, PanelMapa.LIBRE));
        leyenda.add(texto("■ Muro / borde", 12, PanelMapa.MURO.brighter()));
        leyenda.add(texto("● Agente", 12, PanelMapa.AGENTE));
        leyenda.add(texto("□ Objetivo", 12, PanelMapa.OBJETIVO));
        pie.add(leyenda);
        mensaje = texto("Descubriendo retos disponibles…", 13, new Color(0x94A8BE)); pie.add(mensaje);
        raiz.add(pie, BorderLayout.SOUTH);
        return raiz;
    }

    private static JPanel columna() {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p;
    }
    private static JLabel texto(String valor, int tamano, Color color) {
        JLabel label = new JLabel(valor); label.setFont(new Font("SansSerif", Font.PLAIN, tamano));
        label.setForeground(color); return label;
    }
    private static JLabel dato(JPanel padre, String nombre, String valor) {
        padre.add(texto(nombre, 10, new Color(0x94A8BE))); padre.add(Box.createVerticalStrut(6));
        JLabel label = texto(valor, 19, PanelMapa.LIBRE); padre.add(label); padre.add(Box.createVerticalStrut(24));
        return label;
    }
    private void publicar(Runnable evento) { SwingUtilities.invokeLater(evento); }

    public void conectarAcciones(ControladorGrid.Acciones acciones) {
        publicar(() -> { this.acciones = acciones;
            reiniciar.setEnabled(!finalizando); finalizar.setEnabled(!finalizando); });
    }

    @Override public void mostrarOperacion(String valor, boolean ocupado, boolean cerrando) {
        publicar(() -> {
            // Un evento tardío del arranque no puede reabrir la selección tras Finalizar.
            if (finalizando && !cerrando) return;
            finalizando |= cerrando;
            reiniciar.setEnabled(acciones != null && !ocupado && !finalizando);
            finalizar.setEnabled(acciones != null && !finalizando);
            mensaje.setText(valor); mensaje.setToolTipText(valor); });
    }

    @Override public void cancelarSeleccion() {
        publicar(() -> { finalizando = true; if (seleccion != null) seleccion.dispose(); });
    }

    @Override public void cerrarVentana(String aviso) {
        FutureTask<Void> cerrar = new FutureTask<>(() -> {
            if (aviso != null) JOptionPane.showMessageDialog(ventana, aviso, "Cierre de Grid", JOptionPane.WARNING_MESSAGE);
            if (ventana != null) ventana.dispose();
            return null;
        });
        if (SwingUtilities.isEventDispatchThread()) cerrar.run(); else publicar(cerrar);
        try { cerrar.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (java.util.concurrent.ExecutionException e) { throw new IllegalStateException("No se pudo cerrar la ventana", e.getCause()); }
    }

    @Override public InfoReto seleccionarReto(List<InfoReto> retos) {
        if (retos.isEmpty()) throw new IllegalArgumentException("No hay retos Grid disponibles");
        FutureTask<InfoReto> seleccion = new FutureTask<>(() -> {
            if (finalizando) throw new ControladorGrid.SeleccionCancelada();
            String[] opciones = retos.stream().map(r -> "Reto " + r.id() + " · " + r.environmentType()).toArray(String[]::new);
            JOptionPane panel = new JOptionPane("Selecciona un reto descubierto en DBA Lab", JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            panel.setWantsInput(true); panel.setSelectionValues(opciones); panel.setInitialSelectionValue(opciones[0]);
            this.seleccion = panel.createDialog(ventana, "Elegir nivel");
            try {
                this.seleccion.setVisible(true);
                Object elegido = panel.getInputValue();
                if (finalizando || !Integer.valueOf(JOptionPane.OK_OPTION).equals(panel.getValue())
                        || elegido == JOptionPane.UNINITIALIZED_VALUE || elegido == null) {
                    throw new ControladorGrid.SeleccionCancelada();
                }
                for (int i = 0; i < opciones.length; i++) if (opciones[i].equals(elegido)) return retos.get(i);
            } finally { this.seleccion.dispose(); this.seleccion = null; }
            throw new IllegalStateException("Selección desconocida");
        });
        if (SwingUtilities.isEventDispatchThread()) seleccion.run(); else publicar(seleccion);
        try { return seleccion.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Selección interrumpida", e); }
        catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof ControladorGrid.SeleccionCancelada cancelada) throw cancelada;
            throw new IllegalStateException(e.getCause().getMessage(), e.getCause());
        }
    }

    @Override public void mostrarRetoEncontrado(InfoReto info) {
        publicar(() -> { mapa.reiniciar(); reto.setText("Reto " + info.id()); pasos.setText("0");
            energia.setText("—"); posicion.setText("—"); objetivo.setText("—"); distancia.setText("—");
            estado.setForeground(PanelMapa.LIBRE);
            estado.setText("Preparando"); mensaje.setText("Capacidades del reto verificadas"); panelMapa.repaint(); });
    }
    @Override public void mostrarInicio(String runId, EstadoControl control) {
        publicar(() -> { estado.setText(control.runStatus().toString());
            reto.setText(reto.getText() + " / " + runId); mensaje.setText("Exploración en curso"); });
    }
    @Override public void mostrarPercepcion(SensoresGrid sensores) {
        publicar(() -> { mapa.percibir(sensores);
            energia.setText("INFINITE".equals(sensores.modoEnergia()) ? "∞" : "" + sensores.energiaRestante());
            actualizarMapa(); });
    }
    private void actualizarMapa() {
        if (mapa.agente() != null) {
            posicion.setText("(" + mapa.agente().x() + ", " + mapa.agente().y() + ")");
            objetivo.setText("(" + mapa.objetivo().x() + ", " + mapa.objetivo().y() + ")");
            distancia.setText("" + mapa.agente().distanciaManhattan(mapa.objetivo()));
        }
        pasos.setText("" + mapa.movimientos()); panelMapa.repaint();
    }
    @Override public void mostrarAccion(Direccion accion, String resultado, EstadoControl control) {
        publicar(() -> { if ("APPLIED".equals(resultado)) mapa.aplicar(accion);
            estado.setText(control.runStatus().toString());
            mensaje.setText(accion + " · " + resultado + control.motivo().map(m -> " · " + m).orElse(""));
            actualizarMapa(); });
    }
    @Override public void mostrarCierre(String runId) {
        publicar(() -> mensaje.setText("Solicitando cierre del run " + runId));
    }
    @Override public void mostrarFin(EstadoControl control) {
        publicar(() -> { estado.setText(control.runStatus().toString());
            estado.setForeground(control.runStatus() == EstadoControl.RunStatus.SUCCEEDED ? PanelMapa.AGENTE : PanelMapa.OBJETIVO);
            mensaje.setText(control.motivo().orElse("Fin del bucle") + " · " + mapa.celdas().size() + " celdas descubiertas"); });
    }
    @Override public void mostrarMensaje(String valor) { publicar(() -> mensaje.setText(valor)); }
}
