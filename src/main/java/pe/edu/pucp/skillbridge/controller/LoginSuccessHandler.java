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
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UsuarioRepository usuarioRepository;

    public LoginSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
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
