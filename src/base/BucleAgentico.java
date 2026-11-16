package base;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import java.util.Objects;

/**
 * Bucle agéntico abstracto que ejecuta el ciclo de percepción-decisión-acción en el entorno.
 * Proporciona el esqueleto canónico del ciclo y los métodos gancho {@code antes()} y {@code despues()}.
 *
 * @param <S> tipo de percepciones que extiende {@link Sensor}
 * @param <A> tipo de acciones que extiende {@link Accion}
 */
public abstract class BucleAgentico<S extends Sensor, A extends Accion> extends SimpleBehaviour {

    protected final Entorno<S, A> entorno;
    protected final Politica<S, A> politica;
    protected boolean terminado = false;

    /**
     * Construye el bucle agéntico asociando el agente JADE, el entorno y la política de decisión.
     *
     * @param agente   agente JADE que ejecuta este comportamiento
     * @param entorno  entorno con el que interactúa el agente
     * @param politica política que decide las acciones a partir de las percepciones
     */
    public BucleAgentico(Agent agente, Entorno<S, A> entorno, Politica<S, A> politica) {
        super(Objects.requireNonNull(agente, "El agente no puede ser nulo"));
        this.entorno = Objects.requireNonNull(entorno, "El entorno no puede ser nulo");
        this.politica = Objects.requireNonNull(politica, "La política no puede ser nula");
    }

    /**
     * Tareas o comprobaciones previas al ciclo de percepción-decisión-acción.
     * Si se detecta una condición de parada, debe marcar {@code terminado = true}.
     *
     * @throws Exception si ocurre un error durante las tareas previas
     */
    protected abstract void antes() throws Exception;

    /**
     * Tareas o comprobaciones posteriores a la acción física ejecutada.
     * Si se detecta una condición de parada, debe marcar {@code terminado = true}.
     *
     * @throws Exception si ocurre un error durante las tareas posteriores
     */
    protected abstract void despues() throws Exception;

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
            entorno.ejecutarAccion(accion);

            despues();

        } catch (Exception e) {
            manejarError(e);
            terminado = true;
        }
    }

    /**
     * Gestiona excepciones producidas durante la ejecución del bucle ante fallos inesperados.
     */
    protected void manejarError(Exception e) {
        System.err.println("Error durante la ejecución del agente: " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public boolean done() {
        return terminado;
    }

    @Override
    public int onEnd() {
        // Notificamos el estado de finalización oficial al visualizador si está disponible
        entorno.getEstadoControl().ifPresent(estado -> entorno.getVisualizador().mostrarFin(estado));
        myAgent.doDelete();
        return 0;
    }
}
