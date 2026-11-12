package base;

import java.io.IOException;

/**
 * Interfaz que define el comportamiento de un entorno con el que un agente interactúa.
 *
 * @param <S> tipo de percepciones (debe implementar Sensor)
 * @param <A> tipo de acciones (debe implementar Accion)
 */
public interface Entorno<S extends Sensor, A extends Accion> {

    /**
     * Inicia el descubrimiento del reto y solicita el nuevo run en DBA Lab.
     *
     * @throws IOException          si ocurre un fallo de red durante el inicio
     * @throws InterruptedException si se interrumpe la comunicación
     */
    void iniciarRun() throws IOException, InterruptedException;

    /**
     * Lee las percepciones necesarias del entorno.
     *
     * @return percepciones actuales del agente
     * @throws IOException          si ocurre un error de comunicación al consultar sensores
     * @throws InterruptedException si se interrumpe la comunicación
     */
    S leerSensores() throws IOException, InterruptedException;

    /**
     * Envía la acción elegida por la política y devuelve el nuevo estado de control oficial.
     *
     * @param accion acción física a ejecutar
     * @return estado de control actualizado tras procesar la acción
     * @throws IOException          si ocurre un error de comunicación al enviar la acción
     * @throws InterruptedException si se interrumpe la comunicación
     */
    EstadoControl ejecutarAccion(A accion) throws IOException, InterruptedException;

    /**
     * Permite consultar el estado de control actual sin consumir turnos ni peticiones.
     *
     * @return estado de control actual
     */
    EstadoControl getEstadoActual();
}
