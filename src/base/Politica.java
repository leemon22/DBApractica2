package base;

// Política que ejecuta el agente para decidir qué acción tomar en base a los sensores leídos del
// entorno.
// S: sensores leídos del entorno
// A: acción a ejecutar en el entorno
public interface Politica<S, A> {
    A aplicar(S sensores);
}
