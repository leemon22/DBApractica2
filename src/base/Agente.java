package base;

import jade.core.Agent;

// Clase base abstracta para agentes JADE situados en DBA Lab.
// Utiliza tipos fuertemente tipados basados en los contratos Sensor y Accion.
public abstract class Agente<S extends Sensor, A extends Accion> extends Agent {

    protected Entorno<S, A> entorno;
    protected BucleAgentico<S, A> bucle;

    // Método abstracto donde las subclases instancian el entorno y el bucle agéntico.
    protected abstract void iniciar() throws Exception;

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciando...");
        try {
            iniciar();
            entorno.iniciarRun();
            addBehaviour(bucle);
        } catch (Exception e) {
            System.err.println("Error crítico durante la inicialización del agente: " + e.getMessage());
            e.printStackTrace();
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Agente " + getLocalName() + " terminando ejecución.");
    }
}
