package grid;

// Celda de la cuadrícula. x es la columna desde la izquierda e y la fila desde arriba.
// Es inmutable: moverse devuelve una posición nueva.
public record Posicion(int x, int y) {

    // Distancia Manhattan (|dx| + |dy|): la heurística h del algoritmo.
    // Con obstáculos, el recorrido real puede ser mayor.
    public int distanciaManhattan(Posicion otra) {
        return Math.abs(x - otra.x) + Math.abs(y - otra.y);
    }

    // Celda vecina en la dirección indicada.
    // Las comprobaciones las hacen los bumpers.
    public Posicion mover(Direccion direccion) {
        return new Posicion(x + direccion.dx(), y + direccion.dy());
    }
}
