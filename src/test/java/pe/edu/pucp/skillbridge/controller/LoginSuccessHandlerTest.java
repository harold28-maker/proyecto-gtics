package pe.edu.pucp.skillbridge.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSuccessHandlerTest {

    @ParameterizedTest
    @CsvSource({
            "ADMINISTRADOR,Administrador,/admin/inicio",
            "PROJECT_MANAGER,Project Manager,/pm/inicio",
            "RESOURCE_MANAGER,Resource Manager,/resource/inicio",
            "COLABORADOR,Colaborador,/colaborador/inicio"
    })
    void registraElIngresoYConservaLaRedireccionDeCadaRol(String nombreRol, String etiqueta, String ruta) throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
        Rol rol = new Rol();
        rol.setNombre(nombreRol);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setCorreo("prueba@example.test");
        usuario.setRol(rol);
        when(usuarios.findByCorreoIgnoreCase(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                usuario.getCorreo(), null, List.of(new SimpleGrantedAuthority("ROLE_" + nombreRol)));
        var solicitud = new MockHttpServletRequest();
        var respuesta = new MockHttpServletResponse();

        new LoginSuccessHandler(usuarios, auditoria).onAuthenticationSuccess(solicitud, respuesta, autenticacion);

        var evento = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoria).save(evento.capture());
        assertThat(evento.getValue().getUsuario()).isSameAs(usuario);
        assertThat(evento.getValue().getAccion()).isEqualTo(Auditoria.ACCION_INICIAR_SESION);
        assertThat(evento.getValue().getModulo()).isEqualTo(Auditoria.MODULO_AUTENTICACION);
        assertThat(evento.getValue().getDetalle()).isEqualTo(etiqueta);
        assertThat(respuesta.getRedirectedUrl()).isEqualTo(ruta);
    }
}
