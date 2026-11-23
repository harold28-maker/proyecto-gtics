package pe.edu.pucp.skillbridge.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.edu.pucp.skillbridge.service.ConsultaDniService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interfaz de consulta DNI para Administrador y Resource Manager.
 * Consume API Perú como proveedor externo de identidad.
 * Nunca almacena el DNI consultado o la respuesta en la base.
 */
@Controller
public class ConsultaDniWebController {
    private final ConsultaDniService service;

    public ConsultaDniWebController(ConsultaDniService service) {
        this.service = service;
    }

    private String ruta(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        boolean resource = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESOURCE_MANAGER"));
        if (!admin && !resource) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return admin ? "/admin/consulta-dni" : "/resource/consulta-dni";
    }

    private void preparar(Model model, Authentication auth, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0");
        model.addAttribute("titulo", "Consulta DNI");
        model.addAttribute("accion", ruta(auth));
    }

    @GetMapping({"/admin/consulta-dni", "/resource/consulta-dni"})
    public String formulario(Model model, Authentication auth, HttpServletResponse response) {
        preparar(model, auth, response);
        return "identidad/consulta-dni";
    }

    @PostMapping({"/admin/consulta-dni", "/resource/consulta-dni"})
    public String consultar(@RequestParam("dni") String dni,
                            @RequestParam(value="consentimiento", defaultValue="false") boolean consentimiento,
                            Model model, Authentication auth, HttpServletResponse response) {
        preparar(model, auth, response);
        if (!consentimiento) {
            model.addAttribute("error", "Debes confirmar que tienes autorización para consultar este DNI.");
            return "identidad/consulta-dni";
        }
        if (dni == null || !dni.matches("[0-9]{8}")) {
            model.addAttribute("error", "Ingresa exactamente 8 dígitos numéricos.");
            return "identidad/consulta-dni";
        }
        try {
            model.addAttribute("resultado", service.consultar(dni));
        } catch (ResponseStatusException ex) {
            // Mensaje controlado del servicio; nunca mostrar token ni cuerpo HTTP externo.
            model.addAttribute("error", ex.getReason() == null ? "No se pudo completar la consulta." : ex.getReason());
        }
        return "identidad/consulta-dni";
    }
}
