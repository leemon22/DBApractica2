package base;

import java.util.Objects;
import java.util.Optional;

/**
 * Encapsula la respuesta obtenida tras enviar una acción física a DBA Lab.
 *
 * @param actionStatus resultado de la acción ("APPLIED" o "REJECTED")
 * @param reasonCode   código de motivo si fue rechazada o finalizó el run (puede ser nulo)
 * @param control      nuevo estado de control del run
 */
public record RespuestaAccion(
        String actionStatus,
        String reasonCode,
        EstadoControl control
) {

    public RespuestaAccion {
        Objects.requireNonNull(actionStatus, "El actionStatus no puede ser nulo");
        Objects.requireNonNull(control, "El control no puede ser nulo");
    }

    /**
     * Comprueba si la acción fue aceptada y aplicada físicamente por el entorno.
     *
     * @return true si actionStatus es "APPLIED"
     */
    public boolean isAplicada() {
        return "APPLIED".equalsIgnoreCase(actionStatus);
    }

    /**
     * Devuelve el motivo en un Optional.
     *
     * @return Optional con el motivo o vacío si no hay
     */
    public Optional<String> motivo() {
        return Optional.ofNullable(reasonCode);
    }
}
