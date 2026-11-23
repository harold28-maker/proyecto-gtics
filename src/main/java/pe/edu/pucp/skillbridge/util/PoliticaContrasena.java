package pe.edu.pucp.skillbridge.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PoliticaContrasena {

    public static final String MENSAJE = "La contraseña debe tener entre 8 y 30 caracteres, una mayúscula, una minúscula y un número.";
    private static final Pattern FORMATO = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,30}$");

    private PoliticaContrasena() {
    }

    public static String validar(String contrasena, String confirmacion) {
        if (contrasena == null || !FORMATO.matcher(contrasena).matches()) {
            return MENSAJE;
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "La contraseña supera el límite permitido. Usa una contraseña más corta.";
        }
        return contrasena.equals(confirmacion) ? null : "Las contraseñas no coinciden.";
    }
}
