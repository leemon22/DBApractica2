package grid;

import base.EstadoControl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Comprueba el resumen visible con revisitas, lecturas repetidas y una llegada sin otra lectura.
 */
public final class VisualizadorGridCliPrueba {
    public static void main(String[] args) {
        VisualizadorGridCli vista = new VisualizadorGridCli();
        EstadoControl activo = new EstadoControl("prueba", EstadoControl.RunStatus.ACTIVE, null);
        EstadoControl exito =
                new EstadoControl("prueba", EstadoControl.RunStatus.SUCCEEDED, "TARGET_REACHED");
        PrintStream salidaOriginal = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(salida, true, StandardCharsets.UTF_8)) {
            System.setOut(captura);
            vista.mostrarInicio("prueba", activo);
            vista.mostrarPercepcion(percepcion(0));
            vista.mostrarPercepcion(percepcion(0));
            comprobarResumen(vista, activo, salida, 0);

            vista.mostrarAccion(Direccion.ESTE, "APPLIED", activo);
            vista.mostrarPercepcion(percepcion(1));
            vista.mostrarAccion(Direccion.ESTE, "REJECTED", activo);
            vista.mostrarAccion(Direccion.OESTE, "APPLIED", activo);
            vista.mostrarPercepcion(percepcion(0));
            vista.mostrarAccion(Direccion.ESTE, "APPLIED", activo);
            vista.mostrarPercepcion(percepcion(1));
            vista.mostrarAccion(Direccion.ESTE, "APPLIED", exito);

            // Solo se percibieron dos posiciones distintas, pero se aplicaron cuatro pasos.
            comprobarResumen(vista, exito, salida, 4);
            comprobarResumen(vista, exito, salida, 4);

            vista.mostrarInicio("otra-ejecucion", activo);
            vista.mostrarAccion(Direccion.ESTE, "REJECTED", activo);
            comprobarResumen(vista, activo, salida, 0);
        } finally {
            System.setOut(salidaOriginal);
        }
        System.out.println("Recorrido CLI: revisitas, rechazos, llegada y reinicio correctos.");
    }

    private static SensoresGrid percepcion(int x) {
        return new SensoresGrid(new Posicion(x, 0), new Posicion(2, 0), Set.of(), "FINITE", 10);
    }

    private static void comprobarResumen(
            VisualizadorGridCli vista,
            EstadoControl estado,
            ByteArrayOutputStream salida,
            int pasos) {
        salida.reset();
        vista.mostrarFin(estado);
        if (!salida.toString(StandardCharsets.UTF_8)
                .contains("Longitud del recorrido: " + pasos + " pasos")) {
            throw new AssertionError("El resumen debe indicar " + pasos + " movimientos aplicados");
        }
    }
}
