/**
 * Bloque Grid (mundo bidimensional) de la Práctica 2.
 *
 * <p>Reutiliza el paquete {@code base}: implementa {@code Entorno<SensoresGrid, AccionGrid>} y
 * {@code Politica<SensoresGrid, AccionGrid>} y se ejecuta con {@code base.Agente} y
 * {@code base.BucleAgentico}, igual que el termostato.
 *
 * <p>Clases previstas (ver README, sección «Parte 2: Grid»):
 * <ul>
 *   <li>{@code SensoresGrid}: posición, objetivo, bumpers y energía restante.</li>
 *   <li>{@code AccionGrid}: enum con los cuatro códigos {@code grid.move.*}.</li>
 *   <li>{@code EntornoGrid}: descubre el reto {@code GRID_2D}, inicia el run, lee los cuatro
 *       sensores y envía movimientos.</li>
 *   <li>{@code MapaCreencias}: memoria del agente (obstáculos detectados, celdas visitadas).</li>
 *   <li>{@code PlanificadorAEstrella}: A* sobre el mapa de creencias, replanificando cuando
 *       aparece un obstáculo nuevo.</li>
 *   <li>{@code PoliticaGrid}: une mapa, planificador, comprobación de seguridad y energía.</li>
 * </ul>
 */
package grid;
