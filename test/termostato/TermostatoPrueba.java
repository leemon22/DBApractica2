package termostato;

import base.BucleAgentico;
import base.ClienteHttpDba;
import base.EstadoControl;
import base.InfoReto;
import base.LecturaSensor;
import base.RespuestaAccion;

import jade.core.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Verifica decisiones y ciclo completo del termostato con un cliente simulado. */
public final class TermostatoPrueba {
    public static void main(String[] args) throws Exception {
        PoliticaTermostato politica = new PoliticaTermostato();
        LimitesTermostato limites = new LimitesTermostato(18, 26, 22);
        exigir(
                politica.aplicar(new SensoresTermostato(21, limites)) == AccionTermostato.OFF,
                "Apagar por debajo del objetivo");
        exigir(
                politica.aplicar(new SensoresTermostato(22, limites)) == AccionTermostato.ON,
                "Encender en el objetivo");
        exigir(
                politica.aplicar(new SensoresTermostato(23, limites)) == AccionTermostato.ON,
                "Encender por encima del objetivo");

        Cliente cliente = new Cliente();
        EntornoTermostato entorno = new EntornoTermostato(cliente);
        entorno.iniciarRun();
        BucleAgentico<SensoresTermostato, AccionTermostato> bucle =
                new BucleAgentico<>(new Agent(), entorno, politica);
        bucle.action();
        exigir(bucle.done(), "El bucle debe terminar tras SUCCEEDED");
        exigir(
                entorno.getEstadoControl().orElseThrow().runStatus()
                        == EstadoControl.RunStatus.SUCCEEDED,
                "Estado final");
        bucle.action();
        entorno.cerrarRun();
        exigir(
                cliente.eventos.equals(
                        List.of(
                                "inicio",
                                "thermostat.limits",
                                "thermostat.target",
                                "thermostat.temperature",
                                "thermostat.refrigeration.on")),
                "No debe repetir acciones ni cerrar un run ya terminado");

        cliente = new Cliente();
        cliente.capacidadesCompletas = false;
        entorno = new EntornoTermostato(cliente);
        try {
            entorno.iniciarRun();
            throw new AssertionError("Debe rechazar retos sin capacidades suficientes");
        } catch (IllegalStateException esperado) {
            exigir(cliente.eventos.isEmpty(), "La validación debe ocurrir antes de crear el run");
        }
        System.out.println("Política y ciclo del termostato: correctos.");
    }

    private static class Cliente extends ClienteHttpDba {
        final List<String> eventos = new ArrayList<>();
        boolean capacidadesCompletas = true;

        Cliente() {
            super("http://simulado", "ficticio");
        }

        @Override
        public List<InfoReto> getRetos() {
            return List.of(
                    new InfoReto(
                            1,
                            "THERMOSTAT",
                            capacidadesCompletas
                                    ? List.of(
                                            "thermostat.temperature",
                                            "thermostat.limits",
                                            "thermostat.target")
                                    : List.of(),
                            List.of(
                                    "thermostat.refrigeration.on",
                                    "thermostat.refrigeration.off")));
        }

        @Override
        public EstadoControl iniciarRun(long reto) {
            eventos.add("inicio");
            return estado(EstadoControl.RunStatus.ACTIVE);
        }

        @Override
        public LecturaSensor consultarSensor(String run, String sensor) {
            eventos.add(sensor);
            Map<String, Object> valores =
                    sensor.equals("thermostat.limits")
                            ? Map.of("lower", 18, "upper", 26)
                            : Map.of("celsius", sensor.equals("thermostat.target") ? 22 : 23);
            return new LecturaSensor(sensor, valores, estado(EstadoControl.RunStatus.ACTIVE));
        }

        @Override
        public RespuestaAccion enviarAccion(String run, String accion) {
            eventos.add(accion);
            return new RespuestaAccion("APPLIED", null, estado(EstadoControl.RunStatus.SUCCEEDED));
        }

        @Override
        public EstadoControl cerrarRun(String run) {
            throw new AssertionError("Cierre innecesario");
        }

        private EstadoControl estado(EstadoControl.RunStatus estado) {
            return new EstadoControl("1", estado, null);
        }
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
