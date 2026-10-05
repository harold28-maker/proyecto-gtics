package pe.edu.pucp.skillbridge.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
    @GetMapping("/login")
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
