package base;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

import java.util.Objects;

/**
 * Bucle agéntico que ejecuta el ciclo de percepción-decisión-acción en el entorno. Proporciona el
 * esqueleto del ciclo y los métodos {@code antes()} y {@code despues()}. Por defecto, ambos
 * comprueban la actividad del run; las especializaciones pueden añadir otras condiciones de parada.
 *
 * @param <S> tipo de percepciones que extiende {@link Sensor}
 * @param <A> tipo de acciones que extiende {@link Accion}
 */
public class BucleAgentico<S extends Sensor, A extends Accion> extends SimpleBehaviour {

    protected final Entorno<S, A> entorno;
    protected final Politica<S, A> politica;
    protected boolean terminado = false;

    /** Asocia el comportamiento JADE con su entorno y su política. */
    public BucleAgentico(Agent agente, Entorno<S, A> entorno, Politica<S, A> politica) {
        super(Objects.requireNonNull(agente, "El agente no puede ser nulo"));
        this.entorno = Objects.requireNonNull(entorno, "El entorno no puede ser nulo");
        this.politica = Objects.requireNonNull(politica, "La política no puede ser nula");
    }

    /** Comprueba la actividad antes de percibir. Las subclases pueden añadir otras paradas. */
    protected void antes() throws Exception {
        if (!entorno.isActivo()) terminado = true;
    }

    /** Detiene el ciclo si la acción ha terminado el run. */
    protected void despues() throws Exception {
        if (!entorno.isActivo()) terminado = true;
    }

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

    /** Gestiona excepciones producidas durante la ejecución del bucle ante fallos inesperados. */
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
        entorno.getEstadoControl()
                .ifPresent(estado -> entorno.getVisualizador().mostrarFin(estado));
        myAgent.doDelete();
        return 0;
    }
}
