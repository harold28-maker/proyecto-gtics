package pe.edu.pucp.skillbridge.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarPassword {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String hash =
                encoder.encode("Demo1234");

        System.out.println(hash);
    }
}