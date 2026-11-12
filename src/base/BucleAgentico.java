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

    // --- Métodos Abstractos del Ciclo de Vida ---

    /**
     * Tareas previas al ciclo de percepción-decisión-acción (p. ej. verificar si el run sigue activo).
     *
     * @throws Exception si ocurre un error durante las comprobaciones previas
     */
    protected abstract void antes() throws Exception;

    /**
     * Tareas posteriores a la acción ejecutada (p. ej. verificar resultado o registrar métricas).
     *
     * @throws Exception si ocurre un error durante las tareas posteriores
     */
    protected abstract void despues() throws Exception;

    // --- Ciclo Principal JADE ---

    @Override
    public void action() {
        try {
            antes();
            if (terminado) {
                return;
            }

            // 1. Percibir
            S sensores = entorno.leerSensores();
            // 2. Decidir
            A accion = politica.aplicar(sensores);
            // 3. Actuar
            EstadoControl nuevoEstado = entorno.ejecutarAccion(accion);

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
