package base;

// Interfaz que define el comportamiento de un entorno en el que un agente puede interactuar.
// S: sensores leídos del entorno
// A: acción a ejecutar en el entorno
public interface Entorno<S, A> {

    // Inicia el descubrimiento y solicita el run.
    // Usará la Configuración para instanciar su propio ClienteHttpDba.
    void iniciarRun(Configuracion config) throws Exception;

    // Lee las percepciones necesarias del entorno.
    S leerSensores() throws Exception;

    // Envía la acción elegida y devuelve el nuevo estado de control oficial.
    EstadoControl ejecutarAccion(A accion) throws Exception;

    // Permite al agente consultar si el run sigue activo sin gastar un turno.
    EstadoControl getEstadoActual();
}
