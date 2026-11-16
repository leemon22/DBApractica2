/**
 * Paquete del bloque Grid (entorno bidimensional y navegación basada en mapa).
 * <p>
 * Reutiliza el marco de trabajo del paquete {@code base}:
 * las percepciones extienden {@link base.Sensor} (mediante {@link grid.SensoresGrid}),
 * y las acciones implementan {@link base.Accion} (mediante {@link grid.Direccion}).
 * <p>
 * Componentes principales del bloque:
 * <ul>
 *   <li>{@link grid.Direccion}: Direcciones cardinales (NORTE, ESTE, SUR, OESTE) con cálculo de giros y desplazamientos.</li>
 *   <li>{@link grid.Posicion}: Coordenadas (x, y) inmutables y cálculo de heurística de distancia Manhattan.</li>
 *   <li>{@link grid.SensoresGrid}: Percepciones del agente con posición, objetivo, bumpers y energía.</li>
 *   <li>{@link grid.PoliticaGrid}: Política de navegación reactiva Bug0 con progreso y elección dinámica de mano.</li>
 * </ul>
 */
package grid;
