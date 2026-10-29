package termostato;

/**
 * Percepción del termostato: temperatura actual, límites permitidos y temperatura orientativa.
 *
 * @author martinhernandezruiz
 */
public record SensoresTermostato(double temperatura, double limiteInferior, double limiteSuperior,double orientativa) {

}
