package pe.edu.pucp.skillbridge.roles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import pe.edu.pucp.skillbridge.controller.AsignacionesGestionController;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.service.GestionAsignacionesService;
import java.util.Set;
import pe.edu.pucp.skillbridge.controller.ResourceManagerController;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceManagerUnitTest {
    @Mock ColaboradorRepository colaboradores;
    @Mock RecomendacionIARepository recomendaciones;
    @Mock AsignacionRepository asignaciones;
    @Mock ProyectoRepository proyectos;
    @Mock CapacidadColaboradorService capacidad;
    @Mock BindingResult validacion;
    @Mock GestionAsignacionesService gestion;
    private AsignacionesGestionController gestionController;

    private ResourceManagerController controlador;

    @BeforeEach
    void preparar() {
        gestionController = new AsignacionesGestionController(gestion, colaboradores, capacidad);
        controlador = new ResourceManagerController(colaboradores, recomendaciones,
                asignaciones, proyectos, capacidad);
    }

    @Test
    void dashboardCalculaDisponiblesSobrecargadosYPromedio() {
        when(capacidad.consultarEquipo()).thenReturn(List.of(
                carga(1, "Ana", 100, 20),
                carga(2, "Luis", 50, 70),
                carga(3, "Marta", 80, 40)));
        Model model = new ExtendedModelMap();
        assertEquals("resource/inicio", controlador.inicio("", Set.of("talento"), model));
        assertEquals(3, model.asMap().get("totalColaboradores"));
        assertEquals(1L, model.asMap().get("disponibles"));
        assertEquals(1L, model.asMap().get("sobrecargados"));
        assertEquals(43L, model.asMap().get("cargaPromedio"));
    }

    @Test
    void listarColaboradoresFiltraPorSeniority() {
        Colaborador senior = colaborador(1, Colaborador.Seniority.SENIOR);
        Colaborador junior = colaborador(2, Colaborador.Seniority.JUNIOR);
        when(colaboradores.listarConUsuario()).thenReturn(List.of(senior, junior));
        Model model = new ExtendedModelMap();
        assertEquals("resource/colaboradores", controlador.colaboradores("", "SENIOR", "TODOS", "TODOS", "1", Set.of(), model));
        assertEquals(List.of(senior), model.asMap().get("colaboradores"));
    }

    @Test
    void busquedaDeColaboradoresSeDelegaAlRepositorio() {
        Colaborador seleccionado = colaborador(3, Colaborador.Seniority.SEMI_SENIOR);
        when(colaboradores.buscar("Java")).thenReturn(List.of(seleccionado));
        Model model = new ExtendedModelMap();
        controlador.colaboradores("Java", "TODOS", "TODOS", "TODOS", "1", Set.of(), model);
        assertEquals(List.of(seleccionado), model.asMap().get("colaboradores"));
        verify(colaboradores).buscar("Java");
    }

    @Test
    void conteoDeAsignacionesDistingueActivasYPlanificadas() {
        when(asignaciones.findAll(Sort.by("idAsignacion"))).thenReturn(List.of(
                asignacion(Asignacion.EstadoAsignacion.ACTIVA),
                asignacion(Asignacion.EstadoAsignacion.PLANIFICADA),
                asignacion(Asignacion.EstadoAsignacion.FINALIZADA)));
        Model model = new ExtendedModelMap();
        assertEquals("resource/asignaciones", controlador.asignaciones("", "", "TODOS", model));
        assertEquals(1L, model.asMap().get("vigentes"));
        assertEquals(1L, model.asMap().get("planificadas"));
        assertEquals(1L, model.asMap().get("cerradas"));
    }

    @Test
    void calculoDeCargaPromedioMuestraCeroSinColaboradores() {
        when(capacidad.consultarCargaEquipo(eq(List.of()), any(LocalDate.class)))
                .thenReturn(new CapacidadColaboradorService.CargaEquipo(Map.of(), List.of()));
        Model model = new ExtendedModelMap();
        assertEquals("resource/carga", controlador.carga("", "TODOS", "", Set.of(), model));
        assertEquals(0L, model.asMap().get("cargaPromedio"));
    }

    @Test
    void disponibilidadClasificaElEquipoSegunLaCapacidadRestante() {
        when(capacidad.consultarCapacidades(eq(List.of()), any(LocalDate.class))).thenReturn(Map.of(
                1, carga(1, "Ana", 100, 20),
                2, carga(2, "Luis", 100, 80),
                3, carga(3, "Eva", 70, 90)));
        Model model = new ExtendedModelMap();
        assertEquals("resource/disponibilidad", controlador.disponibilidad("hoy", null, null, "", "TODOS", Set.of(), model));
        assertEquals(1L, model.asMap().get("disponibles"));
        assertEquals(1L, model.asMap().get("parciales"));
        assertEquals(1L, model.asMap().get("sinDisponibilidad"));
    }

    @Test
    void formularioValidoSeDelegaAlServicioConElGestor() {
        Usuario gestor = configurarGestor();
        AsignacionFormulario formulario = new AsignacionFormulario();
        formulario.setIdProyecto(8);
        formulario.setIdColaborador(6);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/resource/asignaciones", gestionController.guardar(
                formulario, validacion, gestor, new ExtendedModelMap(), redirect));
        verify(gestion).guardar(formulario, gestor);
    }

    @Test
    void formularioInvalidoNoSeEnviaAlServicio() {
        Usuario gestor = configurarGestor();
        when(validacion.hasErrors()).thenReturn(true);
        assertEquals("resource/asignacion-form", gestionController.guardar(
                new AsignacionFormulario(), validacion, gestor, new ExtendedModelMap(),
                new RedirectAttributesModelMap()));
        verify(gestion, never()).guardar(any(), any());
    }

    @Test
    void cambioDeEstadoValidoSeDelegaAlServicio() {
        Usuario gestor = configurarGestor();
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/resource/asignaciones",
                gestionController.cambiarEstado(5, "ACTIVA", gestor, redirect));
        verify(gestion).cambiarEstado(5, Asignacion.EstadoAsignacion.ACTIVA, gestor);
    }

    @Test
    void estadoDesconocidoSeRechazaSinInvocarAlServicio() {
        Usuario gestor = configurarGestor();
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/resource/asignaciones",
                gestionController.cambiarEstado(5, "DESCONOCIDO", gestor, redirect));
        assertEquals("Selecciona un estado de asignación válido.", redirect.getFlashAttributes().get("mensajeError"));
        verifyNoInteractions(gestion);
    }

    @Test
    void talentMatchingFiltraLasRecomendacionesPorProyecto() {
        Proyecto p1 = new Proyecto(); p1.setIdProyecto(1);
        Proyecto p2 = new Proyecto(); p2.setIdProyecto(2);
        RecomendacionIA r2 = new RecomendacionIA(); r2.setProyecto(p2);
        when(recomendaciones.findTop3ByProyectoIdProyectoOrderByPorcentajeMatchDescIdRecomendacionDesc(2))
                .thenReturn(List.of(r2));
        when(proyectos.findAll(Sort.by("nombre", "idProyecto"))).thenReturn(List.of(p1, p2));
        Model model = new ExtendedModelMap();
        assertEquals("resource/inicio", controlador.inicio("2", Set.of("capacidad"), model));
        assertEquals(List.of(r2), model.asMap().get("candidatos"));
    }

    private Usuario configurarGestor() {
        Usuario gestor = new Usuario();
        Rol rol = new Rol();
        rol.setNombre("RESOURCE_MANAGER");
        gestor.setRol(rol);
        return gestor;
    }

    private CargaColaboradorDTO carga(int id, String nombre, int base, int usada) {
        return new CargaColaboradorDTO(id, nombre, "Desarrollador", "TIC", base, usada, Math.max(0, base - usada));
    }

    private Colaborador colaborador(int id, Colaborador.Seniority seniority) {
        Colaborador c = new Colaborador();
        c.setIdColaborador(id);
        c.setSeniority(seniority);
        return c;
    }

    private Asignacion asignacion(Asignacion.EstadoAsignacion estado) {
        Asignacion a = new Asignacion();
        Proyecto proyecto = new Proyecto();
        proyecto.setEstado(Proyecto.EstadoProyecto.ACTIVE);
        a.setProyecto(proyecto);
        a.setEstado(estado);
        a.setFechaInicio(LocalDate.now().minusDays(1));
        return a;
    }
}
