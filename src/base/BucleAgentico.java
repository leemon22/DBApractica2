package base;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

/**
 * Bucle agéntico abstracto que ejecuta el ciclo de percepción-decisión-acción en el entorno.
 * Proporciona el esqueleto del ciclo y los métodos abstractos antes() y despues(), dejando
 * a las subclases la gestión de sus dependencias (entorno y política) en sus propios constructores.
 *
 * @param <S> tipo de percepciones que extiende Sensor
 * @param <A> tipo de acciones que extiende Accion
 */
public abstract class BucleAgentico<S extends Sensor, A extends Accion> extends SimpleBehaviour {

    protected Entorno<S, A> entorno;
    protected Politica<S, A> politica;
    protected boolean terminado = false;

    // --- Constructores ---

    /**
     * Constructor por defecto para subclases que no requieren pasar el agente inmediatamente.
     */
    protected BucleAgentico() {
        super();
    }

    /**
     * Constructor protegido que asocia el agente JADE al comportamiento.
     *
     * @param agente agente JADE que ejecuta este bucle
     */
    protected BucleAgentico(Agent agente) {
        super(agente);
    }

    // --- Métodos Gancho del Ciclo de Vida ---

    /**
     * Gancho opcional para tareas previas al ciclo de percepción-decisión-acción.
     * Por defecto no realiza ninguna acción; las subclases pueden sobreescribirlo si lo necesitan.
     *
     * @throws Exception si ocurre un error durante las tareas previas
     */
    protected void antes() throws Exception {
    }

    /**
     * Gancho opcional para tareas posteriores a la acción ejecutada.
     * Por defecto no realiza ninguna acción; las subclases pueden sobreescribirlo si lo necesitan.
     *
     * @throws Exception si ocurre un error durante las tareas posteriores
     */
    protected void despues() throws Exception {
    }

    // --- Ciclo Principal JADE ---

    @Override
    public void action() {
        try {
            // Comprobamos si el run continúa activo antes de percibir
            if (entorno.getEstadoActual() != null && !entorno.getEstadoActual().isActivo()) {
                terminado = true;
                return;
            }

            antes();
            if (terminado) {
                return;
            }

            // 1. Percibir
            S sensores = entorno.leerSensores();
            if (!entorno.getEstadoActual().isActivo()) {
                terminado = true;
                return;
            }

            // 2. Decidir
            A accion = politica.aplicar(sensores);

            // 3. Actuar
            EstadoControl nuevoEstado = entorno.ejecutarAccion(accion);
            if (!nuevoEstado.isActivo()) {
                terminado = true;
            }

            despues();

        } catch (Exception e) {
            manejarError(e);
            terminado = true;
        }
    }

    /**
     * Gestiona las excepciones producidas durante la ejecución del bucle, distinguiendo
     * entre la finalización oficial del run por DBA Lab y errores inesperados.
     */
    protected void manejarError(Exception e) {
        EstadoControl estado = (entorno != null) ? entorno.getEstadoActual() : null;

        if (estado != null && !estado.isActivo()
                && estado.runStatus() != EstadoControl.RunStatus.UNKNOWN) {

            System.out.println("Ejecución finalizada: " + estado.runStatus()
                    + " | Motivo: " + estado.reasonCode());

        } else {
            System.err.println("Error durante la ejecución del agente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean done() {
        return terminado;
    }

    @Override
    public int onEnd() {
        myAgent.doDelete();
        return 0;
    }
}
