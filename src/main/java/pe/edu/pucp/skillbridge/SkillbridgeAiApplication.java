package pe.edu.pucp.skillbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase de arranque de la aplicación SkillBridge basada en Spring Boot.
 */
@SpringBootApplication
public class SkillbridgeAiApplication {
    /**
     * Inicia la aplicación Spring Boot y sus componentes configurados.
     *
     * @param args argumentos de línea de comandos entregados al ejecutar la aplicación
     */
    public static void main(String[] args) {
        SpringApplication.run(SkillbridgeAiApplication.class, args);
    }
}
