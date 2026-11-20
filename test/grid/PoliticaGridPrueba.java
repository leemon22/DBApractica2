package grid;

import java.util.EnumSet;
import java.util.Set;

/** Pruebas locales de la política con bumpers simulados y sin acceso a DBA Lab. */
public final class PoliticaGridPrueba {

    public static void main(String[] args) {
        Posicion inicio = new Posicion(0, 0);
        Posicion objetivo = new Posicion(2, 0);
        exigir(simular(inicio, objetivo, "...", "...") == 2, "avance directo");
        exigir(simular(inicio, objetivo, ".#.", "...") == 4, "rodeo de un muro");
        exigir(simular(new Posicion(1, 0), new Posicion(1, 5),
                "....#.#.",
                "..#..#..",
                ".#..#.#.",
                "........",
                ".##....#",
                ".....###",
                ".....#.#",
                "#.....#.") == 31, "recuperación tras cambiar de mano");

        esperarParada("Ciclo de navegación", () -> simular(inicio, new Posicion(2, 2),
                ".....",
                ".###.",
                ".#.#.",
                ".###.",
                "....."));

        PoliticaGrid politica = new PoliticaGrid();
        for (int energia : new int[]{0, -1}) {
            esperarParada("Energía finita agotada", () -> politica.aplicar(
                    new SensoresGrid(inicio, objetivo, Set.of(), "FINITE", energia)));
        }
        exigir(politica.aplicar(new SensoresGrid(inicio, objetivo, Set.of(), "FINITE", 1))
                == Direccion.ESTE, "energía positiva con coste desconocido");
        exigir(new PoliticaGrid().aplicar(
                new SensoresGrid(inicio, objetivo, Set.of(), "INFINITE", 0))
                == Direccion.ESTE, "energía infinita con remaining a cero");

        System.out.println("Pruebas de PoliticaGrid superadas");
    }

    private static int simular(Posicion posicion, Posicion objetivo, String... mapa) {
        PoliticaGrid politica = new PoliticaGrid();
        for (int pasos = 0; pasos <= 100; pasos++) {
            if (posicion.equals(objetivo)) {
                return pasos;
            }
            Set<Direccion> bloqueadas = EnumSet.noneOf(Direccion.class);
            for (Direccion direccion : Direccion.values()) {
                Posicion vecina = posicion.mover(direccion);
                if (vecina.y() < 0 || vecina.y() >= mapa.length || vecina.x() < 0
                        || vecina.x() >= mapa[vecina.y()].length()
                        || mapa[vecina.y()].charAt(vecina.x()) == '#') {
                    bloqueadas.add(direccion);
                }
            }
            Direccion accion = politica.aplicar(
                    new SensoresGrid(posicion, objetivo, bloqueadas, "INFINITE", 0));
            exigir(!bloqueadas.contains(accion), "movimiento libre en " + posicion);
            posicion = posicion.mover(accion);
        }
        throw new AssertionError("La navegación superó el límite de la prueba");
    }

    private static void esperarParada(String motivo, Runnable prueba) {
        try {
            prueba.run();
        } catch (PoliticaGrid.ParadaNavegacion parada) {
            exigir(parada.getMessage().startsWith(motivo), parada.getMessage());
            return;
        }
        throw new AssertionError("Se esperaba la parada: " + motivo);
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
