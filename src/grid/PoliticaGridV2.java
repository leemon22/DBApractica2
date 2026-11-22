package grid;

import base.Politica;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Política de navegación para el entorno Grid basada en Bug0 con progreso y elección dinámica de mano.
 * <p>
 * Implementa una máquina de dos modos:
 * <ul>
 *   <li><b>ACERCAMIENTO:</b> avanza hacia la primera celda libre (en orden N, E, S, O) que reduce la distancia Manhattan.</li>
 *   <li><b>SEGUIMIENTO:</b> si el camino óptimo está bloqueado, rodea el obstáculo siguiendo el muro hasta cumplir la condición de salida.</li>
 * </ul>
 */
public class PoliticaGridV2 implements Politica<SensoresGrid, Direccion> {

    private enum Modo {
        ACERCAMIENTO,
        SEGUIMIENTO
    }

    private enum Mano {
        IZQUIERDA,
        DERECHA
    }

    // El modo es SEGUIMIENTO en todos los estados registrados.
    private record EstadoSeguimiento(
            Posicion posicion, Direccion orientacion, Mano mano, int d0, Set<Direccion> bloqueadas) {
    }

    static final class ParadaNavegacion extends IllegalStateException {
        ParadaNavegacion(String motivo) {
            super(motivo);
        }
    }

    private Modo modo = Modo.ACERCAMIENTO;
    // Dirección del último movimiento realizado o dirección inicial hacia el objetivo.
    private Optional<Direccion> orientacion = Optional.empty();
    // Mano con la que se rodea el obstáculo actual. Solo tiene sentido en SEGUIMIENTO.
    private Mano mano = Mano.DERECHA;
    // Distancia al objetivo al empezar el rodeo actual.
    private int d0;
    private final Set<EstadoSeguimiento> seguimientosVisitados = new HashSet<>();
    private boolean cambioDeManoIntentado;

    @Override
    public Direccion aplicar(SensoresGrid sensores) {
        Objects.requireNonNull(sensores, "Los sensores de Grid no pueden ser nulos");

        if (sensores.enObjetivo()) {
            throw new IllegalStateException("El agente ya está en el objetivo " + sensores.objetivo());
        }

        if ("FINITE".equals(sensores.modoEnergia()) && sensores.energiaRestante() <= 0) {
            throw new ParadaNavegacion("Energía finita agotada en " + sensores.posicion()
                    + " (restante: " + sensores.energiaRestante() + ")");
        }

        // En el primer ciclo se orienta al agente hacia la meta según el eje dominante
        if (orientacion.isEmpty()) {
            orientacion = Optional.of(direccionPrimaria(sensores.posicion(), sensores.objetivo()));
        }

        if (modo == Modo.SEGUIMIENTO && debeSalir(sensores)) {
            modo = Modo.ACERCAMIENTO;
        }

        Direccion direccion = (modo == Modo.ACERCAMIENTO)
                ? acercarse(sensores)
                : seguirMuroConMemoria(sensores);

        // Salvaguarda: moverse hacia un bumper a true termina el run, así que nunca se envía.
        if (sensores.bloqueado(direccion)) {
            throw new IllegalStateException(
                    "La política eligió " + direccion + " estando bloqueada en " + sensores.posicion());
        }

        orientacion = Optional.of(direccion);
        return direccion;
    }

    // --- ACERCAMIENTO ---

    /**
     * Modo ACERCAMIENTO: avanza hacia la meta siguiendo el eje dominante.
     * Si la dirección primaria está bloqueada, detecta el obstáculo e inicia el seguimiento.
     */
    private Direccion acercarse(SensoresGrid sensores) {
        Direccion primaria = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        if (sensores.libre(primaria)) {
            return primaria;
        }
        return iniciarSeguimiento(sensores);
    }

    // Elección dinámica de mano: de los dos giros laterales respecto a la intención bloqueada,
    // se elige el libre cuya celda queda más cerca del objetivo. En empate, mano derecha (girar a
    // la izquierda), que es la preferencia por defecto de la heurística.
    private Direccion iniciarSeguimiento(SensoresGrid sensores) {
        Direccion intencion = mejorMovimiento(sensores);
        Direccion giroIzquierda = intencion.izquierda(); // deja el muro a la derecha
        Direccion giroDerecha = intencion.derecha();     // deja el muro a la izquierda

        Direccion elegida;
        if (sensores.libre(giroIzquierda) && sensores.libre(giroDerecha)) {
            int hIzquierda = distanciaTras(sensores, giroIzquierda);
            int hDerecha = distanciaTras(sensores, giroDerecha);
            elegida = (hDerecha < hIzquierda) ? giroDerecha : giroIzquierda;
        } else if (sensores.libre(giroIzquierda)) {
            elegida = giroIzquierda;
        } else if (sensores.libre(giroDerecha)) {
            elegida = giroDerecha;
        } else {
            // Callejón sin salida: solo queda volver atrás. Se rodea con la mano por defecto.
            elegida = intencion.retroceso();
        }

        mano = (elegida == giroDerecha) ? Mano.IZQUIERDA : Mano.DERECHA;
        d0 = sensores.distanciaAlObjetivo();
        modo = Modo.SEGUIMIENTO;
        return elegida;
    }

