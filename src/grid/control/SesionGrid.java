package grid.control;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Señal de parada y acuse de cierre. No conoce Swing, HTTP ni el planificador JADE. */
public final class SesionGrid {
    private final long retoId;
    private final AtomicBoolean detener = new AtomicBoolean();
    private final CompletableFuture<Void> fin = new CompletableFuture<>();

    public SesionGrid(long retoId) { this.retoId = retoId; }
    public long retoId() { return retoId; }
    public void solicitarParada() { detener.set(true); }
    public boolean detenerSolicitado() { return detener.get(); }

    /** El agente confirma solo después de intentar cerrar el run en takeDown. */
    public void completar(Throwable error) {
        if (error == null) fin.complete(null);
        else fin.completeExceptionally(error);
    }

    public void esperarFin() throws Exception { fin.get(65, TimeUnit.SECONDS); }
}
