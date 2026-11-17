package pe.edu.pucp.skillbridge.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.util.List;

/**
 * Servicio de autenticación que carga desde el repositorio los datos de usuario
 * utilizados por Spring Security para construir el objeto de autenticación.
 */
@Service
public class GestionUsuarioService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea el servicio con el repositorio usado para buscar usuarios por correo.
     *
     * @param usuarioRepository repositorio encargado de consultar los usuarios
     */
    public GestionUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario por correo sin distinguir mayúsculas y construye sus
     * detalles de autenticación, incluyendo contraseña, rol y estado.
     *
     * @param correo correo electrónico usado como nombre de usuario
     * @return detalles de seguridad del usuario encontrado
     * @throws UsernameNotFoundException si no existe un usuario con el correo indicado
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        String rol = usuario.getRol() == null ? "SIN_ROL" : usuario.getRol().getNombre();
        boolean activo = usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO;

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + rol)))
                .disabled(!activo)
                .accountLocked(usuario.getEstado() == Usuario.EstadoUsuario.BLOQUEADO)
                .build();
    }
}
