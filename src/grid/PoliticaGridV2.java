package grid;

import base.Politica;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Politica de navegacion para el entorno Grid basada en Bug0 con progreso,
 * inercia direccional y eleccion informada de mano.
 *
 * Implementa una maquina de dos modos de navegacion:
 * - ACERCAMIENTO: avanza hacia la meta siguiendo el eje de mayor distancia (eje dominante).
 *   En caso de empate diagonal, aplica inercia direccional para evitar zigzags.
 * - SEGUIMIENTO: al topar con un obstaculo, elige la mano con menor coste hacia el objetivo
 *   y rodea el contorno hasta registrar progreso estricto respecto al punto de impacto.
 */
public class PoliticaGridV2 implements Politica<SensoresGrid, Direccion> {

    public enum Modo {
        ACERCAMIENTO,
        SEGUIMIENTO
    }

    public enum Mano {
        IZQUIERDA,
        DERECHA
    }

    /**
     * Registro de estado durante el seguimiento de obstaculo para la deteccion de ciclos.
     */
    private record EstadoSeguimiento(
            Posicion posicion,
            Direccion orientacion,
            Mano mano,
            int distanciaImpacto,
            Set<Direccion> bloqueadas) {
    }

    /**
     * Senal de terminacion controlada cuando la navegacion no puede continuar.
     */
    public static final class ParadaNavegacion extends IllegalStateException {
        public ParadaNavegacion(String motivo) {
            super(motivo);
        }
    }

    private static final String MODO_ENERGIA_FINITA = "FINITE";

    private Modo modo = Modo.ACERCAMIENTO;
    private Optional<Direccion> orientacion = Optional.empty();
    private Mano mano = Mano.DERECHA;
    private int distanciaImpacto;
    private final Set<EstadoSeguimiento> seguimientosVisitados = new HashSet<>();
    private boolean cambioDeManoIntentado;

    /**
     * Determina la siguiente direccion de desplazamiento segun la percepcion actual.
     *
     * Precondiciones:
     * - Los sensores no deben ser nulos.
     * - El agente no debe haber alcanzado el objetivo previamente.
     * - Debe disponer de energia restante si el modo es finito.
     *
     * @param sensores percepciones actuales del agente en la cuadricula
     * @return direccion cardinal seleccionada para el proximo movimiento
     * @throws NullPointerException si sensores es nulo
     * @throws IllegalStateException si el agente ya esta en el objetivo o se elige una casilla bloqueada
     * @throws ParadaNavegacion si se agota la energia o se detecta un ciclo irresoluble
     */
    @Override
    public Direccion aplicar(SensoresGrid sensores) {
        Objects.requireNonNull(sensores, "Los sensores de Grid no pueden ser nulos");

        if (sensores.enObjetivo()) {
            throw new IllegalStateException("El agente ya esta en el objetivo " + sensores.objetivo());
        }

        if (MODO_ENERGIA_FINITA.equals(sensores.modoEnergia()) && sensores.energiaRestante() <= 0) {
            throw new ParadaNavegacion("Energia finita agotada en " + sensores.posicion()
                    + " (restante: " + sensores.energiaRestante() + ")");
        }

        if (orientacion.isEmpty()) {
            orientacion = Optional.of(direccionPrimaria(sensores.posicion(), sensores.objetivo()));
        }

        Direccion direccion;
        if (modo == Modo.SEGUIMIENTO) {
            if (debeSalir(sensores)) {
                modo = Modo.ACERCAMIENTO;
                cambioDeManoIntentado = false;
                direccion = direccionLadoDespegue();
            } else {
                direccion = seguirMuroConMemoria(sensores);
            }
        } else {
            direccion = acercarse(sensores);
        }

        if (sensores.bloqueado(direccion)) {
            throw new IllegalStateException(
                    "La politica eligio " + direccion + " estando bloqueada en " + sensores.posicion());
        }

        orientacion = Optional.of(direccion);
        return direccion;
    }

