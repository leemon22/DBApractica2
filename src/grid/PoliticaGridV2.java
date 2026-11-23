package grid;

import base.Politica;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Politica de navegacion reactiva para Grid basada en Bug0 con progreso,
 * eje dominante e inercia direccional.
 *
 * El agente no dispone de un mapa previo del entorno y decide unicamente a partir
 * de sus sensores locales (bumpers) y la posicion relativa del objetivo.
 *
 * La politica gestiona dos modos:
 * - ACERCAMIENTO: avanza de forma voraz hacia la meta reduciendo la distancia Manhattan.
 *   Prioriza el eje de mayor separacion (eje dominante) y mantiene el rumbo en empates
 *   diagonales (inercia direccional) para evitar zigzags.
 * - SEGUIMIENTO: ante un obstaculo frontal, bordea su perimetro aplicando la regla de la mano
 *   (izquierda o derecha) hasta registrar un progreso neto respecto a la distancia de impacto
 *   y comprobar que la salida apunta hacia el semiplano del objetivo.
 */
public class PoliticaGridV2 implements Politica<SensoresGrid, Direccion> {

    /**
     * Modos de operacion de la maquina de estados de navegacion.
     */
    public enum Modo {
        ACERCAMIENTO,
        SEGUIMIENTO
    }

    /**
     * Mano de contacto con el obstaculo durante el seguimiento perimetral.
     * Define las prioridades relativas de giro del agente.
     */
    public enum Mano {
        IZQUIERDA,
        DERECHA
    }

    /**
     * Instantanea inmutable del estado del agente durante el seguimiento.
     * Permite detectar en tiempo constante O(1) si el agente repite un patron
     * de navegacion identico en un ciclo cerrado.
     */
    private record EstadoSeguimiento(
            Posicion posicion,
            Direccion orientacion,
            Mano mano,
            int distanciaImpacto,
            Set<Direccion> bloqueadas) {
    }

    /**
     * Excepcion que interrumpe la navegacion de forma controlada cuando el agente
     * no puede avanzar (agotamiento de energia finita, callejon sin salida o ciclo irresoluble).
     */
    public static final class ParadaNavegacion extends IllegalStateException {
        public ParadaNavegacion(String motivo) {
            super(motivo);
        }
    }

    private static final String MODO_ENERGIA_FINITA = "FINITE";

    private Modo modo = Modo.ACERCAMIENTO;

    /** Direccion del ultimo movimiento aplicado. Determina los giros relativos en seguimiento. */
    private Direccion orientacion;

    /** Mano activa para bordear el obstaculo actual. */
    private Mano mano = Mano.DERECHA;

    /** Distancia Manhattan al objetivo al colisionar con el obstaculo en curso (d0). */
    private int distanciaImpacto;

    /** Registro de estados visitados para detectar bucles durante el seguimiento. */
    private final Set<EstadoSeguimiento> seguimientosVisitados = new HashSet<>();

    /** Indica si ya se probo a invertir la mano para romper un ciclo detectado. */
    private boolean cambioDeManoIntentado;

    /**
     * Determina el siguiente movimiento del agente a partir de las percepciones recibidas.
     *
     * @param sensores percepciones actuales del agente (posicion, objetivo, bumpers y energia)
     * @return direccion cardinal seleccionada para el desplazamiento
     * @throws NullPointerException si sensores es nulo
     * @throws IllegalStateException si el agente ya esta en el objetivo o se elige una casilla bloqueada
     * @throws ParadaNavegacion si se agota la energia finita o se detecta un ciclo irresoluble
     */
    @Override
    public Direccion aplicar(SensoresGrid sensores) {
        Objects.requireNonNull(sensores, "Los sensores de Grid no pueden ser nulos");

        if (sensores.enObjetivo()) {
            throw new IllegalStateException("El agente ya esta en el objetivo " + sensores.objetivo());
        }

        // Si el entorno limita la energia y esta se agota, abortamos para evitar un fallo en el servidor
        if (MODO_ENERGIA_FINITA.equals(sensores.modoEnergia()) && sensores.energiaRestante() <= 0) {
            throw new ParadaNavegacion("Energia finita agotada en " + sensores.posicion()
                    + " (restante: " + sensores.energiaRestante() + ")");
        }

        // En el primer paso, orienta al agente hacia el objetivo segun el eje dominante
        if (orientacion == null) {
            orientacion = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        }

        Direccion direccion;
        if (modo == Modo.SEGUIMIENTO) {
            if (debeSalir(sensores)) {
                // El agente ha progresado y puede desengancharse del muro saliendo hacia el lado libre
                modo = Modo.ACERCAMIENTO;
                cambioDeManoIntentado = false;
                direccion = direccionLadoDespegue();
            } else {
                direccion = seguirMuroConMemoria(sensores);
            }
        } else {
            direccion = acercarse(sensores);
        }

        // Salvaguarda: chocar contra un obstaculo o salirse del mapa cancela el reto en el servidor
        if (sensores.bloqueado(direccion)) {
            throw new IllegalStateException(
                    "La politica eligio " + direccion + " estando bloqueada en " + sensores.posicion());
        }

        this.orientacion = direccion;
        return direccion;
    }

