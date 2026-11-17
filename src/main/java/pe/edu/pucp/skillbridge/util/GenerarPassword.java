package pe.edu.pucp.skillbridge.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad ejecutable que genera el hash BCrypt de una contraseña de demostración.
 */
public class GenerarPassword {

    /**
     * Genera en consola el hash BCrypt de la contraseña de demostración y lo imprime.
     *
     * @param args argumentos de línea de comandos no utilizados por la utilidad
     */
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String hash =
                encoder.encode("Demo1234");

        System.out.println(hash);
    }
}