    // --- SEGUIMIENTO ---

    private Direccion seguirMuroConMemoria(SensoresGrid sensores) {
        EstadoSeguimiento estado = new EstadoSeguimiento(
                sensores.posicion(), orientacion(), mano, d0, sensores.bloqueadas());
        // Conservar la memoria entre rodeos también detecta ciclos que alternan ambos modos.
        if (!seguimientosVisitados.add(estado)) {
            if (cambioDeManoIntentado) {
                throw new ParadaNavegacion("Ciclo de navegación tras probar el cambio de mano en "
                        + sensores.posicion());
            }
            cambioDeManoIntentado = true;
            mano = (mano == Mano.DERECHA) ? Mano.IZQUIERDA : Mano.DERECHA;
            // Los estados anteriores al cambio no demuestran un ciclo en este nuevo intento.
            seguimientosVisitados.clear();
            // Desandar el último paso permite recorrer el contorno en sentido contrario.
            Direccion regreso = orientacion().retroceso();
            if (sensores.libre(regreso)) {
                return regreso;
            }
        }
        return seguirMuro(sensores);
    }

    // Regla de la mano: con mano derecha, prioridad derecha, frente, izquierda y retroceso;
    // con mano izquierda, izquierda, frente, derecha y retroceso.
    private Direccion seguirMuro(SensoresGrid sensores) {
        Direccion o = orientacion();
        Direccion[] prioridad = (mano == Mano.DERECHA)
                ? new Direccion[]{o.derecha(), o, o.izquierda(), o.retroceso()}
                : new Direccion[]{o.izquierda(), o, o.derecha(), o.retroceso()};

        for (Direccion d : prioridad) {
            if (sensores.libre(d)) {
                return d;
            }
        }
        throw new IllegalStateException("Las cuatro direcciones están bloqueadas en " + sensores.posicion());
    }

    // Condición de salida (criterio lateral del algoritmo Bug0 con progreso):
    // 1. Progreso: más cerca del objetivo que al empezar el rodeo actual (h < d0).
    // 2. El mejor movimiento de acercamiento hacia el objetivo está libre.
    // 3. El objetivo queda en el lado libre, el contrario a la mano que toca el muro.
    private boolean debeSalir(SensoresGrid sensores) {
        if (sensores.distanciaAlObjetivo() >= d0) {
            return false;
        }
        if (sensores.bloqueado(mejorMovimiento(sensores))) {
            return false;
        }
        Direccion o = orientacion();
        Direccion ladoLibre = (mano == Mano.DERECHA) ? o.izquierda() : o.derecha();
        int haciaObjetivoX = sensores.objetivo().x() - sensores.posicion().x();
        int haciaObjetivoY = sensores.objetivo().y() - sensores.posicion().y();
        return haciaObjetivoX * ladoLibre.dx() + haciaObjetivoY * ladoLibre.dy() > 0;
    }

    // --- Utilidades ---

    /**
     * Devuelve la dirección hacia la meta sobre el eje dominante, esté libre o no.
     */
    private Direccion mejorMovimiento(SensoresGrid sensores) {
        return direccionPrimaria(sensores.posicion(), sensores.objetivo());
    }

    private int distanciaTras(SensoresGrid sensores, Direccion d) {
        return sensores.posicion().mover(d).distanciaManhattan(sensores.objetivo());
    }

    /**
     * Devuelve la orientación actual del agente (último movimiento o dirección inicial).
     * Falla explícitamente si se consulta antes de la inicialización.
     */
    private Direccion orientacion() {
        return orientacion.orElseThrow(() -> new IllegalStateException("Orientación no inicializada"));
    }

    /**
     * Calcula la dirección hacia la meta según el eje con mayor distancia (eje dominante):
     * - Si domina la separación horizontal (|dx| >= |dy|), se orienta hacia ESTE u OESTE.
     * - Si domina la separación vertical (|dy| > |dx|), se orienta hacia SUR o NORTE.
     * En caso de empate diagonal, se prioriza el eje horizontal.
     */
    private Direccion direccionPrimaria(Posicion pos, Posicion objetivo) {
        int dx = objetivo.x() - pos.x();
        int dy = objetivo.y() - pos.y();
        Direccion direccion;

        if (Math.abs(dx) >= Math.abs(dy)) {
            if (dx > 0) {
                direccion = Direccion.ESTE;
            } else if (dx < 0) {
                direccion = Direccion.OESTE;
            } else if (dy > 0) {
                direccion = Direccion.SUR;
            } else {
                direccion = Direccion.NORTE;
            }
        } else {
            if (dy > 0) {
                direccion = Direccion.SUR;
            } else if (dy < 0) {
                direccion = Direccion.NORTE;
            } else if (dx > 0) {
                direccion = Direccion.ESTE;
            } else {
                direccion = Direccion.OESTE;
            }
        }

        return direccion;
    }
}
