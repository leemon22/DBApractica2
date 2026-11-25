package base;

/**
 * Contrato para desacoplar la emisión de eventos y presentación del entorno de su lógica operativa.
 * Permite alternar fácilmente entre presentaciones por consola o interfaces gráficas sin modificar
 * el entorno.
 *
 * @param <S> tipo de sensores que percibe el agente
 * @param <A> tipo de acciones que ejecuta el agente
 */
public interface Visualizador<S extends Sensor, A extends Accion> {

    /**
     * Notifica el descubrimiento o selección de un reto en DBA Lab.
     *
     * @param reto información y capacidades del reto seleccionado
     */
    void mostrarRetoEncontrado(InfoReto reto);

    /**
     * Notifica el inicio de un run en el entorno.
     *
     * @param runId identificador de la ejecución
     * @param estado estado inicial del run
     */
    void mostrarInicio(String runId, EstadoControl estado);

    /**
     * Notifica la lectura de nuevas percepciones del agente.
     *
     * @param sensores percepciones leídas
     */
    void mostrarPercepcion(S sensores);

    /**
     * Notifica el resultado de ejecutar una acción en el entorno.
     *
     * @param accion acción semántica ejecutada
     * @param resultado resultado de la acción (p. ej. "APPLIED", "REJECTED")
     * @param nuevoEstado nuevo estado de control tras la acción
     */
    void mostrarAccion(A accion, String resultado, EstadoControl nuevoEstado);

    /**
     * Notifica la solicitud de cierre de un run activo.
     *
     * @param runId identificador del run a cerrar
     */
    void mostrarCierre(String runId);

    /**
     * Notifica la finalización de la ejecución.
     *
     * @param estadoFinal estado de control de cierre oficial
     */
    void mostrarFin(EstadoControl estadoFinal);

    /**
     * Muestra un mensaje informativo o de depuración genérico.
     *
     * @param mensaje texto a mostrar
     */
    default void mostrarMensaje(String mensaje) {}

    /**
     * Solicita al usuario la selección de un reto cuando existen múltiples alternativas
     * disponibles. Las implementaciones interactivas (por consola o interfaz gráfica) pueden
     * solicitar la elección al usuario; la implementación por defecto selecciona el primer reto de
     * la lista.
     *
     * @param retos lista de retos compatibles disponibles
     * @return reto seleccionado
     */
    default InfoReto seleccionarReto(java.util.List<InfoReto> retos) {
        if (retos == null || retos.isEmpty()) {
            throw new IllegalArgumentException("La lista de retos no puede ser nula ni vacía");
        }
        return retos.get(0);
    }
}
