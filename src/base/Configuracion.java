package base;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Gestor de la configuración de la aplicación.
 * Carga parámetros de conexión (URL, token, reto opcional) desde un archivo de propiedades.
 */
public class Configuracion {

    private final Properties propiedades;
    private final String urlApi;
    private final String token;

    /**
     * Carga el archivo de propiedades indicado y valida los parámetros obligatorios.
     *
     * @param rutaArchivo ruta del archivo de configuración (p. ej. "config.properties")
     * @throws RuntimeException si el archivo no se puede leer o faltan variables obligatorias
     */
    public Configuracion(String rutaArchivo) {
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo no puede ser nula");
        this.propiedades = new Properties();

        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            propiedades.load(fis);

            String url = propiedades.getProperty("DBA_API_URL");
            String tok = propiedades.getProperty("DBA_TOKEN");

            if (url == null || url.isBlank() || tok == null || tok.isBlank()) {
                throw new IllegalStateException("Faltan variables obligatorias (DBA_API_URL o DBA_TOKEN) en: " + rutaArchivo);
            }

            this.urlApi = url.trim();
            this.token = tok.trim();

        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo de configuración: " + rutaArchivo, e);
        }
    }

    /**
     * Devuelve la URL base de la API del laboratorio.
     */
    public String getUrlLaboratorio() {
        return urlApi;
    }

    /**
     * Devuelve el token de autenticación Bearer personal.
     */
    public String getToken() {
        return token;
    }

    /**
     * Consulta una propiedad opcional del archivo de configuración.
     *
     * @param clave nombre de la clave
     * @return Optional con el valor recortado o vacío si no está definida
     */
    public Optional<String> getPropiedad(String clave) {
        return Optional.ofNullable(propiedades.getProperty(clave))
                .map(String::trim)
                .filter(valor -> !valor.isBlank());
    }

    /**
     * Consulta el identificador de reto preferido si está definido en "DBA_CHALLENGE_ID".
     *
     * @return Optional con el challengeId numérico o vacío si no se especificó
     */
    public Optional<Long> getChallengeId() {
        return getPropiedad("DBA_CHALLENGE_ID").map(Long::parseLong);
    }
}