    /**
     * Restablece el estado interno a sus valores iniciales para permitir
     * la reutilizacion segura de la instancia en nuevas ejecuciones.
     */
    public void reiniciar() {
        modo = Modo.ACERCAMIENTO;
        orientacion = Optional.empty();
        mano = Mano.DERECHA;
        distanciaImpacto = 0;
        seguimientosVisitados.clear();
        cambioDeManoIntentado = false;
    }

    // --- Modo ACERCAMIENTO ---

    /**
     * Gestiona la navegacion en modo ACERCAMIENTO avanzando por el eje dominante.
     * Si la direccion principal se encuentra obstaculizada, conmuta a SEGUIMIENTO.
     */
    private Direccion acercarse(SensoresGrid sensores) {
        Direccion primaria = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        if (sensores.libre(primaria)) {
            return primaria;
        }
        return iniciarSeguimiento(sensores);
    }

    /**
     * Inicializa el rodeo del obstaculo seleccionando la alternativa lateral libre
     * mas proxima al objetivo y configurando la mano de seguimiento adecuada.
     */
    private Direccion iniciarSeguimiento(SensoresGrid sensores) {
        Direccion intencion = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        Direccion giroIzquierda = intencion.izquierda();
        Direccion giroDerecha = intencion.derecha();

        Direccion elegida;
        if (sensores.libre(giroIzquierda) && sensores.libre(giroDerecha)) {
            int distIzquierda = distanciaObjetivoTrasMover(sensores, giroIzquierda);
            int distDerecha = distanciaObjetivoTrasMover(sensores, giroDerecha);
            if (distDerecha < distIzquierda) {
                elegida = giroDerecha;
            } else if (distIzquierda < distDerecha) {
                elegida = giroIzquierda;
            } else {
                // Conserva coherencia con la mano previa en caso de empate simetrico
                elegida = (mano == Mano.IZQUIERDA) ? giroDerecha : giroIzquierda;
            }
        } else if (sensores.libre(giroIzquierda)) {
            elegida = giroIzquierda;
        } else if (sensores.libre(giroDerecha)) {
            elegida = giroDerecha;
        } else {
            elegida = intencion.retroceso();
        }

        modo = Modo.SEGUIMIENTO;
        mano = (elegida == giroDerecha) ? Mano.IZQUIERDA : Mano.DERECHA;
        distanciaImpacto = sensores.distanciaAlObjetivo();
        cambioDeManoIntentado = false;
        return elegida;
    }

    // --- Modo SEGUIMIENTO ---

    /**
     * Aplica el seguimiento de contorno registrando el estado visitado para
     * abortar o intentar la mano contraria ante la aparicion de ciclos.
     */
    private Direccion seguirMuroConMemoria(SensoresGrid sensores) {
        EstadoSeguimiento estado = new EstadoSeguimiento(
                sensores.posicion(), orientacion(), mano, distanciaImpacto, sensores.bloqueadas());

        if (!seguimientosVisitados.add(estado)) {
            if (cambioDeManoIntentado) {
                throw new ParadaNavegacion("Ciclo de navegacion tras probar el cambio de mano en "
                        + sensores.posicion());
            }
            cambioDeManoIntentado = true;
            mano = (mano == Mano.DERECHA) ? Mano.IZQUIERDA : Mano.DERECHA;
            seguimientosVisitados.clear();

            Direccion regreso = orientacion().retroceso();
            if (sensores.libre(regreso)) {
                return regreso;
            }
        }

        return seguirMuro(sensores);
    }

    /**
     * Selecciona la proxima direccion cardinal siguiendo la regla de la mano respectiva.
     * Evalua secuencialmente las alternativas evitando instanciar arrays en cada paso.
     */
    private Direccion seguirMuro(SensoresGrid sensores) {
        Direccion o = orientacion();
        Direccion primera = (mano == Mano.DERECHA) ? o.derecha() : o.izquierda();
        Direccion tercera = (mano == Mano.DERECHA) ? o.izquierda() : o.derecha();

        if (sensores.libre(primera)) {
            return primera;
        }
        if (sensores.libre(o)) {
            return o;
        }
        if (sensores.libre(tercera)) {
            return tercera;
        }
        if (sensores.libre(o.retroceso())) {
            return o.retroceso();
        }

        throw new ParadaNavegacion("Todas las direcciones adyacentes estan bloqueadas en "
                + sensores.posicion());
    }

