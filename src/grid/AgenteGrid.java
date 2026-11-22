package grid;

import base.Agente;
import base.ComponentesAgente;

/**
 * Agente JADE específico para la navegación autónoma en el entorno Grid (2D).
 */
public class AgenteGrid extends Agente<SensoresGrid, Direccion> {

    @Override
    protected ComponentesAgente<SensoresGrid, Direccion> iniciar() {
        EntornoGrid entorno = new EntornoGrid();
        PoliticaGridV2 politica = new PoliticaGridV2();
        BucleAgenticoGrid bucle = new BucleAgenticoGrid(this, entorno, politica);

        return new ComponentesAgente<>(entorno, bucle);
    }
}
