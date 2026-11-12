package base;

/**
 * Interfaz que representa una acción ejecutable en el entorno de DBA Lab.
 */
public interface Accion {

    /**
     * Devuelve el código oficial de la acción según la API de DBA Lab.
     */
    String getCodigoAccion();
}
