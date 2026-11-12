package termostato;

import base.BucleAgentico;
import base.Entorno;
import base.Politica;
import jade.core.Agent;

/**
 * Bucle agéntico específico para el termostato.
 * Reutiliza el ciclo de vida del bucle agéntico base.
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
}
