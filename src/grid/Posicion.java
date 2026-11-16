package grid;

import java.util.Objects;

/**
 * Celda de la cuadrícula bidimensional de Grid.
 * Coordenada x representa la columna desde la izquierda e y la fila desde arriba.
 * Inmutable: cualquier desplazamiento devuelve una nueva instancia.
 *
 * @param x columna (desde la izquierda, 0-indexada)
 * @param y fila (desde arriba, 0-indexada)
 */
public record Posicion(int x, int y) {

    /**
     * Calcula la distancia Manhattan (|dx| + |dy|) hacia otra celda,
     * correspondiente a la heurística h(n) para la navegación sin diagonales.
     *
     * @param otra celda de destino
     * @return distancia Manhattan en número de celdas
     */
    public int distanciaManhattan(Posicion otra) {
        Objects.requireNonNull(otra, "La celda de destino no puede ser nula");
        return Math.abs(x - otra.x) + Math.abs(y - otra.y);
    }

    /**
     * Devuelve una nueva posición resultante de avanzar una casilla en la dirección indicada.
     *
     * @param direccion dirección cardinal del movimiento
     * @return nueva celda adyacente
     */
    public Posicion mover(Direccion direccion) {
        Objects.requireNonNull(direccion, "La dirección no puede ser nula");
        return new Posicion(x + direccion.dx(), y + direccion.dy());
    }
}
