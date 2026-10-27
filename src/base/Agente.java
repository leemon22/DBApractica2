package base;

import jade.core.Agent;

// Implementación clásica de un agente usando JADE
// Llamar siempre con su Entorno como primer argumento y su Política como segundo argumento.
public final class Agente extends Agent {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciando...");

        // Obtenemos los argumentos pasados al agente y verificamos que sean correctos
        Object[] args = getArguments();
        if (args == null || args.length < 2) {
            System.err.println("Error: Faltan argumentos. Agente(Entorno, Politica)");
            doDelete();
            return;
        }

        // Hacemos un cast seguro de los argumentos a Entorno y Politica
        Entorno<?, ?> entorno = (Entorno<?, ?>) args[0];
        Politica<?, ?> politica = (Politica<?, ?>) args[1];

        try {
            // Leemos del archivo de configuración e iniciamos el run en el entorno
            Configuracion config = new Configuracion("config.properties");
            entorno.iniciarRun(config);

            // Añadimos el comportamiento del bucle agentico al agente
            addBehaviour(new BucleAgentico(this, entorno, politica));

        } catch (Exception e) {
            // Si hay error al iniciar el run, lo registramos y eliminamos el agente
            System.err.println(
                    "Error crítico durante la inicialización del entorno: " + e.getMessage());
            e.printStackTrace();
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        // Mostramos un mensaje cuando el agente termina su ejecución
        System.out.println("Agente " + getLocalName() + " terminando ejecución.");
        // Cerrar la plataforma JADE al terminar el agente.
    }
}
