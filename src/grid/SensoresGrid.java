package grid;

import base.Sensor;
import java.util.Objects;
import java.util.Set;

// Percepción del agente en un paso de Grid, construida por el entorno a partir de los sensores
// grid.position, grid.target, grid.bumpers y grid.energy.
//
// bloqueadas: direcciones cuyo bumper vale true (obstáculo o límite del mapa). Moverse hacia
// una de ellas termina el run.
// modoEnergia y energiaRestante: campos mode y remaining de grid.energy.
public record SensoresGrid(
        Posicion posicion,
        Posicion objetivo,
        Set<Direccion> bloqueadas,
        String modoEnergia,
        int energiaRestante) implements Sensor {

    public SensoresGrid {
        Objects.requireNonNull(posicion, "posicion");
        Objects.requireNonNull(objetivo, "objetivo");
        // Copia inmutable: la política no puede alterar la percepción del entorno.
        bloqueadas = Set.copyOf(bloqueadas);
    }

    public boolean bloqueado(Direccion direccion) {
        return bloqueadas.contains(direccion);
    }

    public boolean libre(Direccion direccion) {
        return !bloqueado(direccion);
    }

    // h(pos): distancia Manhattan desde la posición actual hasta el objetivo.
    public int distanciaAlObjetivo() {
        return posicion.distanciaManhattan(objetivo);
    }

    public boolean enObjetivo() {
        return posicion.equals(objetivo);
    }
}
