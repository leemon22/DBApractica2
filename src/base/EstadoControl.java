package base;

// Record para encapsular los datos del bloque de control que devuelve la API.
public record EstadoControl(String runId, RunStatus runStatus, String reasonCode) {
    public boolean isActivo() {
        return this.runStatus == RunStatus.ACTIVE;
    }

    // Los posibles valores de runStatus según el guia-api-dba-lab.md de Prado
    public enum RunStatus {
        ACTIVE,
        SUCCEEDED,
        FAILED,
        EXPIRED,
        INTERRUPTED,
        UNKNOWN // Valor de seguridad por si hay actualizaciones en la API que no conocemos
    }
}
