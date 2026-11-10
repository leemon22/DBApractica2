package grid;

import base.Politica;

// Política de navegación de Grid: Bug0 con progreso y elección dinámica de mano (Tema 2).
//
// Máquina de dos modos:
// - ACERCAMIENTO: moverse a la primera dirección libre (orden N, E, S, O) que reduce la
//   distancia Manhattan al objetivo.
// - SEGUIMIENTO: si ninguna dirección que acerca está libre, rodear el obstáculo con una mano
//   hasta cumplir la condición de salida (criterio lateral, ver debeSalir).
//
// La política guarda estado entre llamadas (modo, orientación, mano y d0), así que se necesita
// una instancia nueva por cada run.
public class PoliticaGrid implements Politica<SensoresGrid, Direccion> {

    private enum Modo {
        ACERCAMIENTO,
        SEGUIMIENTO
    }

    private enum Mano {
        IZQUIERDA,
        DERECHA
    }

    private Modo modo = Modo.ACERCAMIENTO;
    // Dirección del último movimiento realizado. Los giros del seguimiento son relativos a ella.
    private Direccion orientacion = Direccion.NORTE;
    // Mano con la que se rodea el obstáculo actual. Solo tiene sentido en SEGUIMIENTO.
    private Mano mano = Mano.DERECHA;
    // Distancia al objetivo al empezar el rodeo actual.
    private int d0;

    @Override
    public Direccion aplicar(SensoresGrid sensores) {
        if (sensores.enObjetivo()) {
            throw new IllegalStateException("El agente ya está en el objetivo " + sensores.objetivo());
        }

        if (modo == Modo.SEGUIMIENTO && debeSalir(sensores)) {
            modo = Modo.ACERCAMIENTO;
            System.out.println("Fin del rodeo en " + sensores.posicion());
        }

        Direccion direccion = (modo == Modo.ACERCAMIENTO)
                ? acercarse(sensores)
                : seguirMuro(sensores);

        // Salvaguarda: moverse hacia un bumper a true termina el run, así que nunca se envía.
        // Los métodos anteriores solo eligen direcciones libres; si esto salta, es un error nuestro.
        if (sensores.bloqueado(direccion)) {
            throw new IllegalStateException(
                    "La política eligió " + direccion + " estando bloqueada en " + sensores.posicion());
        }

        orientacion = direccion;
        return direccion;
    }

    // --- ACERCAMIENTO ---

    // Primera dirección libre que acerca al objetivo. Si no hay ninguna, empieza un rodeo.
    private Direccion acercarse(SensoresGrid sensores) {
        for (Direccion d : Direccion.values()) {
            if (sensores.libre(d) && acerca(sensores, d)) {
                return d;
            }
        }
        return iniciarSeguimiento(sensores);
    }

    // Elección dinámica de mano: de los dos giros laterales respecto a la intención bloqueada,
    // se elige el libre cuya celda queda más cerca del objetivo. En empate, mano derecha (girar a
    // la izquierda), que es la preferencia por defecto del Tema 2.
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
            // Callejón: solo queda volver atrás. Se rodea con la mano por defecto.
            elegida = intencion.retroceso();
        }

        mano = (elegida == giroDerecha) ? Mano.IZQUIERDA : Mano.DERECHA;
        d0 = sensores.distanciaAlObjetivo();
        modo = Modo.SEGUIMIENTO;
        System.out.println("Inicio de rodeo en " + sensores.posicion()
                + " | bloqueado " + intencion + " | mano " + mano + " | d0 = " + d0);
        return elegida;
    }

    // --- SEGUIMIENTO ---

    // Regla de la mano: con mano derecha, prioridad derecha, frente, izquierda y retroceso;
    // con mano izquierda, izquierda, frente, derecha y retroceso.
    private Direccion seguirMuro(SensoresGrid sensores) {
        Direccion[] prioridad = (mano == Mano.DERECHA)
                ? new Direccion[]{orientacion.derecha(), orientacion, orientacion.izquierda(), orientacion.retroceso()}
                : new Direccion[]{orientacion.izquierda(), orientacion, orientacion.derecha(), orientacion.retroceso()};

        for (Direccion d : prioridad) {
            if (sensores.libre(d)) {
                return d;
            }
        }
        throw new IllegalStateException("Las cuatro direcciones están bloqueadas en " + sensores.posicion());
    }

    // Condición de salida (criterio lateral, context.md sección 7.2):
    // 1. progreso: más cerca del objetivo que al empezar el rodeo (h < d0);
    // 2. el mejor movimiento de acercamiento está libre;
    // 3. el objetivo queda en el lado libre, el contrario a la mano que toca el muro.
    private boolean debeSalir(SensoresGrid sensores) {
        if (sensores.distanciaAlObjetivo() >= d0) {
            return false;
        }
        if (sensores.bloqueado(mejorMovimiento(sensores))) {
            return false;
        }
        Direccion ladoLibre = (mano == Mano.DERECHA) ? orientacion.izquierda() : orientacion.derecha();
        int haciaObjetivoX = sensores.objetivo().x() - sensores.posicion().x();
        int haciaObjetivoY = sensores.objetivo().y() - sensores.posicion().y();
        return haciaObjetivoX * ladoLibre.dx() + haciaObjetivoY * ladoLibre.dy() > 0;
    }

    // --- Utilidades ---

    // Primera dirección (orden N, E, S, O) que reduce la distancia, esté libre o no.
    // Existe siempre que el agente no esté ya en el objetivo.
    private Direccion mejorMovimiento(SensoresGrid sensores) {
        for (Direccion d : Direccion.values()) {
            if (acerca(sensores, d)) {
                return d;
            }
        }
        throw new IllegalStateException("Ninguna dirección acerca al objetivo desde " + sensores.posicion());
    }

    private boolean acerca(SensoresGrid sensores, Direccion d) {
        return distanciaTras(sensores, d) < sensores.distanciaAlObjetivo();
    }

    private int distanciaTras(SensoresGrid sensores, Direccion d) {
        return sensores.posicion().mover(d).distanciaManhattan(sensores.objetivo());
    }
}
