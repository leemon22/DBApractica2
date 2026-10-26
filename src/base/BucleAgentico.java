package base;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

// Clásico bucle de agente que lee sensores, aplica la política y ejecuta acciones en el entorno
// hasta que el runStatus sea distinto de ACTIVE.
// S: Sensores leídos del entorno
// A: Acción a ejecutar en el entorno
public final class BucleAgentico<S, A> extends SimpleBehaviour {

    private final Entorno<S, A> entorno;
    private final Politica<S, A> politica;
    private boolean terminado = false;

    public BucleAgentico(Agent a, Entorno<S, A> entorno, Politica<S, A> politica) {
        super(a);
        this.entorno = entorno;
        this.politica = politica;
    }

    // Clásica iteración del agente: leer sensores, aplicar política, ejecutar acción y comprobar
    // estado.
    @Override
    public void action() {
        try {
            EstadoControl estado = entorno.getEstadoActual();

            // Comprobamos si el runStatus sigue siendo ACTIVE antes de hacer nada más.
            if (!estado.isActivo()) {
                terminado = true;
                return;
            }

            // 1. Percibir
            S sensores = entorno.leerSensores();
            // 2. Decidir
            A accion = politica.aplicar(sensores);
            // 3. Actuar
            EstadoControl nuevoEstado = entorno.ejecutarAccion(accion);

            // Comprobamos si el runStatus sigue siendo ACTIVE después de ejecutar la acción.
            if (!nuevoEstado.isActivo()) {
                terminado = true;
            }
        } catch (Exception e) {
            // Si hay un error en cualquier paso, lo registramos y marcamos el comportamiento como
            // terminado.
            e.printStackTrace();
            terminado = true;
        }
    }

    // Condición de parada: el runStatus deja de ser ACTIVE o se produce un error.
    @Override
    public boolean done() {
        // Solicitamos al agente que se elimine a sí mismo si hemos terminado.
        if (terminado) myAgent.doDelete();

        return terminado;
    }
}