    /**
     * Evalua si se satisfacen las tres condiciones de despegue Bug0:
     * 1. Progreso estricto: distancia actual menor que la de impacto inicial (h < d0).
     * 2. Lado de despegue despejado: la celda opuesta a la mano no presenta obstaculo.
     * 3. Proyeccion favorable: el vector de despegue apunta hacia el semiplano del objetivo.
     */
    private boolean debeSalir(SensoresGrid sensores) {
        if (sensores.distanciaAlObjetivo() >= distanciaImpacto) {
            return false;
        }

        Direccion salida = direccionLadoDespegue();
        if (sensores.bloqueado(salida)) {
            return false;
        }

        return apuntaHaciaSemiplanoObjetivo(sensores.posicion(), sensores.objetivo(), salida);
    }

    /**
     * Devuelve la direccion perpendicular opuesta a la mano que contacta con el muro.
     */
    private Direccion direccionLadoDespegue() {
        return (mano == Mano.DERECHA) ? orientacion().izquierda() : orientacion().derecha();
    }

    // --- Heuristica y Calculo Vectorial ---

    /**
     * Devuelve la orientacion actual del agente o falla si no ha sido inicializada.
     */
    private Direccion orientacion() {
        return orientacion.orElseThrow(() -> new IllegalStateException("Orientacion no inicializada"));
    }

    /**
     * Verifica si una direccion cardinal tiene proyeccion positiva hacia el objetivo
     * mediante el producto escalar entre el desplazamiento y el vector objetivo.
     */
    private boolean apuntaHaciaSemiplanoObjetivo(Posicion origen, Posicion destino, Direccion direccion) {
        int dx = destino.x() - origen.x();
        int dy = destino.y() - origen.y();
        return (dx * direccion.dx() + dy * direccion.dy()) > 0;
    }

    /**
     * Calcula la distancia Manhattan resultante tras desplazarse una celda en una direccion.
     */
    private int distanciaObjetivoTrasMover(SensoresGrid sensores, Direccion direccion) {
        return sensores.posicion().mover(direccion).distanciaManhattan(sensores.objetivo());
    }

    /**
     * Determina la direccion hacia el objetivo segun el eje dominante:
     * - Si domina la separacion horizontal (|dx| > |dy|), orienta hacia ESTE u OESTE.
     * - Si domina la separacion vertical (|dy| > |dx|), orienta hacia SUR o NORTE.
     * - En empate diagonal (|dx| == |dy|), aplica inercia direccional para minimizar zigzags.
     */
    private Direccion direccionPrimaria(Posicion pos, Posicion objetivo) {
        int dx = objetivo.x() - pos.x();
        int dy = objetivo.y() - pos.y();
        Direccion direccion;

        if (Math.abs(dx) > Math.abs(dy)) {
            direccion = (dx > 0) ? Direccion.ESTE : Direccion.OESTE;
        } else if (Math.abs(dy) > Math.abs(dx)) {
            direccion = (dy > 0) ? Direccion.SUR : Direccion.NORTE;
        } else {
            Direccion dirHorizontal = (dx > 0) ? Direccion.ESTE : Direccion.OESTE;
            Direccion dirVertical = (dy > 0) ? Direccion.SUR : Direccion.NORTE;

            if (orientacion.isPresent() && orientacion.get() == dirVertical) {
                direccion = dirVertical;
            } else if (orientacion.isPresent() && orientacion.get() == dirHorizontal) {
                direccion = dirHorizontal;
            } else {
                direccion = dirHorizontal;
            }
        }

        return direccion;
    }
}
