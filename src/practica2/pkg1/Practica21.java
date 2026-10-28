/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package practica2.pkg1;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import termostato.EntornoTermostato;
import termostato.PoliticaTermostato;

/**
 *
 * @author pj
 */
public class Practica21 {

    public static void main(String[] args) {
        try {
            // Iniciar JADE con su interfaz gráfica.
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile perfil = new ProfileImpl();
            perfil.setParameter(Profile.GUI, "true");

            AgentContainer contenedor =
                    runtime.createMainContainer(perfil);

            // Crear el entorno y la política del termostato.
            EntornoTermostato entorno = new EntornoTermostato();
            PoliticaTermostato politica = new PoliticaTermostato();

            // Crear e iniciar el agente.
            AgentController agente = contenedor.createNewAgent(
                    "AgenteTermostato",
                    "base.Agente",
                    new Object[]{entorno, politica}
            );

            agente.start();

        } catch (Exception e) {
            System.err.println(
                    "Error al iniciar JADE: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}