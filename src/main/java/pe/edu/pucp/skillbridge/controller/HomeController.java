package pe.edu.pucp.skillbridge.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
/**
 * Controlador web encargado de mostrar la página principal y el inicio de sesión.
 * Expone las rutas públicas raíz y de autenticación.
 */
public class HomeController {
    @GetMapping("/")
    /**
     * Resuelve la solicitud GET realizada a la ruta {@code /}.
     *
     * @return Nombre de la vista principal que se debe renderizar.
     */
    public String index() {
        return "index";
    }
    @GetMapping("/login")
    /**
     * Resuelve la solicitud GET realizada a la ruta {@code /login}.
     * Redirige a una sesión autenticada según su rol o prepara los atributos
     * visibles en la vista de inicio de sesión.
     *
     * @param error Parámetro opcional que indica si el inicio de sesión falló.
     * @param logout Parámetro opcional que indica si se cerró una sesión.
     * @param authentication Información de autenticación de la solicitud actual.
     * @param model Modelo utilizado para proporcionar datos a la vista de login.
     * @return Nombre de la vista de login o una redirección según el rol autenticado.
     */
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Authentication authentication,
                        Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + LoginSuccessHandler.inicioSegunRol(authentication);
        }

        model.addAttribute("titulo", "Iniciar sesión");
        model.addAttribute("correoIngresado", "");
        model.addAttribute("errorLogin", error == null ? null
                : "Correo o contraseña incorrectos, o la cuenta no está activa.");
        model.addAttribute("logoutExitoso", logout != null);
        return "login";
    }
}
