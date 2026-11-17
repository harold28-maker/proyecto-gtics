package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
/**
 * Gestiona el acceso exitoso, actualiza el último ingreso y establece los datos
 * de sesión necesarios para la navegación según el rol.
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UsuarioRepository usuarioRepository;

    /**
     * Construye el manejador de autenticación exitosa con el repositorio de usuarios.
     *
     * @param usuarioRepository Repositorio utilizado para actualizar y recuperar al usuario autenticado.
     */
    public LoginSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    /**
     * Procesa una autenticación exitosa y redirige al usuario a la página inicial
     * correspondiente a su rol.
     *
     * @param request Solicitud HTTP que originó la autenticación.
     * @param response Respuesta HTTP utilizada para guardar la sesión y redirigir.
     * @param authentication Datos de autenticación asociados al usuario.
     * @throws IOException Si ocurre un error al emitir la redirección HTTP.
     */
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(authentication.getName()).orElseThrow();
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        HttpSession session = request.getSession();
        session.setAttribute("usuarioSesionId", usuario.getIdUsuario());
        session.setAttribute("rolVista", rolVista(authentication));
        response.sendRedirect(inicioSegunRol(authentication));
    }

    /**
     * Determina la ruta inicial pública asociada a la autoridad de la autenticación.
     *
     * @param authentication Datos de autenticación cuyos permisos se evalúan.
     * @return Ruta inicial del administrador, gerente de proyectos, gerente de recursos
     *         o colaborador según corresponda.
     */
    public static String inicioSegunRol(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            return switch (authority.getAuthority()) {
                case "ROLE_ADMINISTRADOR" -> "/admin/inicio";
                case "ROLE_PROJECT_MANAGER" -> "/pm/inicio";
                case "ROLE_RESOURCE_MANAGER" -> "/resource/inicio";
                default -> "/colaborador/inicio";
            };
        }
        return "/colaborador/inicio";
    }

    /**
     * Determina el valor de rol mostrado en las vistas a partir de la autenticación.
     *
     * @param authentication Datos de autenticación cuyos permisos se evalúan.
     * @return Código de rol de vista correspondiente: {@code ADMIN}, {@code PM},
     *         {@code RM} o {@code COL}.
     */
    private static String rolVista(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            return switch (authority.getAuthority()) {
                case "ROLE_ADMINISTRADOR" -> "ADMIN";
                case "ROLE_PROJECT_MANAGER" -> "PM";
                case "ROLE_RESOURCE_MANAGER" -> "RM";
                default -> "COL";
            };
        }
        return "COL";
    }
}
