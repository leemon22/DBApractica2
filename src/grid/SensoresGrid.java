package grid;

import base.Sensor;
import java.util.Objects;
import java.util.Set;

/**
 * Percepciones del agente en un paso de Grid.
 * Construidas por el entorno a partir de los sensores de posición, objetivo, bumpers y energía.
 *
 * @param posicion        coordenadas (x, y) actuales del agente
 * @param objetivo        coordenadas (x, y) de la celda destino
 * @param bloqueadas      conjunto de direcciones adyacentes bloqueadas por obstáculo o límite
 * @param modoEnergia     modo de energía del reto ("FINITE" o "INFINITE")
 * @param energiaRestante unidades de energía disponibles antes de que expire el run
 */
public record SensoresGrid(
        Posicion posicion,
        Posicion objetivo,
        Set<Direccion> bloqueadas,
        String modoEnergia,
        int energiaRestante) implements Sensor {

    public SensoresGrid {
        Objects.requireNonNull(posicion, "La posición no puede ser nula");
        Objects.requireNonNull(objetivo, "El objetivo no puede ser nulo");
        // Copia inmutable: la política no puede alterar la percepción interna del entorno
        bloqueadas = bloqueadas != null ? Set.copyOf(bloqueadas) : Set.of();
    }

    /**
     * Comprueba si una dirección está bloqueada por un obstáculo o muro.
     */
    public boolean bloqueado(Direccion direccion) {
        return bloqueadas.contains(direccion);
    }

    /**
     * Comprueba si una dirección está libre para transitar.
     */
    public boolean libre(Direccion direccion) {
        return !bloqueado(direccion);
    }

    /**
     * Heurística Manhattan h(n) desde la posición actual hasta el objetivo.
     */
    public int distanciaAlObjetivo() {
        return posicion.distanciaManhattan(objetivo);
    }

    /**
     * Comprueba si el agente ha alcanzado la celda objetivo.
     */
    public boolean enObjetivo() {
        return posicion.equals(objetivo);
    }
}
