package pe.edu.pucp.skillbridge.roles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import pe.edu.pucp.skillbridge.controller.AsignacionesGestionController;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.service.GestionAsignacionesService;
import java.util.Set;
import pe.edu.pucp.skillbridge.controller.ProjectManagerController;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectManagerUnitTest {
    @Mock ProyectoRepository proyectos;
    @Mock AsignacionRepository asignaciones;
    @Mock ColaboradorRepository colaboradores;
    @Mock HabilidadRepository habilidades;
    @Mock CapacidadColaboradorService capacidad;
    @Mock BindingResult validacion;
    @Mock GestionAsignacionesService gestion;
    private AsignacionesGestionController gestionController;

    private ProjectManagerController controlador;

    @BeforeEach
    void preparar() {
        gestionController = new AsignacionesGestionController(gestion, colaboradores, capacidad);
        controlador = new ProjectManagerController(proyectos, asignaciones, colaboradores,
                habilidades, capacidad, mock(AuditoriaRepository.class));
    }

    @Test
    void proyectoNuevoIniciaPlanificadoConPrioridadMediaYProgresoCero() {
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyecto-form", controlador.nuevoProyecto(modelo));
        Proyecto nuevo = (Proyecto) modelo.asMap().get("proyecto");
        assertEquals(Proyecto.EstadoProyecto.PLANNING, nuevo.getEstado());
        assertEquals(Proyecto.Prioridad.MEDIA, nuevo.getPrioridad());
        assertEquals(0, nuevo.getProgreso().intValue());
    }

    @Test
    void listaSolamenteLosProyectosDelGestorYFiltraPorNombre() {
        Usuario gestor = configurarGestor();
        Proyecto web = proyecto("Plataforma Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto movil = proyecto("Aplicación Móvil", Proyecto.EstadoProyecto.PLANNING);
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(web, movil));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyectos", controlador.proyectos("WEB", "", "", gestor, modelo));
        assertEquals(List.of(web), modelo.asMap().get("proyectos"));
        assertEquals(1L, modelo.asMap().get("activos"));
        assertEquals(1L, modelo.asMap().get("planificacion"));
    }

    @Test
    void dashboardCuentaEstadosYAsignacionesDelGestor() {
        Usuario gestor = configurarGestor();
        Proyecto activo = proyecto("Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto plan = proyecto("ERP", Proyecto.EstadoProyecto.PLANNING);
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(activo, plan));
        Colaborador persona = new Colaborador();
        persona.setIdColaborador(30);
        Asignacion a1 = new Asignacion(); a1.setColaborador(persona);
        Asignacion a2 = new Asignacion(); a2.setColaborador(persona);
        for (Asignacion asignacion : List.of(a1, a2)) {
            asignacion.setProyecto(activo);
            asignacion.setEstado(Asignacion.EstadoAsignacion.ACTIVA);
            asignacion.setFechaInicio(LocalDate.now().minusDays(1));
        }
        when(asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(10))
                .thenReturn(List.of(a1, a2));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/inicio", controlador.inicio(gestor, Set.of(), modelo));
        assertEquals(1, modelo.asMap().get("activos"));
        assertEquals(1L, modelo.asMap().get("planificacion"));
        assertEquals(2, modelo.asMap().get("vigentes"));
    }

    @Test
    void formularioDeProyectoInvalidoNoSeGuarda() {
        Usuario gestor = configurarGestor();
        Proyecto nuevo = proyecto("Prueba", Proyecto.EstadoProyecto.PLANNING);
        when(validacion.hasErrors()).thenReturn(true);
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyecto-form", controlador.guardarProyecto(nuevo, validacion, gestor, modelo, new RedirectAttributesModelMap()));
        verify(proyectos, never()).save(any());
    }

    @Test
    void proyectoValidoSeGuardaVinculadoAlProjectManager() {
        Usuario gestor = configurarGestor();
        Proyecto nuevo = proyecto("Nuevo portal", Proyecto.EstadoProyecto.ACTIVE);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/pm/proyectos", controlador.guardarProyecto(
                nuevo, validacion, gestor, new ExtendedModelMap(), redirect));
        verify(proyectos).save(argThat(p -> p.getProjectManager() == gestor && "Nuevo portal".equals(p.getNombre())));
        assertEquals("Proyecto guardado correctamente.", redirect.getFlashAttributes().get("mensajeExito"));
    }

    @Test
    void editarProyectoInexistenteProduceError() {
        when(proyectos.findById(404)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> controlador.editarProyecto(404, configurarGestor(), new ExtendedModelMap()));
    }

    @Test
    void cancelarProyectoRealizaBajaLogicaSinEliminarRegistros() {
        Usuario gestor = configurarGestor();
        Proyecto p = proyecto("Migración", Proyecto.EstadoProyecto.ACTIVE);
        when(proyectos.buscarPropioParaActualizar(7, 10)).thenReturn(Optional.of(p));
        assertEquals("redirect:/pm/proyectos",
                controlador.cancelarProyecto(7, gestor, new RedirectAttributesModelMap()));
        assertEquals(Proyecto.EstadoProyecto.CANCELLED, p.getEstado());
        verify(proyectos, never()).delete(any());
    }

    @Test
    void cambioDeEstadoValidoSeDelegaAlServicio() {
        Usuario gestor = configurarGestor();
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/pm/asignaciones",
                gestionController.cambiarEstado(5, "ACTIVA", gestor, redirect));
        verify(gestion).cambiarEstado(5, Asignacion.EstadoAsignacion.ACTIVA, gestor);
    }

    @Test
    void estadoDesconocidoSeRechazaSinInvocarAlServicio() {
        Usuario gestor = configurarGestor();
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/pm/asignaciones",
                gestionController.cambiarEstado(5, "DESCONOCIDO", gestor, redirect));
        assertEquals("Selecciona un estado de asignación válido.", redirect.getFlashAttributes().get("mensajeError"));
        verifyNoInteractions(gestion);
    }


    @Test
    void formularioValidoSeDelegaAlServicioConElGestor() {
        Usuario gestor = configurarGestor();
        AsignacionFormulario formulario = new AsignacionFormulario();
        formulario.setIdProyecto(8);
        formulario.setIdColaborador(6);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/pm/asignaciones", gestionController.guardar(
                formulario, validacion, gestor, new ExtendedModelMap(), redirect));
        verify(gestion).guardar(formulario, gestor);
    }

    @Test
    void formularioInvalidoNoSeEnviaAlServicio() {
        Usuario gestor = configurarGestor();
        when(validacion.hasErrors()).thenReturn(true);
        assertEquals("pm/asignacion-form", gestionController.guardar(
                new AsignacionFormulario(), validacion, gestor, new ExtendedModelMap(),
                new RedirectAttributesModelMap()));
        verify(gestion, never()).guardar(any(), any());
    }

    private Usuario configurarGestor() {
        Usuario gestor = new Usuario();
        gestor.setIdUsuario(10);
        Rol rol = new Rol(); rol.setNombre("PROJECT_MANAGER");
        gestor.setRol(rol);
        return gestor;
    }

    private Proyecto proyecto(String nombre, Proyecto.EstadoProyecto estado) {
        Proyecto p = new Proyecto();
        p.setNombre(nombre);
        p.setEstado(estado);
        return p;
    }
}
