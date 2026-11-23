package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.service.CorreoCuentaService;
import pe.edu.pucp.skillbridge.service.RecuperacionContrasenaService;
import pe.edu.pucp.skillbridge.util.PoliticaContrasena;

import java.util.Locale;
import java.util.regex.Pattern;

@Controller
public class RecuperacionContrasenaController {

    private static final String SOLICITUD_SESION = "recuperacionContrasena";
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final RecuperacionContrasenaService recuperacion;
    private final CorreoCuentaService correos;

    public RecuperacionContrasenaController(RecuperacionContrasenaService recuperacion, CorreoCuentaService correos) {
        this.recuperacion = recuperacion;
        this.correos = correos;
    }

    @ModelAttribute
    public void protegerEnlace(HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping("/recuperar-contrasena")
    public String mostrarSolicitud() {
        return "recuperacion";
    }

    @PostMapping("/recuperar-contrasena")
    public String solicitar(@RequestParam(defaultValue = "") String correo, Model model,
                            RedirectAttributes redirect) {
        String destinatario = correo.strip().toLowerCase(Locale.ROOT);
        if (destinatario.length() > 150 || !CORREO.matcher(destinatario).matches()) {
            model.addAttribute("errorRecuperacion", "Ingresa un correo institucional válido.");
            return "recuperacion";
        }
        if (!correos.disponible()) {
            model.addAttribute("errorRecuperacion", "El envío de correo no está disponible. Comunícate con el administrador.");
            return "recuperacion";
        }

        recuperacion.solicitar(destinatario);
        redirect.addFlashAttribute("solicitudEnviada", true);
        return "redirect:/recuperar-contrasena";
    }

    @GetMapping("/restablecer-contrasena")
    public String mostrarCambio(@RequestParam(required = false) String token, HttpSession session, Model model) {
        if (token != null) {
            String solicitud = recuperacion.verificarEnlace(token);
            if (solicitud == null) {
                session.removeAttribute(SOLICITUD_SESION);
            } else {
                session.setAttribute(SOLICITUD_SESION, solicitud);
            }
            return "redirect:/restablecer-contrasena";
        }

        String solicitud = (String) session.getAttribute(SOLICITUD_SESION);
        if (!recuperacion.vigente(solicitud)) {
            session.removeAttribute(SOLICITUD_SESION);
            return vistaCambio(model, null, null);
        }
        return vistaCambio(model, solicitud, null);
    }

    @PostMapping("/restablecer-contrasena")
    public String guardar(@RequestParam(defaultValue = "") String solicitud,
                          @RequestParam(defaultValue = "") String password,
                          @RequestParam(defaultValue = "") String confirmarPassword,
                          HttpServletRequest request, HttpServletResponse response, Authentication authentication,
                          Model model, RedirectAttributes redirect) {
        HttpSession session = request.getSession(false);
        if (session == null || !solicitud.equals(session.getAttribute(SOLICITUD_SESION))) {
            return vistaCambio(model, null, null);
        }

        String error = PoliticaContrasena.validar(password, confirmarPassword);
        if (error != null) {
            if (!recuperacion.vigente(solicitud)) {
                session.removeAttribute(SOLICITUD_SESION);
                return vistaCambio(model, null, null);
            }
            return vistaCambio(model, solicitud, error);
        }

        if (!recuperacion.cambiarContrasena(solicitud, password)) {
            session.removeAttribute(SOLICITUD_SESION);
            return vistaCambio(model, null, null);
        }

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        redirect.addFlashAttribute("contrasenaActualizada", true);
        return "redirect:/login";
    }

    private String vistaCambio(Model model, String solicitud, String error) {
        model.addAttribute("cambiarContrasena", true);
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("errorRecuperacion", error);
        return "recuperacion";
    }
}
