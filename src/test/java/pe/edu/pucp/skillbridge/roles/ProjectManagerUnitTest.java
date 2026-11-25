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
import pe.edu.pucp.skillbridge.controller.ProjectManagerController;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectManagerUnitTest {
    @Mock UsuarioRepository usuarios;
    @Mock ProyectoRepository proyectos;
    @Mock AsignacionRepository asignaciones;
    @Mock ColaboradorRepository colaboradores;
    @Mock RecomendacionIARepository recomendaciones;
    @Mock ForoRepository foros;
    @Mock HabilidadRepository habilidades;
    @Mock RespuestaForoRepository respuestas;
    @Mock CapacidadColaboradorService capacidad;
    @Mock BindingResult validacion;

    private ProjectManagerController controlador;

    @BeforeEach
    void preparar() {
        controlador = new ProjectManagerController(usuarios, proyectos, asignaciones, colaboradores,
                recomendaciones, foros, habilidades, respuestas, capacidad);
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
        configurarGestor();
        Proyecto web = proyecto("Plataforma Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto movil = proyecto("Aplicación Móvil", Proyecto.EstadoProyecto.PLANNING);
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(web, movil));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyectos", controlador.proyectos("WEB", modelo));
        assertEquals(List.of(web), modelo.asMap().get("proyectos"));
        assertEquals(1L, modelo.asMap().get("activos"));
        assertEquals(0L, modelo.asMap().get("planning"));
    }

    @Test
    void dashboardCuentaEstadosYAsignacionesDelGestor() {
        configurarGestor();
        Proyecto activo = proyecto("Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto plan = proyecto("ERP", Proyecto.EstadoProyecto.PLANNING);
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(activo, plan));
        Colaborador persona = new Colaborador();
        persona.setIdColaborador(30);
        Asignacion a1 = new Asignacion(); a1.setColaborador(persona);
        Asignacion a2 = new Asignacion(); a2.setColaborador(persona);
        when(asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(10))
                .thenReturn(List.of(a1, a2));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/inicio", controlador.inicio(modelo));
        assertEquals(1L, modelo.asMap().get("proyectosActivos"));
        assertEquals(1L, modelo.asMap().get("proyectosPlanning"));
        assertEquals(2, modelo.asMap().get("asignaciones"));
        assertEquals(1L, modelo.asMap().get("colaboradores"));
    }

    @Test
    void formularioDeProyectoInvalidoNoSeGuarda() {
        configurarGestor();
        Proyecto nuevo = proyecto("Prueba", Proyecto.EstadoProyecto.PLANNING);
        when(validacion.hasErrors()).thenReturn(true);
        when(validacion.getAllErrors()).thenReturn(List.of(new ObjectError("proyecto", "Nombre obligatorio")));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyecto-form", controlador.guardarProyecto(nuevo, validacion, modelo));
        assertEquals("Nombre obligatorio", modelo.asMap().get("mensajeError"));
        assertEquals(10, nuevo.getProjectManager().getIdUsuario().intValue());
        verify(proyectos, never()).save(any());
    }

    @Test
    void proyectoValidoSeGuardaVinculadoAlProjectManager() {
        configurarGestor();
        Proyecto nuevo = proyecto("Nuevo portal", Proyecto.EstadoProyecto.ACTIVE);
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(nuevo));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/proyectos", controlador.guardarProyecto(nuevo, validacion, modelo));
        assertEquals(10, nuevo.getProjectManager().getIdUsuario().intValue());
        verify(proyectos).save(nuevo);
        assertEquals("Proyecto guardado correctamente.", modelo.asMap().get("mensajeExito"));
    }

    @Test
    void editarProyectoInexistenteProduceError() {
        when(proyectos.findById(404)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> controlador.editarProyecto(404, new ExtendedModelMap()));
    }

    @Test
    void cancelarProyectoRealizaBajaLogicaSinEliminarRegistros() {
        configurarGestor();
        Proyecto p = proyecto("Migración", Proyecto.EstadoProyecto.ACTIVE);
        when(proyectos.findById(7)).thenReturn(Optional.of(p));
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10))
                .thenReturn(List.of(p));
        assertEquals("pm/proyectos", controlador.cancelarProyecto(7, new ExtendedModelMap()));
        assertEquals(Proyecto.EstadoProyecto.CANCELLED, p.getEstado());
        verify(proyectos).save(p);
        verify(proyectos, never()).delete(any());
    }

    @Test
    void cambiarEstadoDeAsignacionSeGuardaCorrectamente() {
        configurarGestor();
        Asignacion a = new Asignacion();
        a.setEstado(Asignacion.EstadoAsignacion.PLANIFICADA);
        when(asignaciones.findById(5)).thenReturn(Optional.of(a));
        when(asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(10))
                .thenReturn(List.of(a));
        assertEquals("pm/asignaciones", controlador.cambiarEstadoAsignacion(5, "ACTIVA", new ExtendedModelMap()));
        assertEquals(Asignacion.EstadoAsignacion.ACTIVA, a.getEstado());
        verify(asignaciones).save(a);
    }

    @Test
    void estadoDeAsignacionInexistenteNoDebeGuardar() {
        Asignacion a = new Asignacion();
        when(asignaciones.findById(5)).thenReturn(Optional.of(a));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.cambiarEstadoAsignacion(5, "DESCONOCIDO", new ExtendedModelMap()));
        verify(asignaciones, never()).save(any());
    }


    @Test
    void asignacionValidaSeGuardaEnUnProyectoDelGestor() {
        configurarGestor();
        Proyecto proyecto = proyecto("Portal interno", Proyecto.EstadoProyecto.ACTIVE);
        proyecto.setIdProyecto(8);
        Colaborador colaborador = new Colaborador();
        colaborador.setIdColaborador(6);
        when(proyectos.findById(8)).thenReturn(Optional.of(proyecto));
        when(colaboradores.findById(6)).thenReturn(Optional.of(colaborador));
        when(asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(10))
                .thenReturn(List.of());
        Asignacion asignacion = new Asignacion();
        asignacion.setEstado(Asignacion.EstadoAsignacion.PLANIFICADA);
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/asignaciones", controlador.guardarAsignacion(
                asignacion, validacion, 8, 6, modelo));
        assertSame(proyecto, asignacion.getProyecto());
        assertSame(colaborador, asignacion.getColaborador());
        verify(asignaciones).save(asignacion);
    }

    @Test
    void asignacionConErroresNoDebePersistirse() {
        configurarGestor();
        Proyecto proyecto = proyecto("Portal interno", Proyecto.EstadoProyecto.ACTIVE);
        when(proyectos.findById(8)).thenReturn(Optional.of(proyecto));
        when(colaboradores.findById(6)).thenReturn(Optional.of(new Colaborador()));
        when(validacion.hasErrors()).thenReturn(true);
        when(validacion.getAllErrors()).thenReturn(List.of(new ObjectError("asignacion", "Fecha inválida")));
        when(proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(10)).thenReturn(List.of(proyecto));
        Model modelo = new ExtendedModelMap();
        assertEquals("pm/asignacion-form", controlador.guardarAsignacion(
                new Asignacion(), validacion, 8, 6, modelo));
        assertEquals("Fecha inválida", modelo.asMap().get("mensajeError"));
        verify(asignaciones, never()).save(any());
    }

    private void configurarGestor() {
        Usuario gestor = new Usuario();
        gestor.setIdUsuario(10);
        Rol rol = new Rol(); rol.setNombre("PROJECT_MANAGER");
        gestor.setRol(rol);
        when(usuarios.findFirstByRolNombreOrderByIdUsuarioAsc("PROJECT_MANAGER"))
                .thenReturn(Optional.of(gestor));
    }

    private Proyecto proyecto(String nombre, Proyecto.EstadoProyecto estado) {
        Proyecto p = new Proyecto();
        p.setNombre(nombre);
        p.setEstado(estado);
        return p;
    }
}
