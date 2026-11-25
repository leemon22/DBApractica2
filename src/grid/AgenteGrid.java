package grid;

import base.Agente;
import base.ComponentesAgente;

import grid.vista.FabricaPresentacionGrid;

/** Crea los componentes de Grid. JADE lo instancia sin argumentos. */
public class AgenteGrid extends Agente<SensoresGrid, Direccion> {
    private FabricaPresentacionGrid.Tipo tipo;
    private FabricaPresentacionGrid.Presentacion presentacion;

    @Override
    protected void setup() {
        try {
            tipo = FabricaPresentacionGrid.elegirTipo();
            if (tipo == null) {
                doDelete(); // Cancelar el selector no debe crear un run.
                return;
            }
            super.setup();
        } catch (Exception e) {
            System.err.println("No se pudo iniciar la presentación: " + e.getMessage());
            doDelete();
        }
    }

    @Override
    protected ComponentesAgente<SensoresGrid, Direccion> iniciar() {
        presentacion = FabricaPresentacionGrid.crear(tipo);
        EntornoGrid mundo = new EntornoGrid(presentacion);
        PoliticaGridV2 navegacion = new PoliticaGridV2();
        BucleAgenticoGrid bucle = new BucleAgenticoGrid(this, mundo, navegacion);
        return new ComponentesAgente<>(mundo, bucle);
    }

    @Override
    protected void takeDown() {
        if (entorno == null && presentacion != null && presentacion.control() != null) {
            // La vista puede existir aunque falle la configuración del cliente HTTP.
            presentacion
                    .control()
                    .cerrarVentana(
                            new IllegalStateException(
                                    "No se pudo iniciar el entorno; revisa la configuración y la"
                                            + " salida"));
            return;
        }
        if (!(entorno instanceof EntornoGrid mundo) || !mundo.esInteractivo()) {
            super.takeDown();
            return;
        }
        Exception fallo = null;
        try {
            mundo.terminarEjecucion();
        } catch (Exception e) {
            fallo = e;
        } finally {
            mundo.cerrarPresentacion(fallo);
        }
    }
}
