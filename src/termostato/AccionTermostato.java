package termostato;

import base.Accion;

/**
 * Acciones del termostato con su código oficial en la API de DBA Lab.
 */
public enum AccionTermostato implements Accion {

    ON("thermostat.refrigeration.on"),
    OFF("thermostat.refrigeration.off");

    private final String codigo;

    AccionTermostato(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public String getCodigoAccion() {
        return codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
