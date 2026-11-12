package grid;

import base.Accion;

// Las cuatro direcciones cardinales del Grid. También es la acción que decide la política:
// cada movimiento es una dirección, y cada dirección conoce su código de acción en DBA Lab.
//
// El orden de declaración (N, E, S, O) es el sentido horario y, además, el desempate fijo del
// algoritmo del Tema 2 (arriba, derecha, abajo, izquierda): values() lo devuelve en ese orden.
//
// La coordenada y crece hacia abajo (fila desde arriba), así que el norte resta 1 a la y.
public enum Direccion implements Accion {
    NORTE(0, -1, "grid.move.north", "north"),
    ESTE(1, 0, "grid.move.east", "east"),
    SUR(0, 1, "grid.move.south", "south"),
    OESTE(-1, 0, "grid.move.west", "west");

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

    // Desplazamiento en columnas (x) al moverse en esta dirección.
    public int dx() {
        return dx;
    }

    // Desplazamiento en filas (y) al moverse en esta dirección.
    public int dy() {
        return dy;
    }

    // Código que se envía en POST /runs/{runId}/actions.
    public String codigoAccion() {
        return codigoAccion;
    }

    @Override
    public String getCodigoAccion() {
        return codigoAccion;
    }

    // Nombre del campo de esta dirección dentro del sensor grid.bumpers.
    public String campoBumper() {
        return campoBumper;
    }

    // --- Cálculo de giros del agente ---

    // Giro de 90º en sentido horario: N -> E -> S -> O -> N.
    public Direccion derecha() {
        return values()[(ordinal() + 1) % 4];
    }

    // Giro de 90º en sentido antihorario: N -> O -> S -> E -> N.
    public Direccion izquierda() {
        return values()[(ordinal() + 3) % 4];
    }

    // Media vuelta: N <-> S, E <-> O.
    public Direccion retroceso() {
        return values()[(ordinal() + 2) % 4];
    }
}
