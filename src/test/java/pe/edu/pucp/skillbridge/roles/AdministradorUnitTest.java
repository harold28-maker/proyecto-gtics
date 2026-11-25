package pe.edu.pucp.skillbridge.roles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.entity.PermisoDeshabilitado;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository;
import pe.edu.pucp.skillbridge.repository.PermisoDeshabilitadoRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;
import pe.edu.pucp.skillbridge.service.GestionUsuarioService;
import pe.edu.pucp.skillbridge.service.PermisosRolService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AdministradorUnitTest {
    @Mock UsuarioRepository usuarios;
    @Mock PermisoDeshabilitadoRepository bloqueos;
    @Mock AuditoriaRepository auditoria;
    @Mock EntityManager entidades;

    private GestionUsuarioService autenticacion;
    private PermisosRolService permisos;

    @BeforeEach
    void preparar() {
        autenticacion = new GestionUsuarioService(usuarios);
        permisos = new PermisosRolService(bloqueos, auditoria, entidades);
    }

    @Test
    void usuarioAdministradorActivoPuedeAutenticarseConSuRol() {
        // Given: cuenta activa registrada con el rol ADMINISTRADOR.
        Usuario admin = usuario(Usuario.EstadoUsuario.ACTIVO, "ADMINISTRADOR");
        when(usuarios.findByCorreoIgnoreCase("admin@skillbridge.com")).thenReturn(Optional.of(admin));
        // When: Spring Security solicita los detalles de la cuenta.
        UserDetails resultado = autenticacion.loadUserByUsername("  admin@skillbridge.com  ");
        // Then: conserva el correo y recibe la autoridad del rol.
        assertEquals("admin@skillbridge.com", resultado.getUsername());
        assertTrue(resultado.isEnabled());
        assertTrue(resultado.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")));
    }

    @Test
    void usuarioInactivoNoQuedaHabilitadoParaIniciarSesion() {
        when(usuarios.findByCorreoIgnoreCase("admin@skillbridge.com"))
                .thenReturn(Optional.of(usuario(Usuario.EstadoUsuario.INACTIVO, "ADMINISTRADOR")));
        assertFalse(autenticacion.loadUserByUsername("admin@skillbridge.com").isEnabled());
    }

    @Test
    void usuarioBloqueadoQuedaConLaCuentaBloqueada() {
        when(usuarios.findByCorreoIgnoreCase("admin@skillbridge.com"))
                .thenReturn(Optional.of(usuario(Usuario.EstadoUsuario.BLOQUEADO, "ADMINISTRADOR")));
        assertFalse(autenticacion.loadUserByUsername("admin@skillbridge.com").isAccountNonLocked());
    }

    @Test
    void correoInexistenteGeneraErrorDeAutenticacion() {
        when(usuarios.findByCorreoIgnoreCase("desconocido@skillbridge.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> autenticacion.loadUserByUsername("desconocido@skillbridge.com"));
    }

    @Test
    void administradorTienePermisoParaGestionarUsuarios() {
        var permiso = permisos.permisoDeRuta("ADMINISTRADOR", "/admin/usuarios/nuevo");
        assertNotNull(permiso);
        assertEquals("usuarios", permiso.codigo());
    }

    @Test
    void administradorNoRecibePermisosDeGestionDeProyectosDelPm() {
        assertNull(permisos.permisoDeRuta("ADMINISTRADOR", "/pm/proyectos"));
    }

    @Test
    void administradorPuedeDescargarReporteDeAuditoriaSegunCatalogo() {
        assertEquals("auditoria", permisos.permisoDeReporte("ADMINISTRADOR", "AUDITORIA").codigo());
        assertNull(permisos.permisoDeReporte("ADMINISTRADOR", "talento"));
    }

    @Test
    void noSePuedeConfigurarUnRolInexistente() {
        when(entidades.find(eq(Rol.class), eq(99), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> permisos.guardar(99, "prueba", Set.of(), usuario(Usuario.EstadoUsuario.ACTIVO, "ADMINISTRADOR")));
        verifyNoInteractions(bloqueos, auditoria);
    }

    @Test
    void descripcionDelRolNoPuedeSuperarDoscientosCaracteres() {
        Rol rol = rol("ADMINISTRADOR");
        when(entidades.find(eq(Rol.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(rol);
        assertThrows(IllegalArgumentException.class,
                () -> permisos.guardar(1, "a".repeat(201), Set.of(), usuario(Usuario.EstadoUsuario.ACTIVO, "ADMINISTRADOR")));
        verifyNoInteractions(bloqueos, auditoria);
    }

    @Test
    void noSeAceptanPermisosQueNoExistenEnElCatalogoDelRol() {
        when(entidades.find(eq(Rol.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(rol("ADMINISTRADOR"));
        assertThrows(IllegalArgumentException.class,
                () -> permisos.guardar(1, null, Set.of("proyectos"), usuario(Usuario.EstadoUsuario.ACTIVO, "ADMINISTRADOR")));
        verifyNoInteractions(bloqueos, auditoria);
    }

    @Test
    void deshabilitarPermisosGuardaBloqueosYRegistraAuditoria() {
        Rol rol = rol("ADMINISTRADOR");
        when(entidades.find(eq(Rol.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(rol);
        when(bloqueos.findByRol_IdRol(1)).thenReturn(List.of());
        boolean modificado = permisos.guardar(1, null, Set.of(), usuario(Usuario.EstadoUsuario.ACTIVO, "ADMINISTRADOR"));
        assertTrue(modificado);
        verify(bloqueos, atLeastOnce()).save(any(PermisoDeshabilitado.class));
        verify(auditoria, atLeastOnce()).save(any(Auditoria.class));
    }

    private Rol rol(String nombre) {
        Rol r = new Rol();
        r.setIdRol(1);
        r.setNombre(nombre);
        return r;
    }

    private Usuario usuario(Usuario.EstadoUsuario estado, String nombreRol) {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setCorreo("admin@skillbridge.com");
        u.setPasswordHash("hash-de-prueba");
        u.setEstado(estado);
        u.setRol(rol(nombreRol));
        return u;
    }
}
