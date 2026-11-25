package base;

import jade.core.Agent;

/**
 * Clase base abstracta para agentes JADE situados en DBA Lab. Coordina la inicialización de
 * componentes, el inicio del run y el cierre controlado.
 *
 * @param <S> tipo de percepciones (debe implementar {@link Sensor})
 * @param <A> tipo de acciones (debe implementar {@link Accion})
 */
public abstract class Agente<S extends Sensor, A extends Accion> extends Agent {

    protected Entorno<S, A> entorno;

    /**
     * Instancia y devuelve el entorno y el bucle agéntico específicos del agente.
     *
     * @return componentes inicializados del agente
     * @throws Exception si ocurre un error durante la creación
     */
    protected abstract ComponentesAgente<S, A> iniciar() throws Exception;

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciando...");
        try {
            ComponentesAgente<S, A> componentes = iniciar();
            this.entorno = componentes.entorno();

            this.entorno.iniciarRun();
            addBehaviour(componentes.bucle());

        } catch (Exception e) {
            System.err.println(
                    "Error crítico durante la inicialización del agente: " + e.getMessage());
            e.printStackTrace();
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Agente " + getLocalName() + " terminando ejecución.");
        if (entorno != null) {
            try {
                entorno.cerrarRun();
            } catch (Exception e) {
                System.err.println("Aviso al cerrar run en takeDown: " + e.getMessage());
            }
        }
    }
}
