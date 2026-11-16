package termostato;

/**
 * Parámetros invariantes del reto del termostato.
 * Agrupa los límites permitidos y la temperatura objetivo definidos al iniciar la simulación.
 *
 * @param inferior límite inferior permitido (°C)
 * @param superior límite superior permitido (°C)
 * @param objetivo temperatura objetivo orientativa (°C)
 */
public record LimitesTermostato(
        double inferior,
        double superior,
        double objetivo
) {
}
