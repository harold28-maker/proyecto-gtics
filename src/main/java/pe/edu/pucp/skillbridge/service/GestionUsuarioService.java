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

@Service
public class GestionUsuarioService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public GestionUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

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
