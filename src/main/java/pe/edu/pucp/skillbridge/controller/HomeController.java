package pe.edu.pucp.skillbridge.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
                        @RequestParam(required = false) String rol,
                        @RequestParam(required = false) String correo,
                        Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + LoginSuccessHandler.inicioSegunRol(authentication);
        }

        String mensaje = null;
        if ("rol".equals(error)) {
            mensaje = "La cuenta no pertenece al perfil seleccionado. Verifica el rol e inténtalo nuevamente.";
        } else if (error != null) {
            mensaje = "El correo o la contraseña no son correctos.";
        }
        prepararLogin(model, rol, error != null, correo, mensaje);
        model.addAttribute("logoutExitoso", logout != null);
        return "login";
    }

    private void prepararLogin(Model model,
                               String rol,
                               boolean mostrarCredenciales,
                               String correo,
                               String error) {
        model.addAttribute("titulo", "Iniciar sesión");
        model.addAttribute("rolSeleccionado", rol == null ? "COL" : rol);
        model.addAttribute("mostrarCredenciales", mostrarCredenciales);
        model.addAttribute("correoIngresado", correo == null ? "" : correo);
        model.addAttribute("errorLogin", error);
    }
}
