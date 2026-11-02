package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.skillbridge.entity.MensajeChatbot;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.MensajeChatbotRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock private AiChatGateway aiChatGateway;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ColaboradorRepository colaboradorRepository;
    @Mock private AsignacionRepository asignacionRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private MensajeChatbotRepository mensajeChatbotRepository;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(aiChatGateway, usuarioRepository,
                colaboradorRepository, asignacionRepository, proyectoRepository, mensajeChatbotRepository);
    }

    @Test
    void generaRespuestaConContextoDelUsuarioYGuardaAmbosTurnos() {
        Usuario usuario = usuarioAdministrador();
        when(usuarioRepository.findByCorreoIgnoreCase(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        when(mensajeChatbotRepository.findTop12ByUsuarioIdUsuarioOrderByIdMensajeChatbotDesc(1))
                .thenReturn(List.of());
        when(aiChatGateway.generar(any(), any())).thenReturn("Ve al módulo Usuarios (/admin/usuarios).");

        ChatbotService.Respuesta respuesta = chatbotService.responder(
                usuario.getCorreo(), "¿Dónde administro usuarios?");

        assertThat(respuesta.contenido()).contains("Usuarios");

        ArgumentCaptor<String> sistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pregunta = ArgumentCaptor.forClass(String.class);
        verify(aiChatGateway).generar(sistema.capture(), pregunta.capture());
        assertThat(sistema.getValue()).contains("Administrador Demo", "ADMINISTRADOR", "/admin");
        assertThat(pregunta.getValue()).contains("¿Dónde administro usuarios?");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MensajeChatbot>> mensajes = ArgumentCaptor.forClass(List.class);
        verify(mensajeChatbotRepository).saveAll(mensajes.capture());
        assertThat(mensajes.getValue())
                .extracting(MensajeChatbot::getRemitente)
                .containsExactly(MensajeChatbot.Remitente.USUARIO, MensajeChatbot.Remitente.ASISTENTE);
    }

    private Usuario usuarioAdministrador() {
        Rol rol = new Rol();
        rol.setNombre("ADMINISTRADOR");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setRol(rol);
        usuario.setNombres("Administrador");
        usuario.setApellidos("Demo");
        usuario.setCorreo("admin@skillbridge.com");
        return usuario;
    }
}
