/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package termostato;

/**
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
