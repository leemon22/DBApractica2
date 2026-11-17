/**
 * Paquete del bloque Grid (entorno bidimensional y navegación autónoma).
 * <p>
 * Reutiliza el marco de trabajo del paquete {@code base}:
 * las percepciones extienden {@link base.Sensor} (mediante {@link grid.SensoresGrid}),
 * y las acciones implementan {@link base.Accion} (mediante {@link grid.Direccion}).
 * <p>
 * Componentes principales del bloque:
 * <ul>
 *   <li>{@link grid.AgenteGrid}: Agente JADE que coordina el ciclo de vida en Grid.</li>
 *   <li>{@link grid.EntornoGrid}: Entorno que comunica con DBA Lab, cachea el objetivo y envía movimientos.</li>
 *   <li>{@link grid.BucleAgenticoGrid}: Bucle agéntico que comprueba condiciones de parada en la meta o por fin de run.</li>
 *   <li>{@link grid.VisualizadorGridCli}: Vista por consola (CLI) con renderizado ASCII dinámico y autoescalable.</li>
 *   <li>{@link grid.PoliticaGrid}: Política reactiva Bug0 con progreso y elección dinámica de mano.</li>
 *   <li>{@link grid.Direccion}: Direcciones cardinales (NORTE, ESTE, SUR, OESTE) y acciones de movimiento.</li>
 *   <li>{@link grid.Posicion}: Coordenadas (x, y) inmutables y cálculo de heurística de distancia Manhattan.</li>
 *   <li>{@link grid.SensoresGrid}: Percepciones del agente con posición, objetivo, bumpers y energía.</li>
 * </ul>
 */
package grid;