    /**
     * Restablece el estado interno de la politica a sus valores iniciales.
     * Permite reutilizar la misma instancia en sucesivas ejecuciones o pruebas.
     */
    public void reiniciar() {
        this.modo = Modo.ACERCAMIENTO;
        this.orientacion = null;
        this.mano = Mano.DERECHA;
        this.distanciaImpacto = 0;
        this.seguimientosVisitados.clear();
        this.cambioDeManoIntentado = false;
    }

    // --- Metodos de consulta de estado ---

    /**
     * Devuelve el modo operativo actual (ACERCAMIENTO o SEGUIMIENTO).
     *
     * @return modo actual
     */
    public Modo getModo() {
        return modo;
    }

    /**
     * Devuelve la mano asignada actualmente para el seguimiento perimetral.
     *
     * @return mano activa
     */
    public Mano getMano() {
        return mano;
    }

    /**
     * Devuelve la orientacion actual del agente si ya ha sido inicializada.
     *
     * @return orientacion actual envuelta en Optional, o Optional.empty() si no se ha iniciado
     */
    public Optional<Direccion> getOrientacion() {
        return Optional.ofNullable(orientacion);
    }

    /**
     * Devuelve la distancia al objetivo registrada al chocar con el obstaculo actual (d0).
     *
     * @return distancia de impacto
     */
    public int getDistanciaImpacto() {
        return distanciaImpacto;
    }

    /**
     * Indica si ya se ha probado a cambiar la mano de seguimiento ante un ciclo.
     *
     * @return true si ya se intento el cambio de mano
     */
    public boolean isCambioDeManoIntentado() {
        return cambioDeManoIntentado;
    }

    // --- Subrutinas de navegacion en modo ACERCAMIENTO ---

    /**
     * Gestiona el avance voraz hacia el objetivo en modo ACERCAMIENTO.
     * Si la direccion primaria esta bloqueada por un obstaculo, inicia el modo SEGUIMIENTO.
     */
    private Direccion acercarse(SensoresGrid sensores) {
        Direccion primaria = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        if (sensores.libre(primaria)) {
            return primaria;
        }
        return iniciarSeguimiento(sensores);
    }

    /**
     * Inicia el rodeo del obstaculo eligiendo la alternativa lateral libre mas favorable hacia la meta.
     * Asigna la mano de seguimiento de forma consistente con el giro: girar a la derecha deja el obstaculo
     * a la izquierda (Mano.IZQUIERDA) y viceversa.
     */
    private Direccion iniciarSeguimiento(SensoresGrid sensores) {
        Direccion intencion = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        Direccion giroIzquierda = intencion.izquierda();
        Direccion giroDerecha = intencion.derecha();

        Direccion elegida;
        // Compara que alternativa lateral libre deja al agente mas cerca del objetivo tras el giro
        if (sensores.libre(giroIzquierda) && sensores.libre(giroDerecha)) {
            int distIzquierda = distanciaObjetivoTrasMover(sensores, giroIzquierda);
            int distDerecha = distanciaObjetivoTrasMover(sensores, giroDerecha);
            if (distDerecha < distIzquierda) {
                elegida = giroDerecha;
            } else if (distIzquierda < distDerecha) {
                elegida = giroIzquierda;
            } else {
                // En empate simetrico se conserva la mano previa para evitar oscilaciones erraticas
                elegida = (mano == Mano.IZQUIERDA) ? giroDerecha : giroIzquierda;
            }
        } else if (sensores.libre(giroIzquierda)) {
            elegida = giroIzquierda;
        } else if (sensores.libre(giroDerecha)) {
            elegida = giroDerecha;
        } else {
            // Callejon sin salida: la unica alternativa transitable es dar media vuelta
            elegida = intencion.retroceso();
        }

        this.modo = Modo.SEGUIMIENTO;
        // Si gira a la derecha, el obstaculo queda a la izquierda (mano IZQUIERDA), y viceversa
        this.mano = (elegida == giroDerecha) ? Mano.IZQUIERDA : Mano.DERECHA;
        this.distanciaImpacto = sensores.distanciaAlObjetivo();
        this.cambioDeManoIntentado = false;
        return elegida;
    }

    // --- Subrutinas de navegacion en modo SEGUIMIENTO ---

    /**
     * Aplica el seguimiento perimetral comprobando si el estado ya ha sido visitado previamente.
     * Si se detecta un ciclo, invierte la mano y retrocede para intentar salir por el otro sentido.
     * Si el ciclo persiste tras el cambio, aborta la navegacion.
     */
    private Direccion seguirMuroConMemoria(SensoresGrid sensores) {
        EstadoSeguimiento estado = new EstadoSeguimiento(
                sensores.posicion(), orientacionActual(), mano, distanciaImpacto, sensores.bloqueadas());

        // Set.add devuelve false si ya habiamos pasado por esta casilla con el mismo rumbo y mano
        if (!seguimientosVisitados.add(estado)) {
            if (cambioDeManoIntentado) {
                throw new ParadaNavegacion("Ciclo de navegacion tras probar el cambio de mano en "
                        + sensores.posicion());
            }
            // Ante el primer bucle detectado, invierte la mano y retrocede para intentar salir por el otro sentido
            cambioDeManoIntentado = true;
            this.mano = (mano == Mano.DERECHA) ? Mano.IZQUIERDA : Mano.DERECHA;
            this.seguimientosVisitados.clear();

            Direccion regreso = orientacionActual().retroceso();
            if (sensores.libre(regreso)) {
                return regreso;
            }
        }

        return seguirMuro(sensores);
    }

