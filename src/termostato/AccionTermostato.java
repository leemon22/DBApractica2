package termostato;

/**
 * Acciones del termostato con su código exacto en la API de DBA Lab.
 *
 * @author martinhernandezruiz
 */
public enum AccionTermostato {
    
    
    ON("thermostat.refrigeration.on"),
    OFF("thermostat.refrigeration.off");

    private final String codigo;

    AccionTermostato(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
