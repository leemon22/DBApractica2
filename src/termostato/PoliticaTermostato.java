package termostato;

import base.Politica;

import java.util.Objects;

/**
 * Política de decisión para el termostato. Si la temperatura actual es mayor o igual que la
 * objetivo, decide encender la refrigeración (ON); en caso contrario, la apaga (OFF).
 */
public class PoliticaTermostato implements Politica<SensoresTermostato, AccionTermostato> {

    @Override
    public AccionTermostato aplicar(SensoresTermostato sensores) {
        Objects.requireNonNull(sensores, "Los sensores no pueden ser nulos");

        return sensores.temperaturaActual() >= sensores.temperaturaObjetivo()
                ? AccionTermostato.ON
                : AccionTermostato.OFF;
    }
}