    /**
     * Aplica la regla de seguimiento de la mano asignada sin instanciar colecciones en memoria.
     * Con mano DERECHA la prioridad es: derecha, frente, izquierda y retroceso.
     * Con mano IZQUIERDA la prioridad es: izquierda, frente, derecha y retroceso.
     */
    private Direccion seguirMuro(SensoresGrid sensores) {
        Direccion o = orientacionActual();
        // La regla de la mano intenta primero rodear esquinas del muro hacia su lado antes de seguir recto
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
     * Evalua si se cumplen las condiciones de despegue de Bug0:
     * 1. Progreso estricto: la distancia actual al objetivo es menor que la inicial del rodeo (h < d0).
     * 2. Lado libre: la celda perpendicular opuesta al muro esta despejada.
     * 3. Semiplano favorable: la direccion de salida proyecta positivamente hacia el objetivo.
     */
    private boolean debeSalir(SensoresGrid sensores) {
        // 1. Debe existir progreso estricto respecto a la distancia al objetivo cuando comenzo el rodeo
        if (sensores.distanciaAlObjetivo() >= distanciaImpacto) {
            return false;
        }

        // 2. La casilla lateral libre contraria al muro debe estar transitable
        Direccion salida = direccionLadoDespegue();
        if (sensores.bloqueado(salida)) {
            return false;
        }

        // 3. El vector de salida debe apuntar hacia el semiplano del objetivo (no alejarse de el)
        return apuntaHaciaSemiplanoObjetivo(sensores.posicion(), sensores.objetivo(), salida);
    }

    /**
     * Devuelve la direccion perpendicular opuesta al muro con el que se mantiene contacto.
     */
    private Direccion direccionLadoDespegue() {
        return (mano == Mano.DERECHA) ? orientacionActual().izquierda() : orientacionActual().derecha();
    }

    // --- Calculo vectorial y utilidades geometricas ---

    /**
     * Devuelve la orientacion actual o lanza una excepcion si se consulta antes de tiempo.
     */
    private Direccion orientacionActual() {
        if (orientacion == null) {
            throw new IllegalStateException("Orientacion no inicializada");
        }
        return orientacion;
    }

    /**
     * Comprueba si una direccion proyecta positivamente hacia el objetivo calculando
     * el producto escalar entre el desplazamiento y el vector hacia la meta.
     * Un resultado mayor que cero confirma que el angulo es agudo (< 90 grados) y por
     * tanto la salida se adentra en el semiplano del objetivo.
     */
    private boolean apuntaHaciaSemiplanoObjetivo(Posicion origen, Posicion destino, Direccion direccion) {
        int dx = destino.x() - origen.x();
        int dy = destino.y() - origen.y();
        // Si el producto escalar es positivo, el angulo con el objetivo es menor de 90 grados
        return (dx * direccion.dx() + dy * direccion.dy()) > 0;
    }

    /**
     * Calcula la distancia Manhattan resultante de aplicar un movimiento hipotetico.
     */
    private int distanciaObjetivoTrasMover(SensoresGrid sensores, Direccion direccion) {
        return sensores.posicion().mover(direccion).distanciaManhattan(sensores.objetivo());
    }

    /**
     * Determina la direccion de avance preferente hacia el objetivo:
     * - Eje dominante: prioriza el eje con mayor separacion cartesiana (|dx| frente a |dy|).
     * - Inercia direccional: ante empate diagonal (|dx| == |dy|), mantiene la direccion
     *   previa del agente para evitar oscilaciones en zigzag.
     */
    private Direccion direccionPrimaria(Posicion pos, Posicion objetivo) {
        int dx = objetivo.x() - pos.x();
        int dy = objetivo.y() - pos.y();
        Direccion direccion;

        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);

        // Avanza por el eje donde la distancia a recorrer es mayor (eje dominante)
        if (absDx > absDy) {
            direccion = (dx > 0) ? Direccion.ESTE : Direccion.OESTE;
        } else if (absDy > absDx) {
            direccion = (dy > 0) ? Direccion.SUR : Direccion.NORTE;
        } else {
            // En empate diagonal perfecto (45 grados), mantiene el rumbo previo para no zigzaguear
            Direccion dirHorizontal = (dx > 0) ? Direccion.ESTE : Direccion.OESTE;
            Direccion dirVertical = (dy > 0) ? Direccion.SUR : Direccion.NORTE;

            if (orientacion != null && orientacion == dirVertical) {
                direccion = dirVertical;
            } else if (orientacion != null && orientacion == dirHorizontal) {
                direccion = dirHorizontal;
            } else {
                direccion = dirHorizontal;
            }
        }

        return direccion;
    }
}
