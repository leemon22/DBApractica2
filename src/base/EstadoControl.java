package base;

import java.util.Optional;

/**
 * Encapsula los datos oficiales del bloque "control" que devuelve la API de DBA Lab. Representa el
 * estado del ciclo de vida de la ejecución de forma desacoplada de la infraestructura HTTP/JSON.
 *
 * @param runId identificador numérico de la ejecución en curso
 * @param runStatus estado del ciclo de vida (ACTIVE, SUCCEEDED, FAILED, etc.)
 * @param reasonCode motivo oficial de finalización o rechazo (puede ser nulo)
 */
public record EstadoControl(String runId, RunStatus runStatus, String reasonCode) {

    /**
     * Comprueba si la ejecución sigue en curso y puede recibir más acciones.
     *
     * @return true si el estado es ACTIVE, false si ha finalizado
     */
    public boolean isActivo() {
        return this.runStatus == RunStatus.ACTIVE;
    }

    /**
     * Devuelve el motivo de cierre o rechazo envuelto en un {@link Optional}.
     *
     * @return Optional con el reasonCode o vacío si no se especificó motivo
     */
    public Optional<String> motivo() {
        return Optional.ofNullable(reasonCode);
    }

    /**
     * Traduce el texto del estado devuelto por el servidor al enumerado {@link RunStatus}. Si el
     * servidor devuelve un valor desconocido o nulo, se asigna UNKNOWN para detener el bucle con
     * seguridad.
     *
     * @param estado cadena con el nombre del estado recibido de la API
     * @return RunStatus correspondiente o UNKNOWN
     */
    public static RunStatus parsearEstado(String estado) {
        if (estado == null) {
            return RunStatus.UNKNOWN;
        }
        try {
            return RunStatus.valueOf(estado);
        } catch (IllegalArgumentException e) {
            System.err.println("Aviso: runStatus desconocido recibido de DBA Lab: " + estado);
            return RunStatus.UNKNOWN;
        }
    }

    /** Posibles estados oficiales de una ejecución según DBA Lab. */
    public enum RunStatus {
        ACTIVE,
        SUCCEEDED,
        FAILED,
        EXPIRED,
        INTERRUPTED,
        UNKNOWN
    }
}
