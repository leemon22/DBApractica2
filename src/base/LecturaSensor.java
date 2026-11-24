package base;

import java.util.Map;
import java.util.Objects;

/**
 * Lectura de un sensor de DBA Lab, desacoplada de JSON. Los accesos obligatorios fallan con el
 * nombre del sensor y la propiedad si el dato falta o tiene un tipo incorrecto.
 *
 * @param sensorId identificador del sensor leído
 * @param valores mapa de propiedades clave-valor devueltas en el bloque "value"
 * @param control estado de control del run devuelto por la API tras la lectura
 */
public record LecturaSensor(String sensorId, Map<String, Object> valores, EstadoControl control) {

    public LecturaSensor {
        Objects.requireNonNull(sensorId, "El sensorId no puede ser nulo");
        valores = valores != null ? Map.copyOf(valores) : Map.of();
        Objects.requireNonNull(control, "El estado de control no puede ser nulo");
    }

    /** Acepta cualquier Number, igual que la conversión numérica de la API. */
    public double getDouble(String clave) {
        if (valores.get(clave) instanceof Number numero) return numero.doubleValue();
        throw propiedadInvalida(clave, "no es numérica o no existe");
    }

    public int getInt(String clave) {
        if (valores.get(clave) instanceof Number numero) return numero.intValue();
        throw propiedadInvalida(clave, "no es un entero o no existe");
    }

    public boolean getBoolean(String clave) {
        if (valores.get(clave) instanceof Boolean valor) return valor;
        throw propiedadInvalida(clave, "no es booleana o no existe");
    }

    public String getString(String clave) {
        Object valor = valores.get(clave);
        if (valor != null) return valor.toString();
        throw propiedadInvalida(clave, "no existe");
    }

    private IllegalArgumentException propiedadInvalida(String clave, String motivo) {
        return new IllegalArgumentException(
                "La propiedad '" + clave + "' " + motivo + " en sensor " + sensorId);
    }
}
