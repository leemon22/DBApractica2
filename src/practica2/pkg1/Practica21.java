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

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
            try {
            // Iniciar la plataforma JADE.
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile perfil = new ProfileImpl();
            AgentContainer contenedor =
                    runtime.createMainContainer(perfil);

            // Crear los componentes del termostato.
            EntornoTermostato entorno =
                    new EntornoTermostato();

            PoliticaTermostato politica =
                    new PoliticaTermostato();

            // Crear el agente utilizando la clase Base.
            AgentController agente = contenedor.createNewAgent(
                    "AgenteTermostato",
                    "base.Agente",
                    new Object[]{entorno, politica}
            );

            // Iniciar el agente.
            agente.start();
          
         

            // Cerrar el contenedor principal de JADE.
            contenedor.kill();

        } catch (Exception e) {
            System.err.println(
                    "Error al iniciar JADE: " + e.getMessage()
            );
            e.printStackTrace();
        }
	}
	
}
