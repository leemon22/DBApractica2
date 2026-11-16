package termostato;

import base.Sensor;
import java.util.Objects;

/**
 * Percepciones físicas del termostato.
 * Combina la lectura dinámica de temperatura actual con los límites invariantes del reto.
 *
 * @param temperaturaActual lectura de temperatura en el paso actual (°C)
 * @param limites           límites permitidos y temperatura objetivo del reto
 */
public record SensoresTermostato(
        double temperaturaActual,
        LimitesTermostato limites
) implements Sensor {

    public SensoresTermostato {
        Objects.requireNonNull(limites, "Los límites del termostato no pueden ser nulos");
    }

    /**
     * Constructor de conveniencia que agrupa los parámetros invariantes automáticamente.
     */
    public SensoresTermostato(
            double temperaturaActual,
            double limiteInferior,
            double limiteSuperior,
            double temperaturaObjetivo) {
        this(temperaturaActual, new LimitesTermostato(limiteInferior, limiteSuperior, temperaturaObjetivo));
    }

    /**
     * Límite inferior permitido para la temperatura (°C).
     */
    public double limiteInferior() {
        return limites.inferior();
    }

    /**
     * Límite superior permitido para la temperatura (°C).
     */
    public double limiteSuperior() {
        return limites.superior();
    }

    /**
     * Temperatura objetivo orientativa (°C).
     */
    public double temperaturaObjetivo() {
        return limites.objetivo();
    }
}
