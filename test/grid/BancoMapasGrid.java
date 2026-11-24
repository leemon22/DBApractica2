package grid;

import base.Politica;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Banco de pruebas local de las políticas de Grid, sin JADE ni DBA Lab.
 * <p>
 * Ejecuta cada política sobre los 7 mapas de la primera tanda (Pr2.pdf, sección 3.3) con todas las
 * combinaciones de inicio y objetivo en celdas libres, y sobre mapas aleatorios que imitan los
 * mundos desconocidos de la segunda tanda. Clasifica cada caso como llegada, parada por ciclo,
 * otra parada, bucle (supera el límite de pasos), choque o error, y compara V1 con V2 caso a caso.
 * <p>
 * Uso, desde la raíz del proyecto:
 * <pre>
 *   java -cp CLASES grid.BancoMapasGrid
 *   java -cp CLASES grid.BancoMapasGrid traza "Barrera en C" X0 Y0 X1 Y1 v1|v2
 * </pre>
 * El segundo modo dibuja el recorrido de un caso concreto (x = columna, y = fila desde arriba).
 */
public final class BancoMapasGrid {

    // Transcritos de las imágenes de Pr2.pdf leyendo el color del centro de cada celda.
    private static final Map<String, String[]> MAPAS_PRIMERA_TANDA = new LinkedHashMap<>();

    static {
        MAPAS_PRIMERA_TANDA.put("Campo abierto", new String[]{
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            "..........",
            ".........."});
        MAPAS_PRIMERA_TANDA.put("Muro vertical", new String[]{
            "..........",
            "..........",
            "....#.....",
            "....#.....",
            "....#.....",
            "....#.....",
            "....#.....",
            "....#.....",
            "....#.....",
            "....#....."});
        MAPAS_PRIMERA_TANDA.put("Muro horizontal", new String[]{
            "..........",
            "..........",
            "..........",
            "..........",
            "...####...",
            "..........",
            "..........",
            "..........",
            "..........",
            ".........."});
        MAPAS_PRIMERA_TANDA.put("Muro diagonal", new String[]{
            "..........",
            "..........",
            ".........#",
            "........#.",
            ".......#..",
            "......#...",
            ".....#....",
            "....#.....",
            "..........",
            ".........."});
        MAPAS_PRIMERA_TANDA.put("Barrera en U", new String[]{
            "..........",
            "..........",
            "..........",
            "....#.....",
            "....#..#..",
            "....#..#..",
            "....####..",
            "..........",
            "..........",
            ".........."});
        MAPAS_PRIMERA_TANDA.put("Barrera en arco", new String[]{
            "..........",
            "..........",
            "..........",
            "..........",
            "...######.",
            "...#....#.",
            "...#....#.",
            "...#....#.",
            "..........",
            ".........."});
        MAPAS_PRIMERA_TANDA.put("Barrera en C", new String[]{
            "..........",
            "..........",
            "..........",
            "..........",
            ".....####.",
            ".....#..#.",
            ".....#..#.",
            ".....#....",
            ".....#....",
            ".....####."});
    }

    private static final Map<String, Supplier<Politica<SensoresGrid, Direccion>>> POLITICAS = new LinkedHashMap<>();

    static {
        POLITICAS.put("V1", PoliticaGrid::new);
        POLITICAS.put("V2", PoliticaGridV2::new);
    }

    // Mapas aleatorios: semilla fija para que los resultados sean reproducibles.
    private static final long SEMILLA = 2026L;
    private static final int MAPAS_ALEATORIOS = 300;
    private static final int CASOS_POR_MAPA_ALEATORIO = 40;
    private static final int EJEMPLOS_POR_GRUPO = 3;

    private enum Resultado { LLEGA, PARADA_CICLO, PARADA_OTRA, BUCLE, CHOQUE, ERROR }

    private record Caso(Resultado resultado, int pasos, String detalle) {
    }

    /** Mapa rectangular: '#' es obstáculo y los límites también bloquean. */
    private record Mapa(String nombre, String[] filas) {

        int alto() {
            return filas.length;
        }

        int ancho() {
            return filas[0].length();
        }

        boolean libre(Posicion p) {
            return p.y() >= 0 && p.y() < alto() && p.x() >= 0 && p.x() < ancho()
                    && filas[p.y()].charAt(p.x()) != '#';
        }

