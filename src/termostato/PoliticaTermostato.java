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
public class PoliticaTermostato
        implements Politica<SensoresTermostato, AccionTermostato> {

    @Override
    public AccionTermostato aplicar(SensoresTermostato sensores) {

        double temperatura = sensores.temperatura();

        // Si alcanza el límite superior, encender.
        if (temperatura >= sensores.limiteSuperior()) {
            return AccionTermostato.ON;
        }

        // Si alcanza el límite inferior, apagar.
        if (temperatura <= sensores.limiteInferior()) {
            return AccionTermostato.OFF;
        }

        // Dentro del intervalo, utilizar el objetivo como referencia.
        if (temperatura > sensores.orientativa()) {
            return AccionTermostato.ON;
        }

        return AccionTermostato.OFF;
    }
}
