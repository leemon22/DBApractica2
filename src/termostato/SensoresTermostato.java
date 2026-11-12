package termostato;

import base.Sensor;
import com.google.gson.JsonObject;

/**
 * Percepciones del termostato: temperatura actual, límites permitidos y temperatura objetivo.
 */
public record SensoresTermostato(
        double temperaturaActual,
        double limiteInferior,
        double limiteSuperior,
        double temperaturaObjetivo) implements Sensor {

    /**
     * Construye las percepciones a partir de los objetos JSON devueltos por DBA Lab.
     *
     * @param temperatura JSON de thermostat.temperature
     * @param limites     JSON de thermostat.limits
     * @param objetivo    JSON de thermostat.target
     */
    public SensoresTermostato(JsonObject temperatura, JsonObject limites, JsonObject objetivo) {
        this(
                temperatura.get("celsius").getAsDouble(),
                limites.get("lower").getAsDouble(),
                limites.get("upper").getAsDouble(),
                objetivo.get("celsius").getAsDouble()
        );
    }
}
