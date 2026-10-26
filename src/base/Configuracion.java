package base;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

// Clase para manejar el archivo de configuración de la aplicación.
// Lee la URL de la API y el token de autenticación desde un archivo de propiedades.
public class Configuracion {

    private final String urlLaboratorio;
    private final String token;

    public Configuracion(String rutaArchivo) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            props.load(fis);
            this.urlLaboratorio = props.getProperty("DBA_LAB_URL");
            this.token = props.getProperty("DBA_TOKEN");

            if (this.urlLaboratorio == null || this.token == null) {
                throw new IllegalStateException("Faltan variables en el archivo de configuración.");
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error al leer el archivo de configuración: " + rutaArchivo, e);
        }
    }

    public String getUrlLaboratorio() {
        return urlLaboratorio;
    }

    public String getToken() {
        return token;
    }
}
