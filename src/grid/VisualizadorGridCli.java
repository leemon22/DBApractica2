package grid;

import base.EstadoControl;
import base.InfoReto;
import base.Visualizador;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Visualizador por consola (CLI) para el bloque Grid.
 * Muestra las métricas de percepción en cada paso, genera una representación ASCII
 * del mapa autoescalable y gestiona la selección interactiva de mapas por teclado.
 */
public class VisualizadorGridCli implements Visualizador<SensoresGrid, Direccion> {

    // Caracteres representativos del mapa ASCII
    private static final char CHAR_AGENTE = 'A';
    private static final char CHAR_OBJETIVO = 'X';
    private static final char CHAR_MURO = '#';
    private static final char CHAR_TRANSITADO = '.';
    private static final char CHAR_LIBRE = ' ';
    private static final char CHAR_DESCONOCIDO = '?';

    // Memoria del mapa explorado
    private final Map<Posicion, Character> celdasConocidas = new HashMap<>();
    private final Set<Posicion> rastro = new HashSet<>();
    private Posicion posicionObjetivo;
    private Posicion posicionAgente;

    @Override
    public InfoReto seleccionarReto(List<InfoReto> retosGrid) {
        System.out.println("\n==================================================");
        System.out.println("       RETOS DE GRID DISPONIBLES EN DBA LAB       ");
        System.out.println("==================================================");

        for (int i = 0; i < retosGrid.size(); i++) {
            InfoReto reto = retosGrid.get(i);
            System.out.printf("[%d] Reto ID: %d | Sensores: %d | Acciones: %d%n",
                    i + 1, reto.id(), reto.sensorCodes().size(), reto.actionCodes().size());
        }

        Scanner scanner = new Scanner(System.in);
        int seleccion = -1;

        while (seleccion < 1 || seleccion > retosGrid.size()) {
            System.out.printf("Selecciona el número de mapa a ejecutar (1-%d): ", retosGrid.size());
            if (scanner.hasNextInt()) {
                seleccion = scanner.nextInt();
                if (seleccion < 1 || seleccion > retosGrid.size()) {
                    System.out.println("Opción fuera de rango. Inténtalo de nuevo.");
                }
            } else if (scanner.hasNext()) {
                scanner.next(); // Descartar entrada no numérica
                System.out.println("Por favor, introduce un número válido.");
            } else {
                // Entrada no interactiva: seleccionar el primero por defecto
                System.out.println("\nSin entrada interactiva detectada. Seleccionando reto 1 por defecto.");
                seleccion = 1;
                break;
            }
        }

        return retosGrid.get(seleccion - 1);
    }

    @Override
    public void mostrarRetoEncontrado(InfoReto reto) {
        System.out.println("Reto Grid seleccionado: " + reto.id() + " (" + reto.environmentType() + ")");
    }

    @Override
    public void mostrarInicio(String runId, EstadoControl estado) {
        System.out.println("Run iniciado en DBA Lab: " + runId);
        System.out.println("Estado inicial: " + estado.runStatus());
    }

    @Override
    public void mostrarPercepcion(SensoresGrid sensores) {
        this.posicionAgente = sensores.posicion();
        this.posicionObjetivo = sensores.objetivo();
        this.rastro.add(posicionAgente);

        // Actualizamos los bumpers detectados alrededor de la posición actual
        for (Direccion d : Direccion.values()) {
            Posicion adyacente = posicionAgente.mover(d);
            if (sensores.bloqueado(d)) {
                celdasConocidas.put(adyacente, CHAR_MURO);
            } else if (!rastro.contains(adyacente) && !adyacente.equals(posicionObjetivo)) {
                celdasConocidas.putIfAbsent(adyacente, CHAR_LIBRE);
            }
        }

        // Trazas de métricas del paso
        System.out.println("\n--- PERCEPCIÓN DEL AGENTE ---");
        System.out.printf("Posición: %s | Objetivo: %s | Distancia: %d celdas%n",
                posicionAgente, posicionObjetivo, sensores.distanciaAlObjetivo());
        System.out.printf("Energía restante: %d (%s)%n", sensores.energiaRestante(), sensores.modoEnergia());
        System.out.println("Direcciones bloqueadas: " + sensores.bloqueadas());

        // Renderizado del mapa ASCII dinámico
        dibujarMapa();
    }

    @Override
    public void mostrarAccion(Direccion accion, String resultado, EstadoControl nuevoEstado) {
        System.out.printf("Movimiento: %s (%s) | Resultado: %s | Estado: %s%n",
                accion, accion.codigoAccion(), resultado, nuevoEstado.runStatus());
    }

    @Override
    public void mostrarCierre(String runId) {
        System.out.println("Cerrando run activo en DBA Lab: " + runId);
    }

    @Override
    public void mostrarFin(EstadoControl estadoFinal) {
        System.out.println("\n========================================");
        System.out.println("FIN DE LA EJECUCIÓN GRID: " + estadoFinal.runStatus());
        estadoFinal.motivo().ifPresent(motivo -> System.out.println("Motivo oficial: " + motivo));
        System.out.println("Total de celdas descubiertas: " + celdasConocidas.size());
        System.out.println("Longitud del recorrido: " + rastro.size() + " pasos");
        System.out.println("========================================\n");
    }

    // --- Renderizado ASCII Escalable ---

    private void dibujarMapa() {
        if (posicionAgente == null || posicionObjetivo == null) {
            return;
        }

        // Calculamos los límites mínimos y máximos de la exploración actual
        int minX = Math.min(posicionAgente.x(), posicionObjetivo.x());
        int maxX = Math.max(posicionAgente.x(), posicionObjetivo.x());
        int minY = Math.min(posicionAgente.y(), posicionObjetivo.y());
        int maxY = Math.max(posicionAgente.y(), posicionObjetivo.y());

        for (Posicion p : celdasConocidas.keySet()) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }

        // Añadimos un pequeño margen de 1 celda para visión de contexto
        int xInicio = Math.max(0, minX - 1);
        int xFin = maxX + 1;
        int yInicio = Math.max(0, minY - 1);
        int yFin = maxY + 1;

        System.out.println("\nMapa explorado:");
        // Encabezado con coordenadas X
        System.out.print("    ");
        for (int x = xInicio; x <= xFin; x++) {
            System.out.printf("%2d", x % 100);
        }
        System.out.println();

        System.out.print("   +");
        for (int x = xInicio; x <= xFin; x++) {
            System.out.print("--");
        }
        System.out.println("-+");

        // Filas del mapa
        for (int y = yInicio; y <= yFin; y++) {
            System.out.printf("%2d | ", y);
            for (int x = xInicio; x <= xFin; x++) {
                Posicion p = new Posicion(x, y);
                char glifo;

                if (p.equals(posicionAgente)) {
                    glifo = CHAR_AGENTE;
                } else if (p.equals(posicionObjetivo)) {
                    glifo = CHAR_OBJETIVO;
                } else if (rastro.contains(p)) {
                    // El rastro transitado tiene prioridad sobre las celdas conocidas libres para no sobreescribir el camino
                    glifo = CHAR_TRANSITADO;
                } else if (celdasConocidas.containsKey(p)) {
                    glifo = celdasConocidas.get(p);
                } else {
                    glifo = CHAR_DESCONOCIDO;
                }

                System.out.print(glifo + " ");
            }
            System.out.println("|");
        }

        System.out.print("   +");
        for (int x = xInicio; x <= xFin; x++) {
            System.out.print("--");
        }
        System.out.println("-+");
        System.out.println("Leyenda: A = Agente, X = Objetivo, # = Muro, . = Rastro, ? = Desconocido\n");
    }
}
