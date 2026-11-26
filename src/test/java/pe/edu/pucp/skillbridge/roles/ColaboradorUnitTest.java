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
import pe.edu.pucp.skillbridge.controller.ComunicacionColaboradorController;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO.Nivel;
import pe.edu.pucp.skillbridge.dto.HabilidadPerfilForm;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.transaction.support.TransactionCallback;
import java.time.LocalDate;
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
    @Mock NotificacionRepository notificaciones;
    @Mock MensajeChatRepository mensajes;
    @Mock CapacidadColaboradorService capacidad;
    @Mock FotoPerfilService fotos;
    @Mock TransactionTemplate transacciones;
    @Mock EntityManager entidades;

    private ColaboradorController controlador;

    @BeforeEach
    void preparar() {
        controlador = new ColaboradorController(usuarios, colaboradores, habilidades, certificaciones,
                asignaciones, capacidad, fotos,
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
        HabilidadPerfilForm formulario = habilidad(Nivel.BASICO, "-1");
        var errores = new BeanPropertyBindingResult(formulario, "habilidadForm");
        var redirect = new RedirectAttributesModelMap();
        String vista = controlador.guardarHabilidad(formulario, errores, new ExtendedModelMap(), redirect);
        assertEquals("colaborador/habilidades", vista);
        assertTrue(errores.hasFieldErrors("aniosExperiencia"));
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void noRegistraHabilidadConNivelDesconocido() {
        configurarColaborador(12);
        HabilidadPerfilForm formulario = habilidad(null, "2");
        var errores = new BeanPropertyBindingResult(formulario, "habilidadForm");
        var redirect = new RedirectAttributesModelMap();
        errores.rejectValue("nivel", "typeMismatch");
        String vista = controlador.guardarHabilidad(formulario, errores, new ExtendedModelMap(), redirect);
        assertEquals("colaborador/habilidades", vista);
        assertEquals("typeMismatch", errores.getFieldError("nivel").getCode());
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void guardaHabilidadDisponibleQueAunNoTieneElColaborador() {
        configurarColaborador(12);
        ejecutarTransaccion();
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(nuevaHabilidad(true)));
        HabilidadPerfilForm formulario = habilidad(Nivel.AVANZADO, "4.5");
        var errores = new BeanPropertyBindingResult(formulario, "habilidadForm");
        var redirect = new RedirectAttributesModelMap();
        String vista = controlador.guardarHabilidad(formulario, errores, new ExtendedModelMap(), redirect);
        assertEquals("redirect:/colaborador/habilidades", vista);
        verify(colaboradores).guardarHabilidad(12, 3, "AVANZADO", new BigDecimal("4.5"));
        assertEquals("Habilidad agregada correctamente.", redirect.getFlashAttributes().get("mensajeExito"));
    }

    @Test
    void noAgregaHabilidadInactiva() {
        configurarColaborador(12);
        ejecutarTransaccion();
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(nuevaHabilidad(false)));
        HabilidadPerfilForm formulario = habilidad(Nivel.AVANZADO, "2");
        var errores = new BeanPropertyBindingResult(formulario, "habilidadForm");
        var redirect = new RedirectAttributesModelMap();
        String vista = controlador.guardarHabilidad(formulario, errores, new ExtendedModelMap(), redirect);
        assertEquals("colaborador/habilidades", vista);
        assertEquals("validation.habilidadPerfil.id.noDisponible", errores.getFieldError("idHabilidad").getCode());
        verify(colaboradores, never()).guardarHabilidad(any(), any(), any(), any());
    }

    @Test
    void noDuplicaUnaHabilidadYaRegistrada() {
        configurarColaborador(12);
        ejecutarTransaccion();
        when(habilidades.buscarParaActualizar(3)).thenReturn(Optional.of(nuevaHabilidad(true)));
        when(colaboradores.contarHabilidad(12, 3)).thenReturn(1L);
        HabilidadPerfilForm formulario = habilidad(Nivel.EXPERTO, "5");
        var errores = new BeanPropertyBindingResult(formulario, "habilidadForm");
        var redirect = new RedirectAttributesModelMap();
        String vista = controlador.guardarHabilidad(formulario, errores, new ExtendedModelMap(), redirect);
        assertEquals("colaborador/habilidades", vista);
        assertEquals("validation.habilidadPerfil.id.duplicada", errores.getFieldError("idHabilidad").getCode());
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
        assertEquals("colaborador/mis-proyectos", controlador.proyectos(1, Set.of("historial"), model));
        assertEquals(1, model.getAttribute("totalProyectos"));
        assertEquals(false, model.getAttribute("puedeVerHistorial"));
    }

    @Test
    void historialMuestraSoloProyectosConParticipacionesFinalizadas() {
        configurarColaborador(12);
        Asignacion vigente = asignacion(Proyecto.EstadoProyecto.ACTIVE, Asignacion.EstadoAsignacion.ACTIVA);
        Asignacion historica = asignacion(Proyecto.EstadoProyecto.COMPLETED, Asignacion.EstadoAsignacion.FINALIZADA);
        when(asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(12))
                .thenReturn(List.of(vigente, historica));
        Model model = new ExtendedModelMap();
        assertEquals("colaborador/historial", controlador.historial(Set.of(), model));
        var filas = (List<?>) model.getAttribute("filas");
        assertEquals(1, filas.size());
        assertSame(historica, ((FilaAsignacionDTO) filas.get(0)).asignacion());
    }

    @Test
    void marcarTodasLasNotificacionesActualizaSoloLasDelUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(22);
        var comunicacion = new ComunicacionColaboradorController(null, mensajes, notificaciones);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/colaborador/notificaciones", comunicacion.marcarTodas("TODAS", usuario, redirect));
        verify(notificaciones).marcarTodasLeidas(22);
    }

    private HabilidadPerfilForm habilidad(Nivel nivel, String anios) {
        HabilidadPerfilForm formulario = new HabilidadPerfilForm();
        formulario.setIdHabilidad(3);
        formulario.setNivel(nivel);
        formulario.setAniosExperiencia(anios);
        return formulario;
    }

    private void ejecutarTransaccion() {
        when(transacciones.execute(any())).thenAnswer(invocacion ->
                invocacion.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
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
        proyecto.setIdProyecto(estadoProyecto.ordinal() + 1);
        Asignacion a = new Asignacion();
        a.setProyecto(proyecto);
        a.setEstado(estado);
        a.setIdAsignacion(estado.ordinal() + 1);
        a.setFechaInicio(LocalDate.now().minusDays(2));
        return a;
    }
}
