/**
 * Bloque Grid (mundo bidimensional) de la Práctica 2.
 *
 * <p>Reutiliza el paquete {@code base}: implementa {@code Entorno<SensoresGrid, Direccion>} y
 * {@code Politica<SensoresGrid, Direccion>} y se ejecuta con {@code base.Agente} y
 * {@code base.BucleAgentico}, igual que el termostato. La acción es la propia
 * {@link grid.Direccion}: cada movimiento es una dirección cardinal.
 *
 * <p>Algoritmo: seguimiento de muros (Bug0 con progreso y elección dinámica de mano, Tema 2).
 * Ver README, sección «Parte 2: Grid», y context.md, sección 7.
 *
 * <ul>
 *   <li>{@link grid.Direccion}: N/E/S/O con desplazamiento, giros relativos y códigos de la API.</li>
 *   <li>{@link grid.Posicion}: celda (x, y) y distancia Manhattan.</li>
 *   <li>{@link grid.SensoresGrid}: posición, objetivo, direcciones bloqueadas y energía.</li>
 *   <li>{@code EntornoGrid} (pendiente): descubre el reto {@code GRID_2D}, inicia el run, lee los
 *       sensores y envía movimientos.</li>
 *   <li>{@code PoliticaGrid} (pendiente): máquina de estados ACERCAMIENTO / SEGUIMIENTO.</li>
 *   <li>{@code SimuladorGrid} (pendiente): entorno local con los mapas del guion.</li>
 * </ul>
 */
package grid;
