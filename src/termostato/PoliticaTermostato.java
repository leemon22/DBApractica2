/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package termostato;

import base.Politica;

/**
 *
 * @author martinhernandezruiz
 */
public class PoliticaTermostato implements Politica<SensoresTermostato, AccionTermostato> {

    @Override
    public AccionTermostato aplicar(SensoresTermostato sensores) {

        double temperatura = sensores.temperatura();
        double inferior = sensores.limiteInferior();
        double superior = sensores.limiteSuperior();
        double objetivo = sensores.orientativa();

        // Si la temperatura alcanza el limite superior,
        // encendemos la refrigeracion.
        if (temperatura >= superior) {
            return AccionTermostato.ON;
        }

        // Si alcanza el limite inferior,
        // apagamos la refrigeracion.
        if (temperatura <= inferior) {
            return AccionTermostato.OFF;
        }

        // Dentro del intervalo utilizamos la
        // temperatura orientativa como referencia.
        if (temperatura > objetivo) {
            return AccionTermostato.ON;
        }

        return AccionTermostato.OFF;
    }
}
