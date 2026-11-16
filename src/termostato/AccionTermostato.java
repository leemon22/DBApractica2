package termostato;

import base.Accion;

/**
 * Acciones semánticas del agente termostato.
 * Representa la decisión de encender o apagar la refrigeración,
 * completamente desacoplada de los códigos y cadenas de red de la API.
 */
public enum AccionTermostato implements Accion {
    ON,
    OFF
}
