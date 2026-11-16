package grid;

import base.Accion;

/**
 * Las cuatro direcciones cardinales del Grid y acción física del agente.
 * Cada movimiento representa un desplazamiento unitario en la cuadrícula.
 * <p>
 * El orden de declaración (NORTE, ESTE, SUR, OESTE) sigue el sentido horario y
 * actúa como criterio fijo de desempate en el algoritmo de búsqueda.
 */
public enum Direccion implements Accion {

    NORTE(0, -1, "grid.move.north", "north"),
    ESTE(1, 0, "grid.move.east", "east"),
    SUR(0, 1, "grid.move.south", "south"),
    OESTE(-1, 0, "grid.move.west", "west");

    // Matriz de valores en caché para evitar la clonación continua del array interno en cada llamada
    private static final Direccion[] VALORES = values();

    private final int dx;
    private final int dy;
    private final String codigoAccion;
    private final String campoBumper;

    Direccion(int dx, int dy, String codigoAccion, String campoBumper) {
        this.dx = dx;
        this.dy = dy;
        this.codigoAccion = codigoAccion;
        this.campoBumper = campoBumper;
    }

    /**
     * Desplazamiento en columnas (x) al moverse en esta dirección.
     */
    public int dx() {
        return dx;
    }

    /**
     * Desplazamiento en filas (y) al moverse en esta dirección.
     */
    public int dy() {
        return dy;
    }

    /**
     * Código oficial de la acción para enviar a la API de DBA Lab.
     */
    public String codigoAccion() {
        return codigoAccion;
    }

    /**
     * Nombre del campo correspondiente en el sensor {@code grid.bumpers}.
     */
    public String campoBumper() {
        return campoBumper;
    }

    // --- Cálculo de giros del agente ---

    /**
     * Giro de 90° en sentido horario: NORTE -&gt; ESTE -&gt; SUR -&gt; OESTE -&gt; NORTE.
     */
    public Direccion derecha() {
        return VALORES[(ordinal() + 1) % 4];
    }

    /**
     * Giro de 90° en sentido antihorario: NORTE -&gt; OESTE -&gt; SUR -&gt; ESTE -&gt; NORTE.
     */
    public Direccion izquierda() {
        return VALORES[(ordinal() + 3) % 4];
    }

    /**
     * Media vuelta (180°): NORTE &lt;-&gt; SUR, ESTE &lt;-&gt; OESTE.
     */
    public Direccion retroceso() {
        return VALORES[(ordinal() + 2) % 4];
    }
}
