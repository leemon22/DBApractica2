package grid;

import base.Politica;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Bug0 con dos modos: acercamiento por eje dominante y seguimiento de muros. Decide solo con la
 * percepción actual; no conoce el mapa completo. Conserva rumbo, mano y estados visitados entre
 * pasos. reiniciar() empieza otro run.
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

    // Repetir posición no basta: el rumbo, la mano y la distancia de impacto también cuentan.
    private record EstadoSeguimiento(
            Posicion posicion,
            Direccion orientacion,
            Mano mano,
            int distanciaImpacto,
            Set<Direccion> bloqueadas) {}

    public static final class ParadaNavegacion extends IllegalStateException {
        public ParadaNavegacion(String motivo) {
            super(motivo);
        }
    }

    private static final String MODO_ENERGIA_FINITA = "FINITE";

    private Modo modo = Modo.ACERCAMIENTO;

    // Se inicializa en aplicar() antes de entrar en cualquier rutina de seguimiento.
    private Direccion orientacion;

    private Mano mano = Mano.DERECHA;

    // Distancia al empezar el rodeo: solo se abandona el muro si se ha mejorado.
    private int distanciaImpacto;

    private final Set<EstadoSeguimiento> seguimientosVisitados = new HashSet<>();

    // V2 permite un cambio por rodeo; V1 lo limita a toda la ejecución.
    private boolean cambioDeManoIntentado;

    @Override
    public Direccion aplicar(SensoresGrid sensores) {
        Objects.requireNonNull(sensores, "Los sensores de Grid no pueden ser nulos");

        if (sensores.enObjetivo()) {
            throw new IllegalStateException(
                    "El agente ya esta en el objetivo " + sensores.objetivo());
        }

        // Si el entorno limita la energia y esta se agota, abortamos para evitar un fallo en el
        // servidor
        if (MODO_ENERGIA_FINITA.equals(sensores.modoEnergia()) && sensores.energiaRestante() <= 0) {
            throw new ParadaNavegacion(
                    "Energia finita agotada en "
                            + sensores.posicion()
                            + " (restante: "
                            + sensores.energiaRestante()
                            + ")");
        }

        // En el primer paso, orienta al agente hacia el objetivo segun el eje dominante
        if (orientacion == null) {
            orientacion = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        }

        Direccion direccion;
        if (modo == Modo.SEGUIMIENTO) {
            if (debeSalir(sensores)) {
                // El agente ha progresado y puede desengancharse del muro saliendo hacia el lado
                // libre
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
                    "La politica eligio "
                            + direccion
                            + " estando bloqueada en "
                            + sensores.posicion());
        }

        this.orientacion = direccion;
        return direccion;
    }

    /** Borra toda la memoria al comenzar otra ejecución. */
    public void reiniciar() {
        this.modo = Modo.ACERCAMIENTO;
        this.orientacion = null;
        this.mano = Mano.DERECHA;
        this.distanciaImpacto = 0;
        this.seguimientosVisitados.clear();
        this.cambioDeManoIntentado = false;
    }

    public Modo getModo() {
        return modo;
    }

    public Mano getMano() {
        return mano;
    }

    public Optional<Direccion> getOrientacion() {
        return Optional.ofNullable(orientacion);
    }

    public int getDistanciaImpacto() {
        return distanciaImpacto;
    }

    public boolean isCambioDeManoIntentado() {
        return cambioDeManoIntentado;
    }

    private Direccion acercarse(SensoresGrid sensores) {
        Direccion primaria = direccionPrimaria(sensores.posicion(), sensores.objetivo());
        if (sensores.libre(primaria)) {
            return primaria;
        }
        return iniciarSeguimiento(sensores, primaria);
    }

    private Direccion iniciarSeguimiento(SensoresGrid sensores, Direccion intencion) {
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

    private Direccion seguirMuroConMemoria(SensoresGrid sensores) {
        EstadoSeguimiento estado =
                new EstadoSeguimiento(
                        sensores.posicion(),
                        orientacion,
                        mano,
                        distanciaImpacto,
                        sensores.bloqueadas());

        // Set.add devuelve false si ya habiamos pasado por esta casilla con el mismo rumbo y mano
        if (!seguimientosVisitados.add(estado)) {
            if (cambioDeManoIntentado) {
                throw new ParadaNavegacion(
                        "Ciclo de navegacion tras probar el cambio de mano en "
                                + sensores.posicion());
            }
            // Ante el primer bucle detectado, invierte la mano y retrocede para intentar salir por
            // el otro sentido
            cambioDeManoIntentado = true;
            this.mano = (mano == Mano.DERECHA) ? Mano.IZQUIERDA : Mano.DERECHA;
            this.seguimientosVisitados.clear();

            Direccion regreso = orientacion.retroceso();
            if (sensores.libre(regreso)) {
                return regreso;
            }
        }

        return seguirMuro(sensores);
    }

    /** Prioridad: lado del muro, frente, lado contrario y retroceso. */
    private Direccion seguirMuro(SensoresGrid sensores) {
        // La regla de la mano intenta primero rodear esquinas del muro hacia su lado antes de
        // seguir recto
        Direccion primera = (mano == Mano.DERECHA) ? orientacion.derecha() : orientacion.izquierda();
        Direccion tercera = (mano == Mano.DERECHA) ? orientacion.izquierda() : orientacion.derecha();

        if (sensores.libre(primera)) {
            return primera;
        } else if (sensores.libre(orientacion)) {
            return orientacion;
        } else if (sensores.libre(tercera)) {
            return tercera;
        } else if (sensores.libre(orientacion.retroceso())) {
            return orientacion.retroceso();
        }

        throw new ParadaNavegacion(
                "Todas las direcciones adyacentes estan bloqueadas en " + sensores.posicion());
    }

    /** Sale solo con progreso, lateral libre y producto escalar positivo hacia el objetivo. */
    private boolean debeSalir(SensoresGrid sensores) {
        // 1. Debe existir progreso estricto respecto a la distancia al objetivo cuando comenzo el
        // rodeo
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

    private Direccion direccionLadoDespegue() {
        return (mano == Mano.DERECHA) ? orientacion.izquierda() : orientacion.derecha();
    }

    private boolean apuntaHaciaSemiplanoObjetivo(
            Posicion origen, Posicion destino, Direccion direccion) {
        int dx = destino.x() - origen.x();
        int dy = destino.y() - origen.y();
        // Si el producto escalar es positivo, el angulo con el objetivo es menor de 90 grados
        return (dx * direccion.dx() + dy * direccion.dy()) > 0;
    }

    private int distanciaObjetivoTrasMover(SensoresGrid sensores, Direccion direccion) {
        return sensores.posicion().mover(direccion).distanciaManhattan(sensores.objetivo());
    }

    private Direccion direccionPrimaria(Posicion pos, Posicion objetivo) {
        int dx = objetivo.x() - pos.x();
        int dy = objetivo.y() - pos.y();
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);

        if (absDx > absDy) return dx > 0 ? Direccion.ESTE : Direccion.OESTE;
        if (absDy > absDx) return dy > 0 ? Direccion.SUR : Direccion.NORTE;

        // En empate se mantiene el rumbo vertical si acerca al objetivo.
        // Los otros casos eligen el horizontal, igual que en el desempate inicial.
        Direccion horizontal = dx > 0 ? Direccion.ESTE : Direccion.OESTE;
        Direccion vertical = dy > 0 ? Direccion.SUR : Direccion.NORTE;
        return orientacion == vertical ? vertical : horizontal;
    }
}
