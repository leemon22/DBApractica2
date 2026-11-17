package base;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Clase base abstracta que representa el entorno con el que interactúa un agente en DBA Lab.
 * Centraliza la comunicación con el cliente HTTP, la notificación al visualizador y el ciclo de vida del run.
 *
 * @param <S> tipo de percepciones (debe implementar {@link Sensor})
 * @param <A> tipo de acciones (debe implementar {@link Accion})
 */
public abstract class Entorno<S extends Sensor, A extends Accion> {

    protected final ClienteHttpDba cliente;
    protected final Visualizador<S, A> visualizador;
    protected EstadoControl estadoActual;
    protected String runId;

    /**
     * Construye un entorno asociando el cliente HTTP para DBA Lab y el visualizador correspondiente.
     *
     * @param cliente      cliente de comunicación con DBA Lab
     * @param visualizador visualizador de eventos y estados
     */
    public Entorno(ClienteHttpDba cliente, Visualizador<S, A> visualizador) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente HTTP no puede ser nulo");
        this.visualizador = Objects.requireNonNull(visualizador, "El visualizador no puede ser nulo");
    }

    /**
     * Devuelve el visualizador asociado al entorno.
     */
    public Visualizador<S, A> getVisualizador() {
        return visualizador;
    }

    /**
     * Inicia el descubrimiento del reto y solicita el nuevo run en DBA Lab.
     *
     * @throws IOException          si ocurre un fallo de red durante el inicio
     * @throws InterruptedException si se interrumpe la comunicación
     */
    public abstract void iniciarRun() throws IOException, InterruptedException;

    /**
     * Lee las percepciones necesarias del entorno.
     *
     * @return percepciones actuales del agente
     * @throws IOException          si ocurre un error de comunicación al consultar sensores
     * @throws InterruptedException si se interrumpe la comunicación
     */
    public abstract S leerSensores() throws IOException, InterruptedException;

    /**
     * Envía la acción elegida por la política al entorno para su ejecución física.
     *
     * @param accion acción física o de movimiento a ejecutar
     * @throws IOException          si ocurre un error de comunicación al enviar la acción
     * @throws InterruptedException si se interrumpe la comunicación
     */
    public abstract void ejecutarAccion(A accion) throws IOException, InterruptedException;

    /**
     * Selecciona un reto de DBA Lab usando {@link Optional}.
     * Si se proporciona un identificador de reto preferido, busca dicho ID y comprueba que exista.
     * Si no se especifica reto preferido:
     * <ul>
     *   <li>Si hay un único reto compatible con el tipo solicitado, se selecciona directamente.</li>
     *   <li>Si hay múltiples retos compatibles, delega la selección en {@link Visualizador#seleccionarReto(List)}.</li>
     * </ul>
     *
     * @param tipoEntorno          tipo de entorno buscado (p. ej. "THERMOSTAT", "GRID_2D")
     * @param challengeIdPreferido ID numérico explícito opcional (p. ej. de config.properties)
     * @return Optional con el InfoReto seleccionado, o vacío si no hay coincidencias
     * @throws IOException          si ocurre un fallo de red
     * @throws InterruptedException si se interrumpe la comunicación
     */
    protected Optional<InfoReto> seleccionarReto(String tipoEntorno, Optional<Long> challengeIdPreferido)
            throws IOException, InterruptedException {
        List<InfoReto> retos = cliente.getRetos();
        List<InfoReto> compatibles = retos.stream()
                .filter(r -> tipoEntorno.equalsIgnoreCase(r.environmentType()))
                .toList();

        if (compatibles.isEmpty()) {
            return Optional.empty();
        }

        if (challengeIdPreferido.isPresent()) {
            long preferido = challengeIdPreferido.get();
            Optional<InfoReto> especifico = compatibles.stream()
                    .filter(r -> r.id() == preferido)
                    .findFirst();
            if (especifico.isPresent()) {
                return especifico;
            }
            throw new IllegalStateException("El reto configurado (" + preferido + ") no está disponible en el servidor.");
        }

        if (compatibles.size() == 1) {
            return Optional.of(compatibles.get(0));
        }

        // Si hay múltiples retos compatibles y no hay ID fijado, delegamos en el visualizador (CLI, GUI o default)
        return Optional.of(visualizador.seleccionarReto(compatibles));
    }

    /**
     * Comprueba si la ejecución sigue activa en DBA Lab.
     *
     * @return true si el estado actual es ACTIVE, false si es terminal o no ha iniciado
     */
    public boolean isActivo() {
        return estadoActual != null && estadoActual.isActivo();
    }

    /**
     * Devuelve el estado de control actual envuelto en un {@link Optional}.
     *
     * @return Optional con el estado de control o vacío si no se ha iniciado
     */
    public Optional<EstadoControl> getEstadoControl() {
        return Optional.ofNullable(estadoActual);
    }

    /**
     * Devuelve el identificador de la ejecución actual envuelto en un {@link Optional}.
     *
     * @return Optional con el runId o vacío si no se ha iniciado
     */
    public Optional<String> getRunId() {
        return Optional.ofNullable(runId);
    }

    /**
     * Cierra el run activo en DBA Lab (pasa a INTERRUPTED), liberando la sesión del estudiante.
     *
     * @throws IOException          si ocurre un error de comunicación
     * @throws InterruptedException si se interrumpe la comunicación
     */
    public void cerrarRun() throws IOException, InterruptedException {
        if (runId != null && isActivo()) {
            visualizador.mostrarCierre(runId);
            this.estadoActual = cliente.cerrarRun(runId);
        }
    }
}