        Set<Direccion> bloqueadas(Posicion p) {
            Set<Direccion> bloqueadas = EnumSet.noneOf(Direccion.class);
            for (Direccion d : Direccion.values()) {
                if (!libre(p.mover(d))) {
                    bloqueadas.add(d);
                }
            }
            return bloqueadas;
        }

        List<Posicion> celdasLibres() {
            List<Posicion> libres = new ArrayList<>();
            for (int y = 0; y < alto(); y++) {
                for (int x = 0; x < ancho(); x++) {
                    Posicion p = new Posicion(x, y);
                    if (libre(p)) {
                        libres.add(p);
                    }
                }
            }
            return libres;
        }

        // Límite generoso: un recorrido legítimo de Bug0 no se acerca a 10 visitas por celda.
        int limitePasos() {
            return 10 * alto() * ancho();
        }
    }

    /** Resultados acumulados de una política sobre un grupo de casos. */
    private static final class Estadisticas {
        final Map<Resultado, Integer> cuenta = new EnumMap<>(Resultado.class);
        final List<String> ejemplosFallo = new ArrayList<>();
        int casos;
        long sumaExtra;
        int maxExtra = -1;
        String casoMaxExtra = "";

        void registrar(Caso caso, int manhattan, String descripcion) {
            casos++;
            cuenta.merge(caso.resultado(), 1, Integer::sum);
            if (caso.resultado() == Resultado.LLEGA) {
                int extra = caso.pasos() - manhattan;
                sumaExtra += extra;
                if (extra > maxExtra) {
                    maxExtra = extra;
                    casoMaxExtra = descripcion;
                }
            } else if (ejemplosFallo.size() < EJEMPLOS_POR_GRUPO) {
                ejemplosFallo.add(caso.resultado() + " " + descripcion
                        + (caso.detalle().isEmpty() ? "" : " (" + caso.detalle() + ")"));
            }
        }

        int de(Resultado r) {
            return cuenta.getOrDefault(r, 0);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("traza")) {
            trazar(args);
            return;
        }

        boolean inseguro = false;
        System.out.println("Banco de PoliticaGrid (V1) y PoliticaGridV2 (V2)");
        System.out.println("Mapas de la primera tanda: todas las combinaciones inicio != objetivo en celdas libres.");

        Map<String, List<Mapa>> grupos = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : MAPAS_PRIMERA_TANDA.entrySet()) {
            grupos.put(e.getKey(), List.of(new Mapa(e.getKey(), e.getValue())));
        }
        grupos.put("Aleatorios", generarMapasAleatorios());
        grupos.put("Muros aleatorios", generarMapasConMuros());

        Map<String, Map<String, Estadisticas>> porPolitica = new LinkedHashMap<>();
        Map<String, int[]> diferencias = new LinkedHashMap<>();
        Map<String, List<String>> ejemplosDiferencia = new LinkedHashMap<>();
        Random random = new Random(SEMILLA);

        for (Map.Entry<String, List<Mapa>> grupo : grupos.entrySet()) {
            boolean aleatorio = !MAPAS_PRIMERA_TANDA.containsKey(grupo.getKey());
            int[] dif = new int[2]; // [0] solo V1 llega, [1] solo V2 llega
            List<String> ejemplos = new ArrayList<>();
            for (Mapa mapa : grupo.getValue()) {
                for (Posicion[] par : pares(mapa, aleatorio, random)) {
                    Posicion inicio = par[0];
                    Posicion objetivo = par[1];
                    int manhattan = inicio.distanciaManhattan(objetivo);
                    String descripcion = describir(mapa, aleatorio, inicio, objetivo);
                    Map<String, Resultado> resultados = new LinkedHashMap<>();
                    for (Map.Entry<String, Supplier<Politica<SensoresGrid, Direccion>>> pol : POLITICAS.entrySet()) {
                        Caso caso = simular(pol.getValue(), mapa, inicio, objetivo, null);
                        porPolitica.computeIfAbsent(pol.getKey(), k -> new LinkedHashMap<>())
                                .computeIfAbsent(grupo.getKey(), k -> new Estadisticas())
                                .registrar(caso, manhattan, descripcion);
                        resultados.put(pol.getKey(), caso.resultado());
                        inseguro |= caso.resultado() == Resultado.CHOQUE || caso.resultado() == Resultado.ERROR;
                    }
                    boolean llegaV1 = resultados.get("V1") == Resultado.LLEGA;
                    boolean llegaV2 = resultados.get("V2") == Resultado.LLEGA;
                    if (llegaV1 != llegaV2) {
                        dif[llegaV1 ? 0 : 1]++;
                        if (ejemplos.size() < EJEMPLOS_POR_GRUPO) {
                            ejemplos.add(descripcion + ": V1 " + resultados.get("V1") + ", V2 " + resultados.get("V2"));
                        }
                    }
                }
            }
            diferencias.put(grupo.getKey(), dif);
            ejemplosDiferencia.put(grupo.getKey(), ejemplos);
        }

