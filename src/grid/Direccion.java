package grid;

import base.Accion;

/**
 * Las cuatro direcciones cardinales del Grid y acción física del agente. Cada movimiento representa
 * un desplazamiento unitario en la cuadrícula.
 *
 * <p>El orden de declaración (NORTE, ESTE, SUR, OESTE) sigue el sentido horario y actúa como
 * criterio fijo de desempate en el algoritmo de búsqueda.
 */
public enum Direccion implements Accion {
    NORTE(0, -1),
    ESTE(1, 0),
    SUR(0, 1),
    OESTE(-1, 0);

    // Matriz de valores en caché para evitar la clonación continua del array interno en cada
    // llamada
    private static final Direccion[] VALORES = values();

    private final int dx;
    private final int dy;

    Direccion(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Desplazamiento en columnas (x) al moverse en esta dirección. */
    public int dx() {
        return dx;
    }

    /** Desplazamiento en filas (y) al moverse en esta dirección. */
    public int dy() {
        return dy;
    }

    /** Giro de 90° en sentido horario: NORTE -&gt; ESTE -&gt; SUR -&gt; OESTE -&gt; NORTE. */
    public Direccion derecha() {
        return VALORES[(ordinal() + 1) % 4];
    }

    /** Giro de 90° en sentido antihorario: NORTE -&gt; OESTE -&gt; SUR -&gt; ESTE -&gt; NORTE. */
    public Direccion izquierda() {
        return VALORES[(ordinal() + 3) % 4];
    }

    /** Media vuelta (180°): NORTE &lt;-&gt; SUR, ESTE &lt;-&gt; OESTE. */
    public Direccion retroceso() {
        return VALORES[(ordinal() + 2) % 4];
    }
}
