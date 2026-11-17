package base;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Gestor de la configuración de la aplicación.
 * Carga parámetros de conexión (URL, token, reto opcional) desde un archivo de propiedades
 * o desde variables de entorno del sistema como mecanismo de respaldo.
 */
public class Configuracion {

    public static final String ARCHIVO_PREDETERMINADO = "config.properties";

    public static final String CLAVE_URL = "DBA_API_URL";
    public static final String CLAVE_TOKEN = "DBA_TOKEN";
    public static final String CLAVE_CHALLENGE_ID = "DBA_CHALLENGE_ID";

    private final Properties propiedades;
    private final String urlApi;
    private final String token;
    private final Optional<Long> challengeId;

    /**
     * Constructor por defecto: carga la configuración desde "config.properties".
     */
    public Configuracion() {
        this(ARCHIVO_PREDETERMINADO);
    }

    /**
     * Carga el archivo de propiedades indicado y valida los parámetros obligatorios.
     *
     * @param rutaArchivo ruta del archivo de configuración (p. ej. "config.properties")
     * @throws IllegalStateException si el archivo no se puede leer o faltan propiedades obligatorias
     */
    public Configuracion(String rutaArchivo) {
        Objects.requireNonNull(rutaArchivo, "La ruta del archivo de configuración no puede ser nula");
        this.propiedades = new Properties();

        Path path = Path.of(rutaArchivo);
        if (Files.exists(path)) {
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                propiedades.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Error al leer el archivo de configuración: " + rutaArchivo, e);
            }
        }

        // Recuperar URL obligatoria (archivo o variable de entorno)
        this.urlApi = resolverPropiedad(CLAVE_URL)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la propiedad obligatoria '" + CLAVE_URL + "' en: " + rutaArchivo));

        // Recuperar Token obligatorio (archivo o variable de entorno)
        this.token = resolverPropiedad(CLAVE_TOKEN)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la propiedad obligatoria '" + CLAVE_TOKEN + "' en: " + rutaArchivo));

        // Recuperar Challenge ID opcional
        this.challengeId = resolverPropiedad(CLAVE_CHALLENGE_ID).flatMap(Configuracion::parsearLongSeguro);
    }

    /**
     * Resuelve una clave buscando primero en el archivo de propiedades cargado
     * y, como respaldo secundario, en las variables de entorno del sistema.
     */
    private Optional<String> resolverPropiedad(String clave) {
        String valorArchivo = propiedades.getProperty(clave);
        if (valorArchivo != null && !valorArchivo.isBlank()) {
            return Optional.of(valorArchivo.trim());
        }

        String valorEnv = System.getenv(clave);
        if (valorEnv != null && !valorEnv.isBlank()) {
            return Optional.of(valorEnv.trim());
        }

        return Optional.empty();
    }

    private static Optional<Long> parsearLongSeguro(String valor) {
        try {
            return Optional.of(Long.parseLong(valor.trim()));
        } catch (NumberFormatException e) {
            System.err.println("Aviso: Formato numérico no válido para " + CLAVE_CHALLENGE_ID + ": '" + valor + "'");
            return Optional.empty();
        }
    }

    /**
     * Devuelve la URL base de la API de DBA Lab.
     */
    public String getUrlApi() {
        return urlApi;
    }

    /**
     * Alias de {@link #getUrlApi()} para compatibilidad con código existente.
     */
    public String getUrlLaboratorio() {
        return getUrlApi();
    }

    /**
     * Devuelve el token personal de autenticación Bearer.
     */
    public String getToken() {
        return token;
    }

    /**
     * Consulta el identificador de reto preferido si fue configurado.
     */
    public Optional<Long> getChallengeId() {
        return challengeId;
    }

    /**
     * Consulta una propiedad arbitraria del archivo de configuración o entorno.
     *
     * @param clave nombre de la propiedad
     * @return Optional con el valor o vacío si no está definida
     */
    public Optional<String> getPropiedad(String clave) {
        return resolverPropiedad(clave);
    }
}
