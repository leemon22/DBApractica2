package termostato;

import base.EstadoControl;
import base.InfoReto;
import base.Visualizador;

/**
 * Visualizador por consola para el bloque del termostato.
 * Centraliza toda la presentación formateada de percepciones, acciones y eventos del entorno.
 */
public class VisualizadorTermostato implements Visualizador<SensoresTermostato, AccionTermostato> {

    private InfoReto retoActual;

    @Override
    public void mostrarRetoEncontrado(InfoReto reto) {
        this.retoActual = reto;
        System.out.println("Reto termostato encontrado: " + reto.id());
    }

    @Override
    public void mostrarInicio(String runId, EstadoControl estado) {
        System.out.println("Run iniciado: " + runId);
        System.out.println("Estado inicial: " + estado.runStatus());
    }

    @Override
    public void mostrarPercepcion(SensoresTermostato sensores) {
        System.out.println("\n--- PERCEPCIÓN DEL AGENTE ---");
        if (retoActual != null) {
            System.out.println("Reto actual: ID " + retoActual.id());
        }
        System.out.println("Temperatura actual: " + sensores.temperaturaActual() + " °C");
        System.out.println("Límites permitidos: [" + sensores.limiteInferior() + ", " + sensores.limiteSuperior() + "] °C");
        System.out.println("Temperatura objetivo: " + sensores.temperaturaObjetivo() + " °C");
    }

    @Override
    public void mostrarAccion(AccionTermostato accion, String resultado, EstadoControl nuevoEstado) {
        System.out.println(
                "Acción: " + accion
                + " | Resultado: " + resultado
                + " | Estado: " + nuevoEstado.runStatus()
        );
    }

    @Override
    public void mostrarCierre(String runId) {
        System.out.println("Cerrando run activo en DBA Lab: " + runId);
    }

    @Override
    public void mostrarFin(EstadoControl estadoFinal) {
        System.out.println("Ejecución finalizada" + (retoActual != null ? " (Reto ID: " + retoActual.id() + ")" : "")
                + ": " + estadoFinal.runStatus()
                + (estadoFinal.reasonCode() != null ? " | Motivo: " + estadoFinal.reasonCode() : ""));
    }
}
