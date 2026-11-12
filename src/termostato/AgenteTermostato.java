package termostato;

import base.Agente;

/**
 * Agente JADE específico para el control del termostato.
 */
public class AgenteTermostato extends Agente<SensoresTermostato, AccionTermostato> {

    @Override
    protected void iniciar() {
        this.entorno = new EntornoTermostato();
        PoliticaTermostato politica = new PoliticaTermostato();
        this.bucle = new BucleAgenticoTermostato(this, this.entorno, politica);
    }
}
