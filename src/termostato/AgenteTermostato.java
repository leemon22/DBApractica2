package termostato;

import base.Agente;
import base.ComponentesAgente;

/**
 * Agente JADE específico para el control del termostato.
 */
public class AgenteTermostato extends Agente<SensoresTermostato, AccionTermostato> {

    @Override
    protected ComponentesAgente<SensoresTermostato, AccionTermostato> iniciar() {
        EntornoTermostato entorno = new EntornoTermostato();
        PoliticaTermostato politica = new PoliticaTermostato();
        BucleAgenticoTermostato bucle = new BucleAgenticoTermostato(this, entorno, politica);

        return new ComponentesAgente<>(entorno, bucle);
    }
}
