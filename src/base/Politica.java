package base;

// Interfaz que define la política de decisión del agente.
// S: percepciones del entorno (Sensor)
// A: acción a ejecutar (Accion)
public interface Politica<S extends Sensor, A extends Accion> {

    // Aplica la política sobre las percepciones actuales y devuelve la acción a ejecutar.
    A aplicar(S sensores);
}
