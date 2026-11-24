package grid;

import base.Politica;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pruebas locales de las políticas de navegación en Grid (V1 y V2) con bumpers simulados y sin
 * necesidad de conexión a DBA Lab.
 */
public final class PoliticaGridPrueba {

    public static void main(String[] args) {
        probarPoliticaGrid();
        probarPoliticaGridV2();
        System.out.println(
                "Todas las pruebas de PoliticaGrid y PoliticaGridV2 han sido superadas con éxito.");
    }

    private static void probarPoliticaGrid() {
        Posicion inicio = new Posicion(0, 0);
        Posicion objetivo = new Posicion(2, 0);
        exigir(simularV1(inicio, objetivo, "...", "...") == 2, "V1 avance directo");
        exigir(simularV1(inicio, objetivo, ".#.", "...") == 4, "V1 rodeo de un muro");
        exigir(
                simularV1(
                                new Posicion(1, 0),
                                new Posicion(1, 5),
                                "....#.#.",
                                "..#..#..",
                                ".#..#.#.",
                                "........",
                                ".##....#",
                                ".....###",
                                ".....#.#",
                                "#.....#.")
                        == 31,
                "V1 recuperación tras cambiar de mano");

        esperarParadaV1(
                "Ciclo de navegación",
                () ->
                        simularV1(
                                inicio,
                                new Posicion(2, 2),
                                ".....",
                                ".###.",
                                ".#.#.",
                                ".###.",
                                "....."));

        PoliticaGrid politica = new PoliticaGrid();
        for (int energia : new int[] {0, -1}) {
            esperarParadaV1(
                    "Energía finita agotada",
                    () ->
                            politica.aplicar(
                                    new SensoresGrid(
                                            inicio, objetivo, Set.of(), "FINITE", energia)));
        }
        exigir(
                politica.aplicar(new SensoresGrid(inicio, objetivo, Set.of(), "FINITE", 1))
                        == Direccion.ESTE,
                "V1 energía positiva con coste desconocido");
        exigir(
                new PoliticaGrid()
                                .aplicar(
                                        new SensoresGrid(inicio, objetivo, Set.of(), "INFINITE", 0))
                        == Direccion.ESTE,
                "V1 energía infinita con remaining a cero");

        System.out.println("Pruebas de PoliticaGrid (V1) superadas");
    }

    private static void probarPoliticaGridV2() {
        PoliticaGridV2 pol = new PoliticaGridV2();
        exigir(pol.getModo() == PoliticaGridV2.Modo.ACERCAMIENTO, "V2 estado inicial modo");
        exigir(pol.getMano() == PoliticaGridV2.Mano.DERECHA, "V2 estado inicial mano");
        exigir(pol.getOrientacion().isEmpty(), "V2 estado inicial orientacion");
        exigir(pol.getDistanciaImpacto() == 0, "V2 estado inicial distancia impacto");
        exigir(!pol.isCambioDeManoIntentado(), "V2 estado inicial cambio de mano");

        Posicion inicio = new Posicion(0, 0);
        Posicion objetivo = new Posicion(2, 0);
        exigir(simularV2(inicio, objetivo, "...", "...") == 2, "V2 avance directo");
        exigir(simularV2(inicio, objetivo, ".#.", "...") == 4, "V2 rodeo de un muro");

        // En V2 la inercia direccional y el eje dominante optimizan la recuperación a 27 pasos
        exigir(
                simularV2(
                                new Posicion(1, 0),
                                new Posicion(1, 5),
                                "....#.#.",
                                "..#..#..",
                                ".#..#.#.",
                                "........",
                                ".##....#",
                                ".....###",
                                ".....#.#",
                                "#.....#.")
                        == 27,
                "V2 recuperación optimizada tras cambiar de mano");

        esperarParadaV2(
                "Ciclo de navegacion",
                () ->
                        simularV2(
                                inicio,
                                new Posicion(2, 2),
                                ".....",
                                ".###.",
                                ".#.#.",
                                ".###.",
                                "....."));

        pol.reiniciar();
        for (int energia : new int[] {0, -1}) {
            esperarParadaV2(
                    "Energia finita agotada",
                    () ->
                            pol.aplicar(
                                    new SensoresGrid(
                                            inicio, objetivo, Set.of(), "FINITE", energia)));
        }
        pol.reiniciar();
        exigir(
                pol.aplicar(new SensoresGrid(inicio, objetivo, Set.of(), "FINITE", 1))
                        == Direccion.ESTE,
                "V2 energía positiva");
        pol.reiniciar();
        exigir(
                pol.aplicar(new SensoresGrid(inicio, objetivo, Set.of(), "INFINITE", 0))
                        == Direccion.ESTE,
                "V2 energía infinita");

        System.out.println("Pruebas de PoliticaGridV2 superadas");
    }

    private static int simularV1(Posicion posicion, Posicion objetivo, String... mapa) {
        return simular(new PoliticaGrid(), posicion, objetivo, mapa);
    }

    private static int simularV2(Posicion posicion, Posicion objetivo, String... mapa) {
        return simular(new PoliticaGridV2(), posicion, objetivo, mapa);
    }

    private static int simular(
            Politica<SensoresGrid, Direccion> politica,
            Posicion posicion,
            Posicion objetivo,
            String... mapa) {
        for (int pasos = 0; pasos <= 100; pasos++) {
            if (posicion.equals(objetivo)) return pasos;
            Set<Direccion> bloqueadas = construirBloqueadas(posicion, mapa);
            Direccion accion =
                    politica.aplicar(
                            new SensoresGrid(posicion, objetivo, bloqueadas, "INFINITE", 0));
            exigir(!bloqueadas.contains(accion), "movimiento libre en " + posicion);
            posicion = posicion.mover(accion);
        }
        throw new AssertionError("La navegación superó el límite de la prueba");
    }

    private static Set<Direccion> construirBloqueadas(Posicion posicion, String... mapa) {
        Set<Direccion> bloqueadas = EnumSet.noneOf(Direccion.class);
        for (Direccion direccion : Direccion.values()) {
            Posicion vecina = posicion.mover(direccion);
            if (vecina.y() < 0
                    || vecina.y() >= mapa.length
                    || vecina.x() < 0
                    || vecina.x() >= mapa[vecina.y()].length()
                    || mapa[vecina.y()].charAt(vecina.x()) == '#') {
                bloqueadas.add(direccion);
            }
        }
        return bloqueadas;
    }

    private static void esperarParadaV1(String motivo, Runnable prueba) {
        try {
            prueba.run();
        } catch (PoliticaGrid.ParadaNavegacion parada) {
            exigir(parada.getMessage().startsWith(motivo), parada.getMessage());
            return;
        }
        throw new AssertionError("Se esperaba la parada V1: " + motivo);
    }

    private static void esperarParadaV2(String motivo, Runnable prueba) {
        try {
            prueba.run();
        } catch (PoliticaGridV2.ParadaNavegacion parada) {
            exigir(parada.getMessage().startsWith(motivo), parada.getMessage());
            return;
        }
        throw new AssertionError("Se esperaba la parada V2: " + motivo);
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
