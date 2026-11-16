package base;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Encapsula la lectura de un sensor en DBA Lab, desacoplada de la infraestructura JSON.
 *
 * @param sensorId identificador del sensor leído
 * @param valores  mapa de propiedades clave-valor devueltas en el bloque "value"
 * @param control  estado de control del run devuelto por la API tras la lectura
 */
public record LecturaSensor(
        String sensorId,
        Map<String, Object> valores,
        EstadoControl control
) {

    public LecturaSensor {
        Objects.requireNonNull(sensorId, "El sensorId no puede ser nulo");
        valores = valores != null ? Map.copyOf(valores) : Map.of();
        Objects.requireNonNull(control, "El estado de control no puede ser nulo");
    }

    /**
     * Obtiene un valor numérico como double obligatorio.
     *
     * @param clave nombre de la propiedad
     * @return valor double
     * @throws IllegalArgumentException si la propiedad no existe o no es numérica
     */
    public double getDouble(String clave) {
        return getDoubleOpcional(clave).orElseThrow(
                () -> new IllegalArgumentException("La propiedad '" + clave + "' no es numérica o no existe en sensor " + sensorId)
        );
    }

    /**
     * Obtiene un valor numérico como double opcional.
     *
     * @param clave nombre de la propiedad
     * @return Optional con el valor o vacío si no existe o no es numérico
     */
    public Optional<Double> getDoubleOpcional(String clave) {
        Object valor = valores.get(clave);
        if (valor instanceof Number numero) {
            return Optional.of(numero.doubleValue());
        }
        return Optional.empty();
    }

    /**
     * Obtiene un valor numérico como int obligatorio.
     *
     * @param clave nombre de la propiedad
     * @return valor int
     * @throws IllegalArgumentException si la propiedad no existe o no es numérica
     */
    public int getInt(String clave) {
        return getIntOpcional(clave).orElseThrow(
                () -> new IllegalArgumentException("La propiedad '" + clave + "' no es un entero o no existe en sensor " + sensorId)
        );
    }

    /**
     * Obtiene un valor numérico como int opcional.
     *
     * @param clave nombre de la propiedad
     * @return Optional con el valor o vacío si no existe o no es numérico
     */
    public Optional<Integer> getIntOpcional(String clave) {
        Object valor = valores.get(clave);
        if (valor instanceof Number numero) {
            return Optional.of(numero.intValue());
        }
        return Optional.empty();
    }

    /**
     * Obtiene un valor booleano obligatorio.
     *
     * @param clave nombre de la propiedad
     * @return valor booleano
     * @throws IllegalArgumentException si la propiedad no existe o no es booleana
     */
    public boolean getBoolean(String clave) {
        return getBooleanOpcional(clave).orElseThrow(
                () -> new IllegalArgumentException("La propiedad '" + clave + "' no es booleana o no existe en sensor " + sensorId)
        );
    }

    /**
     * Obtiene un valor booleano opcional.
     *
     * @param clave nombre de la propiedad
     * @return Optional con el booleano o vacío si no existe o no es booleano
     */
    public Optional<Boolean> getBooleanOpcional(String clave) {
        Object valor = valores.get(clave);
        if (valor instanceof Boolean booleano) {
            return Optional.of(booleano);
        }
        return Optional.empty();
    }

    /**
     * Obtiene una propiedad como texto obligatorio.
     *
     * @param clave nombre de la propiedad
     * @return texto de la propiedad
     * @throws IllegalArgumentException si la propiedad no existe
     */
    public String getString(String clave) {
        return getStringOpcional(clave).orElseThrow(
                () -> new IllegalArgumentException("La propiedad '" + clave + "' no existe en sensor " + sensorId)
        );
    }

    /**
     * Obtiene una propiedad como texto opcional dentro de un {@link Optional}.
     *
     * @param clave nombre de la propiedad
     * @return Optional con la cadena o vacío si no existe
     */
    public Optional<String> getStringOpcional(String clave) {
        Object valor = valores.get(clave);
        return valor != null ? Optional.of(valor.toString()) : Optional.empty();
    }
}
