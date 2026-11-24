package grid;

import base.Agente;
import base.ComponentesAgente;
import base.ClienteHttpDba;
import base.Configuracion;
import base.Visualizador;
import base.Politica;
import grid.control.SesionGrid;
import java.util.Optional;

/**
 * Agente JADE específico para la navegación autónoma en el entorno Grid (2D).
 */
public class AgenteGrid extends Agente<SensoresGrid, Direccion> {
    private SesionGrid sesion;

    @Override
    @SuppressWarnings("unchecked")
    protected ComponentesAgente<SensoresGrid, Direccion> iniciar() {
        // El lanzador inyecta un contrato de presentación; el agente no conoce Swing.
        Object[] argumentos = getArguments();
        sesion = argumentos != null && argumentos.length > 1 && argumentos[1] instanceof SesionGrid control
                ? control : null;
        EntornoGrid entorno = argumentos != null && argumentos.length > 0
                && argumentos[0] instanceof Visualizador<?, ?>
                ? new EntornoGrid(new ClienteHttpDba(),
                        (Visualizador<SensoresGrid, Direccion>) argumentos[0],
                        sesion != null ? Optional.of(sesion.retoId()) : new Configuracion().getChallengeId())
                : new EntornoGrid();
        PoliticaGridV2 navegacion = new PoliticaGridV2();
        Politica<SensoresGrid, Direccion> politica = sensores -> {
            if (sesion != null && sesion.detenerSolicitado()) {
                throw new PoliticaGridV2.ParadaNavegacion("Parada solicitada por el controlador");
            }
            return navegacion.aplicar(sensores);
        };
        BucleAgenticoGrid bucle = new BucleAgenticoGrid(this, entorno, politica) {
            @Override protected void antes() {
                if (sesion != null && sesion.detenerSolicitado()) { terminado = true; return; }
                super.antes();
            }
        };

        return new ComponentesAgente<>(entorno, bucle);
    }

    @Override protected void takeDown() {
        if (sesion == null) { super.takeDown(); return; }
        Throwable fallo = null;
        try {
            if (entorno != null) {
                entorno.cerrarRun();
                if (entorno.isActivo()) throw new IllegalStateException("El run sigue activo después de solicitar el cierre");
            }
        } catch (Exception e) {
            fallo = e;
            System.err.println("No se pudo cerrar el run de Grid: " + e.getMessage());
        } finally {
            sesion.completar(fallo);
        }
    }
}
