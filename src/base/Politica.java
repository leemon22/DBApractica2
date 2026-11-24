package base;

/**
 * Interfaz que define la política de decisión del agente. Determina qué acción semántica ejecutar a
 * partir de las percepciones recibidas del entorno.
 *
 * @param <S> tipo de percepciones del entorno (debe implementar {@link Sensor})
 * @param <A> tipo de acción a ejecutar (debe implementar {@link Accion})
 */
public interface Politica<S extends Sensor, A extends Accion> {

    /**
     * Aplica la función de decisión sobre las percepciones actuales y devuelve la acción a
     * ejecutar.
     *
     * @param sensores percepciones actuales leídas del entorno
     * @return acción de dominio seleccionada
     */
    A aplicar(S sensores);
}
