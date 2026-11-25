/**
 * Paquete del bloque Grid (entorno bidimensional y navegación autónoma).
 *
 * <p>Reutiliza el marco de trabajo del paquete {@code base}: las percepciones extienden {@link
 * base.Sensor} (mediante {@link grid.SensoresGrid}), y las acciones implementan {@link base.Accion}
 * (mediante {@link grid.Direccion}).
 *
 * <p>Componentes principales del bloque:
 *
 * <ul>
 *   <li>{@link grid.AgenteGrid}: Agente JADE que coordina el ciclo de vida en Grid.
 *   <li>{@link grid.EntornoGrid}: Entorno que comunica con DBA Lab, cachea el objetivo y envía
 *       movimientos.
 *   <li>{@link grid.BucleAgenticoGrid}: Bucle agéntico que comprueba condiciones de parada en la
 *       meta o por fin de run.
 *   <li>{@link grid.VisualizadorGridCli}: Vista por consola (CLI) con renderizado ASCII dinámico y
 *       autoescalable.
 *   <li>{@link grid.PoliticaGridV2}: Política reactiva optimizada (activa en AgenteGrid) con
 *       navegación por eje dominante, inercia direccional, despegue coherente por lado libre y
 *       detección de ciclos.
 *   <li>{@link grid.Direccion}: Direcciones cardinales (NORTE, ESTE, SUR, OESTE) y acciones de
 *       movimiento.
 *   <li>{@link grid.Posicion}: Coordenadas (x, y) inmutables y cálculo de heurística de distancia
 *       Manhattan.
 *   <li>{@link grid.SensoresGrid}: Percepciones del agente con posición, objetivo, bumpers y
 *       energía.
 * </ul>
 */
package grid;
