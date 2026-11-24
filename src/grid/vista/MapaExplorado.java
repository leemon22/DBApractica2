package grid.vista;

import grid.Direccion;
import grid.Posicion;
import grid.SensoresGrid;
import java.util.HashMap;
import java.util.Map;

/** Memoria de presentación: solo conserva lo percibido, nunca decide movimientos. */
public final class MapaExplorado {
    public enum Celda { LIBRE, MURO }
    private final Map<Posicion, Celda> celdas = new HashMap<>();
    private Posicion agente;
    private Posicion objetivo;
    private int movimientos;

    public void reiniciar() {
        celdas.clear(); agente = null; objetivo = null; movimientos = 0;
    }

    public void percibir(SensoresGrid sensores) {
        agente = sensores.posicion(); objetivo = sensores.objetivo();
        celdas.put(agente, Celda.LIBRE);
        for (Direccion d : Direccion.values()) {
            celdas.put(agente.mover(d), sensores.bloqueado(d) ? Celda.MURO : Celda.LIBRE);
        }
    }

    /** La respuesta APPLIED confirma el desplazamiento incluso en la última acción. */
    public void aplicar(Direccion direccion) {
        if (agente != null) {
            agente = agente.mover(direccion);
            celdas.put(agente, Celda.LIBRE);
            movimientos++;
        }
    }

    public Posicion agente() { return agente; }
    public Posicion objetivo() { return objetivo; }
    public Map<Posicion, Celda> celdas() { return Map.copyOf(celdas); }
    public int movimientos() { return movimientos; }

    public record Limites(int minX, int minY, int maxX, int maxY) {
        public int columnas() { return maxX - minX + 1; }
        public int filas() { return maxY - minY + 1; }
    }

    public Limites limites() {
        if (agente == null) return new Limites(0, 0, 9, 9);
        int minX = Math.min(agente.x(), objetivo.x()), maxX = Math.max(agente.x(), objetivo.x());
        int minY = Math.min(agente.y(), objetivo.y()), maxY = Math.max(agente.y(), objetivo.y());
        for (Posicion p : celdas.keySet()) {
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
        }
        return new Limites(minX - 1, minY - 1, maxX + 1, maxY + 1);
    }
}
