package base;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Clase base abstracta que representa el entorno con el que interactúa un agente en DBA Lab.
 * Centraliza la comunicación con el cliente HTTP, la notificación al visualizador y el ciclo de
 * vida del run.
 *
 * @param <S> tipo de percepciones (debe implementar {@link Sensor})
 * @param <A> tipo de acciones (debe implementar {@link Accion})
 */
public abstract class Entorno<S extends Sensor, A extends Accion> {

    protected final ClienteHttpDba cliente;
    protected final Visualizador<S, A> visualizador;
    protected EstadoControl estadoActual;
    protected String runId;

    /** Cada entorno utiliza un cliente HTTP y una presentación independientes. */
    public Entorno(ClienteHttpDba cliente, Visualizador<S, A> visualizador) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente HTTP no puede ser nulo");
        this.visualizador =
                Objects.requireNonNull(visualizador, "El visualizador no puede ser nulo");
    }

    /** Devuelve el visualizador asociado al entorno. */
    public Visualizador<S, A> getVisualizador() {
        return visualizador;
    }

    /** Descubre un reto compatible y solicita su ejecución en DBA Lab. */
    public abstract void iniciarRun() throws IOException, InterruptedException;

    /** Lee las percepciones que necesita la política y actualiza el estado del run. */
    public abstract S leerSensores() throws IOException, InterruptedException;

    /** Traduce y envía la acción elegida; comunica si el servidor la rechaza. */
    public abstract void ejecutarAccion(A accion) throws IOException, InterruptedException;

    /**
     * Filtra por tipo. Respeta el ID configurado; sin ID, delega la elección si hay varios retos.
     */
    protected Optional<InfoReto> seleccionarReto(
            String tipoEntorno, Optional<Long> challengeIdPreferido)
            throws IOException, InterruptedException {
        List<InfoReto> retos = cliente.getRetos();
        List<InfoReto> compatibles =
                retos.stream()
                        .filter(r -> tipoEntorno.equalsIgnoreCase(r.environmentType()))
                        .toList();

        if (compatibles.isEmpty()) {
            return Optional.empty();
        }

        if (challengeIdPreferido.isPresent()) {
            long preferido = challengeIdPreferido.get();
            for (InfoReto reto : compatibles) {
                if (reto.id() == preferido) return Optional.of(reto);
            }
            throw new IllegalStateException(
                    "El reto configurado (" + preferido + ") no está disponible en el servidor.");
        }

        if (compatibles.size() == 1) {
            return Optional.of(compatibles.get(0));
        }

        // Si hay múltiples retos compatibles y no hay ID fijado, delegamos en el visualizador (CLI,
        // GUI o default)
        return Optional.of(visualizador.seleccionarReto(compatibles));
    }

    /** Solo ACTIVE permite continuar; sin run iniciado devuelve false. */
    public boolean isActivo() {
        return estadoActual != null && estadoActual.isActivo();
    }

    /** Último estado oficial, o vacío antes de iniciar el run. */
    public Optional<EstadoControl> getEstadoControl() {
        return Optional.ofNullable(estadoActual);
    }

    /** Identificador del run, o vacío antes de iniciarlo. */
    public Optional<String> getRunId() {
        return Optional.ofNullable(runId);
    }

    /** Solicita el cierre solo si el run sigue activo. Un cierre fallido se propaga al llamador. */
    public void cerrarRun() throws IOException, InterruptedException {
        if (runId != null && isActivo()) {
            visualizador.mostrarCierre(runId);
            this.estadoActual = cliente.cerrarRun(runId);
        }
    }
}
