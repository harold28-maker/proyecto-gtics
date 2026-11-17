package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

/**
 * Aporta a todas las vistas los datos del usuario y del rol asociados a la sesión.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UsuarioRepository usuarioRepository;

    /**
     * Construye el componente de atributos globales con el repositorio de usuarios.
     *
     * @param usuarioRepository Repositorio utilizado para recuperar al usuario de sesión.
     */
    public GlobalModelAttributes(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtiene el usuario identificado por el atributo {@code usuarioSesionId}
     * almacenado en la sesión HTTP.
     *
     * @param session Sesión HTTP que contiene el identificador del usuario.
     * @return Usuario encontrado o {@code null} si no existe identificador o usuario.
     */
    @ModelAttribute("usuarioSesion")
    public Usuario usuarioSesion(HttpSession session) {
        Object id = session.getAttribute("usuarioSesionId");
        if (id == null) return null;
        return usuarioRepository.findById((Integer) id).orElse(null);
    }

    /**
     * Obtiene el rol de vista almacenado en la sesión HTTP.
     *
     * @param session Sesión HTTP que contiene el rol mostrado en las vistas.
     * @return Rol de la sesión, o {@code COL} cuando no existe un rol asociado.
     */
    @ModelAttribute("rolSesion")
    public String rolSesion(HttpSession session) {
        Object rol = session.getAttribute("rolVista");
        return rol == null ? "COL" : rol.toString();
    }
}
