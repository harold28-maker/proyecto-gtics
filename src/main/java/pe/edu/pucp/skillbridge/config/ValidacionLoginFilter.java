package pe.edu.pucp.skillbridge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.util.PoliticaContrasena;

import java.io.IOException;

/** Valida los campos del formulario antes de la autenticación de Spring Security. */
public final class ValidacionLoginFilter extends OncePerRequestFilter {

    private static final String AVISOS_LOGIN = ValidacionLoginFilter.class.getName() + ".avisos";
    private final Validator validator;

    public ValidacionLoginFilter(Validator validator) {
        this.validator = validator;
    }

    public record Avisos(String correo, String errorCorreo, String errorContrasena) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/acceder".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        consumirAvisos(request);
        String valor = request.getParameter("correo");
        String correo = valor == null ? "" : valor.trim();
        String errorCorreo = validarCorreo(correo);
        String errorContrasena = PoliticaContrasena.validarIngreso(request.getParameter("password"));
        if (errorCorreo == null && errorContrasena == null) {
            chain.doFilter(request, response);
            return;
        }

        request.getSession().setAttribute(AVISOS_LOGIN,
                new Avisos(correo.length() <= 150 ? correo : "", errorCorreo, errorContrasena));
        response.sendRedirect(request.getContextPath() + "/login");
    }

    public static Avisos consumirAvisos(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object avisos = sesion.getAttribute(AVISOS_LOGIN);
        sesion.removeAttribute(AVISOS_LOGIN);
        return avisos instanceof Avisos resultado ? resultado : null;
    }

    private String validarCorreo(String correo) {
        if (correo.isBlank()) return "Ingresa tu correo institucional.";
        if (correo.length() > 150) return "El correo admite hasta 150 caracteres.";
        return validator.validateValue(Usuario.class, "correo", correo).isEmpty()
                ? null : "Ingresa un correo institucional válido.";
    }
}
