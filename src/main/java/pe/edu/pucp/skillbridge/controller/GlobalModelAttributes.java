package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UsuarioRepository usuarioRepository;

    public GlobalModelAttributes(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("usuarioSesion")
    public Usuario usuarioSesion(HttpSession session) {
        Object id = session.getAttribute("usuarioSesionId");
        if (id == null) return null;
        return usuarioRepository.findById((Integer) id).orElse(null);
    }

    @ModelAttribute("rolSesion")
    public String rolSesion(HttpSession session) {
        Object rol = session.getAttribute("rolVista");
        return rol == null ? "COL" : rol.toString();
    }
}
