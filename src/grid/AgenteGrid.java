package grid;

import base.Agente;
import base.ComponentesAgente;
import grid.vista.FabricaPresentacionGrid;

/** Agente sin argumentos de JADE y sin dependencias de componentes gráficos. */
public class AgenteGrid extends Agente<SensoresGrid, Direccion> {
    private FabricaPresentacionGrid.Tipo tipo;
    private FabricaPresentacionGrid.Presentacion presentacion;

    @Override protected void setup() {
        try {
            tipo = FabricaPresentacionGrid.elegirTipo();
            if (tipo == null) { doDelete(); return; }
            super.setup();
        } catch (Exception e) {
            System.err.println("No se pudo iniciar la presentación: " + e.getMessage());
            doDelete();
        }
    }

    @Override protected ComponentesAgente<SensoresGrid, Direccion> iniciar() {
        presentacion = FabricaPresentacionGrid.crear(tipo);
        EntornoGrid mundo = new EntornoGrid(presentacion);
        PoliticaGridV2 navegacion = new PoliticaGridV2();
        BucleAplicacionGrid bucle = new BucleAplicacionGrid(this, mundo, navegacion);
        return new ComponentesAgente<>(mundo, bucle);
    }
    @Override protected void takeDown() {
        if (entorno == null && presentacion != null && presentacion.control() != null) {
            presentacion.control().cerrarVentana(new IllegalStateException("No se pudo iniciar el entorno; revisa la configuración y la salida"));
            return;
        }
        if (!(entorno instanceof EntornoGrid grid) || !grid.esInteractivo()) { super.takeDown(); return; }
        Throwable fallo = null;
        try { grid.cerrarRun(); }
        catch (Exception e) { fallo = e; }
        finally { grid.cerrarPresentacion(fallo); }
    }
}
