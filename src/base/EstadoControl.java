package base;

import com.google.gson.JsonObject;

/**
 * Encapsula los datos oficiales del bloque "control" que devuelve la API de DBA Lab.
 */
public record EstadoControl(String runId, RunStatus runStatus, String reasonCode) {

    /**
     * Construye un EstadoControl directamente a partir del objeto JSON "control" recibido de DBA Lab.
     *
     * @param control bloque JSON de control
     */
    public EstadoControl(JsonObject control) {
        this(
                control.get("runId").getAsString(),
                parsearEstado(control.get("runStatus").getAsString()),
                control.has("reasonCode") && !control.get("reasonCode").isJsonNull()
                        ? control.get("reasonCode").getAsString()
                        : null
        );
    }

    /**
     * Comprueba si la ejecución sigue en curso y puede recibir más acciones.
     *
     * @return true si el estado es ACTIVE, false si ha finalizado
     */
    public boolean isActivo() {
        return this.runStatus == RunStatus.ACTIVE;
    }

    /**
     * Traduce el runStatus de la API. Si el servidor devuelve un valor desconocido,
     * se asigna UNKNOWN para detener el bucle con seguridad.
     */
    public static RunStatus parsearEstado(String estado) {
        if (estado == null) {
            return RunStatus.UNKNOWN;
        }
        try {
            return RunStatus.valueOf(estado);
        } catch (IllegalArgumentException e) {
            System.err.println("runStatus desconocido recibido: " + estado);
            return RunStatus.UNKNOWN;
        }
    }

    /**
     * Posibles estados oficiales de una ejecución según DBA Lab.
     */
    public enum RunStatus {
        ACTIVE,
        SUCCEEDED,
        FAILED,
        EXPIRED,
        INTERRUPTED,
        UNKNOWN
    }
}
