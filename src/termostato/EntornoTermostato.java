/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package termostato;
import base.Configuracion;
import base.Entorno;
import base.EstadoControl;
import base.ClienteHttpDba;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.net.http.HttpResponse;

/**
 *
 * @author martinhernandezruiz
 */
public class EntornoTermostato implements Entorno<SensoresTermostato, AccionTermostato> {

    private ClienteHttpDba cliente;
    private EstadoControl estadoActual;
    private String runId;
    
    
    private void validarCapacidades(JsonObject reto) {

        JsonArray sensores = reto.getAsJsonArray("sensorCodes");
        JsonArray acciones = reto.getAsJsonArray("actionCodes");

        String[] sensoresNecesarios = {
            "thermostat.temperature",
            "thermostat.limits",
            "thermostat.target"
        };

        String[] accionesNecesarias = {
            "thermostat.refrigeration.on",
            "thermostat.refrigeration.off"
        };

        for (String sensor : sensoresNecesarios) {
            boolean disponible = false;

            for (JsonElement elemento : sensores) {
                if (sensor.equals(elemento.getAsString())) {
                    disponible = true;
                    break;
                }
            }

            if (!disponible) {
                throw new IllegalStateException(
                    "Sensor no disponible: " + sensor
                );
            }
        }

        for (String accion : accionesNecesarias) {
            boolean disponible = false;

            for (JsonElement elemento : acciones) {
                if (accion.equals(elemento.getAsString())) {
                    disponible = true;
                    break;
                }
            }

            if (!disponible) {
                throw new IllegalStateException(
                    "Accion no disponible: " + accion
                );
            }
        }
    }

    

    
    
    private void actualizarEstado(JsonObject control) {

        String identificador = control.get("runId")
                .getAsString();

        String estado = control.get("runStatus")
                .getAsString();

        String motivo = control.has("reasonCode")
                && !control.get("reasonCode").isJsonNull()
                ? control.get("reasonCode").getAsString()
                : null;

        estadoActual = new EstadoControl(
                identificador,
                EstadoControl.RunStatus.valueOf(estado),
                motivo
        );
    }


    private JsonObject consultarSensor(String sensorId) throws Exception {

        HttpResponse<String> respuesta =
                cliente.enviarPercepcion(runId, sensorId);

        if (respuesta.statusCode() < 200
                || respuesta.statusCode() >= 300) {

            throw new IllegalStateException(
                    "Error al consultar " + sensorId
                    + ". HTTP " + respuesta.statusCode()
                    + ": " + respuesta.body()
            );
        }

        JsonObject json = JsonParser.parseString(
                respuesta.body()
        ).getAsJsonObject();

        // Comprobar el estado oficial tras la interacción.
        JsonObject control = json.getAsJsonObject("control");

        actualizarEstado(control);

        

        if (!estadoActual.isActivo()) {
            throw new IllegalStateException(
                "Run finalizado: " + estadoActual.runStatus()
                + " | Motivo: " + estadoActual.reasonCode()
            );
        }



        return json.getAsJsonObject("value");
    }


    
    @Override
    public void iniciarRun(Configuracion config) throws Exception {

        cliente = new ClienteHttpDba(
                config.getUrlLaboratorio(),
                config.getToken()
        );

        HttpResponse<String> respuesta = cliente.getRetos();

        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException(
                    "Error HTTP: " + respuesta.statusCode()
                    + " - " + respuesta.body()
            );
        }

        JsonObject json = JsonParser.parseString(
                respuesta.body()
        ).getAsJsonObject();

        JsonArray retos = json.getAsJsonArray("challenges");

        boolean encontrado = false;

        for (JsonElement elemento : retos) {

            JsonObject reto = elemento.getAsJsonObject();

            String tipo = reto.get("environmentType")
                              .getAsString();

            
    if ("THERMOSTAT".equals(tipo)) {
        
        validarCapacidades(reto);

        long challengeId = reto.get("id").getAsLong();

        System.out.println(
                "Reto termostato encontrado: " + challengeId
        );

        // Iniciar una ejecución del reto.
        HttpResponse<String> inicio = cliente.iniciarRun(challengeId);

        if (inicio.statusCode() < 200 || inicio.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Error al iniciar el run. HTTP "
                    + inicio.statusCode() + ": " + inicio.body()
            );
        }

        // Obtener el bloque de control oficial.
        JsonObject respuestaInicio = JsonParser.parseString(
                inicio.body()
        ).getAsJsonObject();

        JsonObject control = respuestaInicio
                .getAsJsonObject("control");


        actualizarEstado(control);
        runId = estadoActual.runId();


        System.out.println("Run iniciado: " + runId);
        System.out.println("Estado: " + estadoActual.runStatus());

        encontrado = true;
        break;
    }

    }

    if (!encontrado) {
        throw new IllegalStateException(
                "No hay retos de termostato disponibles"
        );
    }
}


    
    @Override
    

    public SensoresTermostato leerSensores() throws Exception {

        JsonObject temperatura =
                consultarSensor("thermostat.temperature");

        JsonObject limites =
                consultarSensor("thermostat.limits");

        JsonObject objetivo =
                consultarSensor("thermostat.target");

        double tempActual = temperatura.get("celsius").getAsDouble();
        double inferior = limites.get("lower").getAsDouble();
        double superior = limites.get("upper").getAsDouble();
        double orientativa = objetivo.get("celsius").getAsDouble();

        System.out.println("\n--- PERCEPCION DEL AGENTE ---");
        System.out.println("Temperatura actual: " + tempActual + " °C");
        System.out.println("Limites permitidos: [" + inferior + ", " + superior + "] °C");
        System.out.println("Temperatura orientativa: " + orientativa + " °C");

        return new SensoresTermostato(
                tempActual,
                inferior,
                superior,
                orientativa
        );
    }



    
    @Override
    public EstadoControl ejecutarAccion(
            AccionTermostato accion) throws Exception {

        // Enviar la accion seleccionada por la politica.
        HttpResponse<String> respuesta =
                cliente.enviarAccion(runId, accion.getCodigo());

        // Comprobar el resultado HTTP.
        if (respuesta.statusCode() < 200
                || respuesta.statusCode() >= 300) {

            throw new IllegalStateException(
                    "Error al ejecutar accion. HTTP "
                    + respuesta.statusCode()
                    + ": " + respuesta.body()
            );
        }

        // Interpretar la respuesta JSON.
        JsonObject json = JsonParser.parseString(
                respuesta.body()
        ).getAsJsonObject();

        String resultado = json.get("actionStatus")
                .getAsString();

        // Actualizar el estado oficial del run.
        actualizarEstado(json.getAsJsonObject("control"));

        System.out.println(
                "Accion: " + accion
                + " | Resultado: " + resultado
                + " | Estado: " + estadoActual.runStatus()
        );

        // Una accion rechazada no modifica el entorno.
        if ("REJECTED".equals(resultado)) {
            throw new IllegalStateException(
                    "Accion rechazada por DBA Lab: "
                    + json.get("reasonCode")
            );
        }

        return estadoActual;
    }


    @Override
    public EstadoControl getEstadoActual() {
        return estadoActual;
    }
    
    
    
}
