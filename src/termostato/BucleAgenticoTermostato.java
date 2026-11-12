package termostato;

import base.BucleAgentico;
import base.Entorno;
import base.EstadoControl;
import base.Politica;
import jade.core.Agent;

/**
 * Bucle agéntico específico para el termostato.
 * Comprueba el estado del run antes y después de cada interacción.
 */
public class BucleAgenticoTermostato extends BucleAgentico<SensoresTermostato, AccionTermostato> {

    public BucleAgenticoTermostato(
            Agent agente,
            Entorno<SensoresTermostato, AccionTermostato> entorno,
            Politica<SensoresTermostato, AccionTermostato> politica) {
        super(agente);
        this.entorno = entorno;
        this.politica = politica;
    }

    @Override
    protected void antes() {
        EstadoControl estado = entorno.getEstadoActual();
        if (estado != null && !estado.isActivo()) {
            terminado = true;
        }
    }

    @Override
    protected void despues() {
        EstadoControl estado = entorno.getEstadoActual();
        if (estado != null && !estado.isActivo()) {
            terminado = true;
        }
    }
}
