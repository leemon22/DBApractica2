package base;

/**
 * Contenedor inmutable que agrupa el entorno y el bucle agéntico que componen un agente.
 *
 * @param <S> tipo de percepciones (Sensor)
 * @param <A> tipo de acciones (Accion)
 * @param entorno entorno con el que interactúa el agente
 * @param bucle   comportamiento que ejecuta el ciclo de percepción-decisión-acción
 */
public record ComponentesAgente<S extends Sensor, A extends Accion>(
        Entorno<S, A> entorno,
        BucleAgentico<S, A> bucle) {
}
