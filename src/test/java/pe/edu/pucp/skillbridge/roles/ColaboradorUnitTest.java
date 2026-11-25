package pe.edu.pucp.skillbridge.roles;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.controller.ColaboradorController;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;
import pe.edu.pucp.skillbridge.service.FotoPerfilService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ColaboradorUnitTest {
    @Mock UsuarioRepository usuarios;
    @Mock ColaboradorRepository colaboradores;
    @Mock HabilidadRepository habilidades;
    @Mock CertificacionRepository certificaciones;
    @Mock AsignacionRepository asignaciones;
    @Mock ForoRepository foros;
    @Mock NotificacionRepository notificaciones;
    @Mock MensajeChatRepository mensajes;
    @Mock RespuestaForoRepository respuestas;
    @Mock CapacidadColaboradorService capacidad;
    @Mock FotoPerfilService fotos;
    @Mock TransactionTemplate transacciones;
    @Mock EntityManager entidades;

    private ColaboradorController controlador;

    @BeforeEach
    void preparar() {
        controlador = new ColaboradorController(usuarios, colaboradores, habilidades, certificaciones,
                asignaciones, foros, notificaciones, mensajes, respuestas, capacidad, fotos,
                transacciones, entidades);
    }

    @AfterEach
    void limpiarSolicitudSimulada() {
        // Evitamos que la identidad simulada afecte a otros tests.
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void noPermiteAbrirPerfilSinUnaCuentaDeColaboradorEnLaSolicitud() {
        assertEquals("redirect:/colaborador/inicio", controlador.perfil(Set.of(), new ExtendedModelMap()));
        verifyNoInteractions(colaboradores);
    }

    @Test
    void perfilSoloConsultaSeccionesNoBloqueadas() {
        Colaborador propio = configurarColaborador(12);
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/perfil", controlador.perfil(
                Set.of("certificaciones", "habilidades", "asignaciones"), model));
        assertSame(propio, model.asMap().get("colaborador"));
        verifyNoInteractions(certificaciones, capacidad);
        verify(colaboradores, never()).obtenerHabilidades(any());
    }

    @Test
    void perfilCargaSoloLasCertificacionesDelColaboradorAutenticado() {
        configurarColaborador(12);
        when(certificaciones.findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(12))
                .thenReturn(List.of(new Certificacion()));
        Model model = new ExtendedModelMap();
        controlador.perfil(Set.of("habilidades", "asignaciones"), model);
        assertEquals(1, ((List<?>) model.asMap().get("certificaciones")).size());
        verify(certificaciones).findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(12);
    }

    @Test
    void formularioDePerfilRecuperaDatosDeLaCuentaActual() {
        configurarColaborador(12);
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/perfil-form", controlador.editarPerfil(model));
        assertEquals("María", model.asMap().get("nombresPerfil"));
        assertEquals("González", model.asMap().get("apellidosPerfil"));
        assertEquals("999111222", model.asMap().get("telefonoPerfil"));
    }

    @Test
    void noRegistraHabilidadConExperienciaNegativa() {
        configurarColaborador(12);
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/habilidades", controlador.guardarHabilidad(
                3, "BASICO", new BigDecimal("-1"), model));
        assertTrue(((String) model.asMap().get("mensajeError")).contains("entre 0 y 50"));
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void noRegistraHabilidadConNivelDesconocido() {
        configurarColaborador(12);
        Model model = new ExtendedModelMap();
        controlador.guardarHabilidad(3, "MAESTRO", new BigDecimal("2"), model);
        assertEquals("Selecciona un nivel válido.", model.asMap().get("mensajeError"));
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void guardaHabilidadDisponibleQueAunNoTieneElColaborador() {
        configurarColaborador(12);
        Habilidad h = nuevaHabilidad(true);
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(h));
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/habilidades", controlador.guardarHabilidad(
                3, "AVANZADO", new BigDecimal("4.5"), model));
        verify(colaboradores).guardarHabilidad(12, 3, "AVANZADO", new BigDecimal("4.5"));
        assertEquals("Habilidad agregada correctamente.", model.asMap().get("mensajeExito"));
    }

    @Test
    void noAgregaHabilidadInactiva() {
        configurarColaborador(12);
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(nuevaHabilidad(false)));
        Model model = new ExtendedModelMap();
        controlador.guardarHabilidad(3, "AVANZADO", new BigDecimal("2"), model);
        assertTrue(((String) model.asMap().get("mensajeError")).contains("ya no está disponible"));
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void noDuplicaUnaHabilidadYaRegistrada() {
        configurarColaborador(12);
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(nuevaHabilidad(true)));
        when(colaboradores.obtenerHabilidades(12)).thenReturn(List.<Object[]>of(
                new Object[]{3, "Java", "Backend", "AVANZADO", new BigDecimal("3")}));
        Model model = new ExtendedModelMap();
        controlador.guardarHabilidad(3, "EXPERTO", new BigDecimal("5"), model);
        assertTrue(((String) model.asMap().get("mensajeError")).contains("ya forma parte de tu perfil"));
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void proyectosPropiosExcluyenHistoricosCuandoPermisoHistorialEstaBloqueado() {
        configurarColaborador(12);
        Asignacion vigente = asignacion(Proyecto.EstadoProyecto.ACTIVE, Asignacion.EstadoAsignacion.ACTIVA);
        Asignacion historica = asignacion(Proyecto.EstadoProyecto.COMPLETED, Asignacion.EstadoAsignacion.FINALIZADA);
        when(asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(12))
                .thenReturn(List.of(vigente, historica));
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/proyectos", controlador.proyectos(Set.of("historial"), model));
        assertEquals(List.of(vigente), model.asMap().get("asignaciones"));
    }

    @Test
    void historialMuestraSoloProyectosConParticipacionesFinalizadas() {
        configurarColaborador(12);
        Asignacion vigente = asignacion(Proyecto.EstadoProyecto.ACTIVE, Asignacion.EstadoAsignacion.ACTIVA);
        Asignacion historica = asignacion(Proyecto.EstadoProyecto.COMPLETED, Asignacion.EstadoAsignacion.FINALIZADA);
        when(asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(12))
                .thenReturn(List.of(vigente, historica));
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/proyectos", controlador.historial(model));
        assertEquals(List.of(historica), model.asMap().get("asignaciones"));
    }

    @Test
    void marcarTodasLasNotificacionesActualizaSoloLasDelUsuarioAutenticado() {
        configurarColaborador(12);
        Notificacion primera = new Notificacion(); primera.setLeida(false);
        Notificacion segunda = new Notificacion(); segunda.setLeida(false);
        when(notificaciones.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(22))
                .thenReturn(List.of(primera, segunda));
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/notificaciones", controlador.marcarTodasLeidas(model));
        assertTrue(primera.getLeida());
        assertTrue(segunda.getLeida());
        assertEquals(0L, model.asMap().get("noLeidas"));
        verify(notificaciones).saveAll(List.of(primera, segunda));
    }

    private Colaborador configurarColaborador(int id) {
        Usuario u = new Usuario();
        u.setIdUsuario(22);
        u.setNombres("María");
        u.setApellidos("González");
        u.setTelefono("999111222");
        Colaborador c = new Colaborador();
        c.setIdColaborador(id);
        c.setUsuario(u);
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.setAttribute("usuarioVerificado", u);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));
        when(colaboradores.findByUsuarioIdUsuario(22)).thenReturn(Optional.of(c));
        return c;
    }

    private Habilidad nuevaHabilidad(boolean activa) {
        Habilidad h = new Habilidad();
        h.setIdHabilidad(3);
        h.setEstado(activa);
        return h;
    }

    private Asignacion asignacion(Proyecto.EstadoProyecto estadoProyecto, Asignacion.EstadoAsignacion estado) {
        Proyecto proyecto = new Proyecto();
        proyecto.setEstado(estadoProyecto);
        Asignacion a = new Asignacion();
        a.setProyecto(proyecto);
        a.setEstado(estado);
        return a;
    }
}
