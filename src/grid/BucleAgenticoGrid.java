package grid;

import base.BucleAgentico;
import base.Entorno;
import base.Politica;
import jade.core.Agent;

/**
 * Bucle agéntico específico para la navegación en Grid.
 * Reutiliza el ciclo canónico de BucleAgentico y gestiona la condición de parada
 * cuando el entorno ya no está activo o se ha alcanzado la meta.
 */
public class BucleAgenticoGrid extends BucleAgentico<SensoresGrid, Direccion> {

    public BucleAgenticoGrid(
            Agent agente,
            Entorno<SensoresGrid, Direccion> entorno,
            Politica<SensoresGrid, Direccion> politica) {
        super(agente, entorno, politica);
    }

    @Override
    protected void antes() {
        if (!entorno.isActivo()) {
            terminado = true;
            return;
        }

        // Salvaguarda: si en la última lectura ya se alcanzó el objetivo, detener antes de decidir
        if (entorno instanceof EntornoGrid eg && eg.isEnObjetivo()) {
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
