package base;

import java.util.List;

/**
 * Encapsula la información descriptiva y capacidades de un reto anunciado por DBA Lab.
 * Permite al entorno comprobar compatibilidad de sensores y acciones antes de iniciar un run.
 *
 * @param id              identificador numérico del reto
 * @param environmentType tipo de entorno (p. ej. "THERMOSTAT", "GRID_2D")
 * @param sensorCodes     lista de códigos oficiales de sensores disponibles
 * @param actionCodes     lista de códigos oficiales de acciones disponibles
 */
public record InfoReto(
        long id,
        String environmentType,
        List<String> sensorCodes,
        List<String> actionCodes
) {

    public InfoReto {
        sensorCodes = sensorCodes != null ? List.copyOf(sensorCodes) : List.of();
        actionCodes = actionCodes != null ? List.copyOf(actionCodes) : List.of();
    }

    /**
     * Comprueba si el reto anuncia un sensor específico.
     *
     * @param sensorId código oficial del sensor
     * @return true si está disponible
     */
    public boolean contieneSensor(String sensorId) {
        return sensorCodes.contains(sensorId);
    }

    /**
     * Comprueba si el reto anuncia una acción específica.
     *
     * @param actionCode código oficial de la acción
     * @return true si está disponible
     */
    public boolean contieneAccion(String actionCode) {
        return actionCodes.contains(actionCode);
    }
}
