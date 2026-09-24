package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
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
        Usuario usuario = usuarioRepository.findByCorreo(authentication.getName()).orElseThrow();
        String rolVista = request.getParameter("rol");
        String rolSeleccionado = rolBd(rolVista);

        if (usuario.getRol() == null || !rolSeleccionado.equals(usuario.getRol().getNombre())) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            String destino = UriComponentsBuilder.fromPath("/login")
                    .queryParam("error", "rol")
                    .queryParam("rol", rolVista)
                    .queryParam("correo", usuario.getCorreo())
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(destino);
            return;
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        HttpSession session = request.getSession();
        session.setAttribute("usuarioSesionId", usuario.getIdUsuario());
        session.setAttribute("rolVista", rolVista);
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

    private static String rolBd(String rolVista) {
        return switch (rolVista == null ? "COL" : rolVista) {
            case "ADMIN" -> "ADMINISTRADOR";
            case "PM" -> "PROJECT_MANAGER";
            case "RM" -> "RESOURCE_MANAGER";
            default -> "COLABORADOR";
        };
    }
}
