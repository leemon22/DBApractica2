package termostato;

import base.Politica;

/**
 * Política de decisión para el termostato.
 * Si la temperatura actual es mayor o igual que la objetivo, enciende la refrigeración;
 * en caso contrario, la apaga.
 */
public class PoliticaTermostato implements Politica<SensoresTermostato, AccionTermostato> {

    @Override
    public AccionTermostato aplicar(SensoresTermostato sensores) {
        AccionTermostato accion;
        if (sensores.temperaturaActual() >= sensores.temperaturaObjetivo()) {
            accion = AccionTermostato.ON;
        } else {
            accion = AccionTermostato.OFF;
        }
        return accion;
    }
}
