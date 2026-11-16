package termostato;

import base.BucleAgentico;
import base.Entorno;
import base.Politica;
import jade.core.Agent;

/**
 * Bucle agéntico específico para el termostato.
 * Reutiliza el ciclo canónico de BucleAgentico e implementa antes() y despues()
 * para comprobar el estado de actividad del entorno.
 */
public class BucleAgenticoTermostato extends BucleAgentico<SensoresTermostato, AccionTermostato> {

    public BucleAgenticoTermostato(
            Agent agente,
            Entorno<SensoresTermostato, AccionTermostato> entorno,
            Politica<SensoresTermostato, AccionTermostato> politica) {
        super(agente, entorno, politica);
    }

    @Override
    protected void antes() {
        if (!entorno.isActivo()) {
            terminado = true;
        }
    }

    @Override
    protected void despues() {
        if (!entorno.isActivo()) {
            terminado = true;
        }
    }
}
