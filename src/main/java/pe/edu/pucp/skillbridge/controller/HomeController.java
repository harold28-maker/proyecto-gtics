package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Controlador web encargado de mostrar la página principal y el inicio de sesión.
 * Expone las rutas públicas raíz y de autenticación.
 */
@Controller
public class HomeController {
    /**
     * Resuelve la solicitud GET realizada a la ruta {@code /}.
     *
     * @return Nombre de la vista principal que se debe renderizar.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
    /**
     * Resuelve la solicitud GET realizada a la ruta {@code /login}.
     * Prepara el formulario o solicita cerrar la sesión activa antes de ingresar de nuevo.
     *
     * @param error Parámetro opcional que indica si el inicio de sesión falló.
     * @param logout Parámetro opcional que indica si se cerró una sesión.
     * @param authentication Información de autenticación de la solicitud actual.
     * @param model Modelo utilizado para proporcionar datos a la vista de login.
     * @return Nombre de la vista de inicio de sesión.
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Authentication authentication,
                        Model model, HttpServletRequest request) {
        model.addAttribute("sesionActiva", tieneSesionActiva(authentication));
        model.addAttribute("titulo", "Iniciar sesión");
        // Consumimos el correo de un único intento fallido; no se guarda la contraseña.
        String correoIngresado = "";
        HttpSession sesion = request.getSession(false);
        if (error != null && sesion != null) {
            Object recordado = sesion.getAttribute("loginCorreoFallido");
            if (recordado instanceof String correo) correoIngresado = correo;
        }
        if (sesion != null) sesion.removeAttribute("loginCorreoFallido");
        model.addAttribute("correoIngresado", correoIngresado);
        model.addAttribute("errorLogin", error == null ? null
                : "Correo o contraseña incorrectos, o la cuenta no está activa.");
        model.addAttribute("logoutExitoso", logout != null);
        return "login";
    }

    /**
     * Entrega a la portada el token vigente para cerrar una sesión activa.
     *
     * @param authentication información de la sesión actual.
     * @param csrfToken token que Spring Security asocia a la solicitud.
     * @return token de cierre de sesión o una respuesta sin contenido si ya salió.
     */
    @GetMapping("/sesion/portada")
    public ResponseEntity<CsrfToken> sesionPortada(Authentication authentication,
                                                   @RequestAttribute("_csrf") CsrfToken csrfToken) {
        if (!tieneSesionActiva(authentication)) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(csrfToken);
    }

    private boolean tieneSesionActiva(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