        for (Map.Entry<String, Map<String, Estadisticas>> pol : porPolitica.entrySet()) {
            imprimirTabla(pol.getKey(), pol.getValue());
        }
        imprimirDiferencias(diferencias, ejemplosDiferencia);

        if (inseguro) {
            System.out.println("\nATENCIÓN: hay choques o errores. Revisar antes de lanzar runs en DBA Lab.");
            System.exit(1);
        }
    }

    // --- Simulación ---

    private static Caso simular(Supplier<Politica<SensoresGrid, Direccion>> fabrica, Mapa mapa,
            Posicion inicio, Posicion objetivo, List<Direccion> recorrido) {
        Politica<SensoresGrid, Direccion> politica = fabrica.get();
        Posicion posicion = inicio;
        for (int pasos = 0; pasos <= mapa.limitePasos(); pasos++) {
            if (posicion.equals(objetivo)) {
                return new Caso(Resultado.LLEGA, pasos, "");
            }
            Set<Direccion> bloqueadas = mapa.bloqueadas(posicion);
            Direccion accion;
            try {
                accion = politica.aplicar(new SensoresGrid(posicion, objetivo, bloqueadas, "INFINITE", 0));
            } catch (PoliticaGrid.ParadaNavegacion | PoliticaGridV2.ParadaNavegacion parada) {
                Resultado r = parada.getMessage().startsWith("Ciclo") ? Resultado.PARADA_CICLO : Resultado.PARADA_OTRA;
                return new Caso(r, pasos, "paso " + pasos + ": " + parada.getMessage());
            } catch (RuntimeException e) {
                return new Caso(Resultado.ERROR, pasos, "paso " + pasos + ": " + e);
            }
            if (bloqueadas.contains(accion)) {
                return new Caso(Resultado.CHOQUE, pasos, "paso " + pasos + ": " + accion + " desde " + posicion);
            }
            if (recorrido != null) {
                recorrido.add(accion);
            }
            posicion = posicion.mover(accion);
        }
        return new Caso(Resultado.BUCLE, mapa.limitePasos(), "más de " + mapa.limitePasos() + " pasos");
    }

    // --- Generación de casos ---

    private static List<Posicion[]> pares(Mapa mapa, boolean aleatorio, Random random) {
        List<Posicion> libres = mapa.celdasLibres();
        List<Posicion[]> pares = new ArrayList<>();
        if (!aleatorio) {
            for (Posicion a : libres) {
                for (Posicion b : libres) {
                    if (!a.equals(b)) {
                        pares.add(new Posicion[]{a, b});
                    }
                }
            }
            return pares;
        }
        // En mapas aleatorios solo cuentan los objetivos alcanzables: si no hay camino, nadie llega.
        int intentos = 0;
        while (pares.size() < CASOS_POR_MAPA_ALEATORIO && intentos++ < 50 * CASOS_POR_MAPA_ALEATORIO) {
            Posicion a = libres.get(random.nextInt(libres.size()));
            Posicion b = libres.get(random.nextInt(libres.size()));
            if (!a.equals(b) && alcanzable(mapa, a, b)) {
                pares.add(new Posicion[]{a, b});
            }
        }
        return pares;
    }

    private static boolean alcanzable(Mapa mapa, Posicion desde, Posicion hasta) {
        Set<Posicion> vistas = new HashSet<>(List.of(desde));
        ArrayDeque<Posicion> cola = new ArrayDeque<>(List.of(desde));
        while (!cola.isEmpty()) {
            Posicion p = cola.poll();
            if (p.equals(hasta)) {
                return true;
            }
            for (Direccion d : Direccion.values()) {
                Posicion q = p.mover(d);
                if (mapa.libre(q) && vistas.add(q)) {
                    cola.add(q);
                }
            }
        }
        return false;
    }

    // Mapas de 8x8 a 15x15 con un 10–30 % de obstáculos sueltos.
    private static List<Mapa> generarMapasAleatorios() {
        Random random = new Random(SEMILLA);
        List<Mapa> mapas = new ArrayList<>();
        for (int i = 0; i < MAPAS_ALEATORIOS; i++) {
            int ancho = 8 + random.nextInt(8);
            int alto = 8 + random.nextInt(8);
            double densidad = 0.10 + 0.20 * random.nextDouble();
            char[][] celdas = vacio(ancho, alto);
            for (int y = 0; y < alto; y++) {
                for (int x = 0; x < ancho; x++) {
                    if (random.nextDouble() < densidad) {
                        celdas[y][x] = '#';
                    }
                }
            }
            mapas.add(new Mapa("aleatorio-" + i, aFilas(celdas)));
        }
        return mapas;
    }

    // Mapas de 10x10 a 20x20 con muros rectos largos que se cruzan y forman recovecos,
    // más parecidos a los obstáculos de la primera tanda que los obstáculos sueltos.
    private static List<Mapa> generarMapasConMuros() {
        Random random = new Random(SEMILLA + 1);
        List<Mapa> mapas = new ArrayList<>();
        for (int i = 0; i < MAPAS_ALEATORIOS; i++) {
            int ancho = 10 + random.nextInt(11);
            int alto = 10 + random.nextInt(11);
            char[][] celdas = vacio(ancho, alto);
            int muros = ancho * alto / 25;
            for (int m = 0; m < muros; m++) {
                boolean horizontal = random.nextBoolean();
                int largo = 3 + random.nextInt(Math.max(1, (horizontal ? ancho : alto) / 2));
                int x = random.nextInt(ancho);
                int y = random.nextInt(alto);
                for (int k = 0; k < largo; k++) {
                    int cx = horizontal ? x + k : x;
                    int cy = horizontal ? y : y + k;
                    if (cx < ancho && cy < alto) {
                        celdas[cy][cx] = '#';
                    }
                }
            }
            mapas.add(new Mapa("muros-" + i, aFilas(celdas)));
        }
        return mapas;
    }

    private static char[][] vacio(int ancho, int alto) {
        char[][] celdas = new char[alto][ancho];
        for (char[] fila : celdas) {
            java.util.Arrays.fill(fila, '.');
        }
        return celdas;
    }

    private static String[] aFilas(char[][] celdas) {
        String[] filas = new String[celdas.length];
        for (int y = 0; y < celdas.length; y++) {
            filas[y] = new String(celdas[y]);
        }
        return filas;
    }

    /** Todos los mapas por nombre: los 7 del guion y los aleatorios (deterministas por la semilla). */
    private static Map<String, Mapa> todosLosMapas() {
        Map<String, Mapa> todos = new LinkedHashMap<>();
        MAPAS_PRIMERA_TANDA.forEach((nombre, filas) -> todos.put(nombre, new Mapa(nombre, filas)));
        for (Mapa m : generarMapasAleatorios()) {
            todos.put(m.nombre(), m);
        }
        for (Mapa m : generarMapasConMuros()) {
            todos.put(m.nombre(), m);
        }
        return todos;
    }

    // --- Informe ---

    // Se describe como los argumentos del modo traza, para poder copiarlo y ver el recorrido.
    private static String describir(Mapa mapa, boolean aleatorio, Posicion a, Posicion b) {
        return "traza \"" + mapa.nombre() + "\" " + a.x() + " " + a.y() + " " + b.x() + " " + b.y();
    }

    private static void imprimirTabla(String politica, Map<String, Estadisticas> grupos) {
        System.out.printf("%n== %s ==%n", politica);
        System.out.printf("%-17s %6s %6s %6s %6s %6s %6s %6s %6s %10s %6s%n",
                "Mapa", "Casos", "Llega", "%", "Ciclo", "Parada", "Bucle", "Choque", "Error", "Extra med", "máx");
        for (Map.Entry<String, Estadisticas> e : grupos.entrySet()) {
            Estadisticas s = e.getValue();
            int llegan = s.de(Resultado.LLEGA);
            System.out.printf(Locale.ROOT, "%-17s %6d %6d %6.1f %6d %6d %6d %6d %6d %10.2f %6d%n",
                    e.getKey(), s.casos, llegan, 100.0 * llegan / s.casos,
                    s.de(Resultado.PARADA_CICLO), s.de(Resultado.PARADA_OTRA), s.de(Resultado.BUCLE),
                    s.de(Resultado.CHOQUE), s.de(Resultado.ERROR),
                    llegan == 0 ? 0.0 : (double) s.sumaExtra / llegan, Math.max(s.maxExtra, 0));
        }
        System.out.println("Extra = pasos por encima de la distancia Manhattan, solo en los casos que llegan.");
        for (Map.Entry<String, Estadisticas> e : grupos.entrySet()) {
            for (String ejemplo : e.getValue().ejemplosFallo) {
                System.out.println("  fallo " + e.getKey() + ": " + ejemplo);
            }
        }
    }

    private static void imprimirDiferencias(Map<String, int[]> diferencias, Map<String, List<String>> ejemplos) {
        System.out.printf("%n== Casos en los que solo una de las dos llega ==%n");
        System.out.printf("%-17s %12s %12s%n", "Mapa", "Solo V1", "Solo V2");
        for (Map.Entry<String, int[]> e : diferencias.entrySet()) {
            System.out.printf("%-17s %12d %12d%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
        for (Map.Entry<String, List<String>> e : ejemplos.entrySet()) {
            for (String ejemplo : e.getValue()) {
                System.out.println("  " + ejemplo);
            }
        }
    }

    // --- Modo traza ---

    private static void trazar(String[] args) {
        if (args.length != 7) {
            System.out.println("Uso: traza \"<mapa>\" X0 Y0 X1 Y1 v1|v2");
            System.out.println("Mapas: " + MAPAS_PRIMERA_TANDA.keySet());
            return;
        }
        Mapa mapa = todosLosMapas().values().stream()
                .filter(m -> m.nombre().equalsIgnoreCase(args[1])).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mapa desconocido: " + args[1]
                        + ". Mapas: " + MAPAS_PRIMERA_TANDA.keySet() + ", aleatorio-N, muros-N"));
        String nombre = mapa.nombre();
        Posicion inicio = new Posicion(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        Posicion objetivo = new Posicion(Integer.parseInt(args[4]), Integer.parseInt(args[5]));
        Supplier<Politica<SensoresGrid, Direccion>> fabrica = POLITICAS.get(args[6].toUpperCase(Locale.ROOT));
        if (fabrica == null || !mapa.libre(inicio) || !mapa.libre(objetivo)) {
            throw new IllegalArgumentException("Política (v1|v2) desconocida o inicio/objetivo sobre obstáculo");
        }

        List<Direccion> recorrido = new ArrayList<>();
        Caso caso = simular(fabrica, mapa, inicio, objetivo, recorrido);

        int[][] visitas = new int[mapa.alto()][mapa.ancho()];
        Posicion p = inicio;
        visitas[p.y()][p.x()]++;
        for (Direccion d : recorrido) {
            p = p.mover(d);
            visitas[p.y()][p.x()]++;
        }
        System.out.printf("%s, %s, %s -> %s: %s en %d pasos (Manhattan %d) %s%n", nombre,
                args[6].toUpperCase(Locale.ROOT), inicio, objetivo, caso.resultado(), caso.pasos(),
                inicio.distanciaManhattan(objetivo), caso.detalle());
        System.out.println("I = inicio, O = objetivo, # = obstáculo, 1-9 = veces que pasa por la celda (+ = más de 9)");
        for (int y = 0; y < mapa.alto(); y++) {
            StringBuilder fila = new StringBuilder();
            for (int x = 0; x < mapa.ancho(); x++) {
                Posicion c = new Posicion(x, y);
                char ch;
                if (c.equals(inicio)) {
                    ch = 'I';
                } else if (c.equals(objetivo)) {
                    ch = 'O';
                } else if (!mapa.libre(c)) {
                    ch = '#';
                } else if (visitas[y][x] == 0) {
                    ch = '.';
                } else {
                    ch = visitas[y][x] > 9 ? '+' : (char) ('0' + visitas[y][x]);
                }
                fila.append(ch).append(' ');
            }
            System.out.println(fila);
        }
        StringBuilder movimientos = new StringBuilder();
        for (int i = 0; i < Math.min(recorrido.size(), 200); i++) {
            movimientos.append(recorrido.get(i).name().charAt(0));
        }
        System.out.println("Movimientos" + (recorrido.size() > 200 ? " (primeros 200)" : "") + ": " + movimientos);
    }
}
