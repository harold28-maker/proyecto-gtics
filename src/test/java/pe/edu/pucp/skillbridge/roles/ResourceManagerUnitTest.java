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
import pe.edu.pucp.skillbridge.controller.ResourceManagerController;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.util.List;
import java.util.Optional;

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

    private ResourceManagerController controlador;

    @BeforeEach
    void preparar() {
        controlador = new ResourceManagerController(colaboradores, recomendaciones,
                asignaciones, proyectos, capacidad);
    }

    @Test
    void dashboardCalculaDisponiblesSobrecargadosYPromedio() {
        when(capacidad.consultarEquipo()).thenReturn(List.of(
                carga(1, "Ana", 100, 20),
                carga(2, "Luis", 50, 70),
                carga(3, "Marta", 80, 40)));
        when(recomendaciones.findAllByOrderByPorcentajeMatchDesc()).thenReturn(List.of());
        Model model = new ExtendedModelMap();
        assertEquals("resource/inicio", controlador.inicio(model));
        assertEquals(3, model.asMap().get("totalColaboradores"));
        assertEquals(1L, model.asMap().get("disponibles"));
        assertEquals(1L, model.asMap().get("sobrecargados"));
        assertEquals(43L, model.asMap().get("cargaPromedio"));
    }

    @Test
    void listarColaboradoresFiltraPorSeniority() {
        Colaborador senior = colaborador(1, Colaborador.Seniority.SENIOR);
        Colaborador junior = colaborador(2, Colaborador.Seniority.JUNIOR);
        when(colaboradores.findAll()).thenReturn(List.of(senior, junior));
        Model model = new ExtendedModelMap();
        assertEquals("resource/colaboradores", controlador.colaboradores(null, "SENIOR", model));
        assertEquals(List.of(senior), model.asMap().get("colaboradores"));
    }

    @Test
    void busquedaDeColaboradoresSeDelegaAlRepositorio() {
        Colaborador seleccionado = colaborador(3, Colaborador.Seniority.SEMI_SENIOR);
        when(colaboradores.buscar("Java")).thenReturn(List.of(seleccionado));
        Model model = new ExtendedModelMap();
        controlador.colaboradores("Java", "TODOS", model);
        assertEquals(List.of(seleccionado), model.asMap().get("colaboradores"));
        verify(colaboradores).buscar("Java");
    }

    @Test
    void conteoDeAsignacionesDistingueActivasYPlanificadas() {
        when(asignaciones.findAll()).thenReturn(List.of(
                asignacion(Asignacion.EstadoAsignacion.ACTIVA),
                asignacion(Asignacion.EstadoAsignacion.PLANIFICADA),
                asignacion(Asignacion.EstadoAsignacion.FINALIZADA)));
        Model model = new ExtendedModelMap();
        assertEquals("resource/asignaciones", controlador.asignaciones(model));
        assertEquals(1L, model.asMap().get("activas"));
        assertEquals(1L, model.asMap().get("planificadas"));
        assertEquals(1L, model.asMap().get("finalizadas"));
    }

    @Test
    void calculoDeCargaPromedioMuestraCeroSinColaboradores() {
        when(capacidad.consultarEquipo()).thenReturn(List.of());
        Model model = new ExtendedModelMap();
        assertEquals("resource/carga", controlador.carga(model));
        assertEquals(0, model.asMap().get("cargaPromedio"));
    }

    @Test
    void disponibilidadClasificaElEquipoSegunLaCapacidadRestante() {
        when(capacidad.consultarEquipo()).thenReturn(List.of(
                carga(1, "Ana", 100, 20), // 80 disponible
                carga(2, "Luis", 100, 80), // 20 parcial
                carga(3, "Eva", 70, 90)));// 0 sin disponibilidad
        Model model = new ExtendedModelMap();
        assertEquals("resource/disponibilidad", controlador.disponibilidad(model));
        assertEquals(1L, model.asMap().get("disponibles"));
        assertEquals(1L, model.asMap().get("parciales"));
        assertEquals(1L, model.asMap().get("sinDisponibilidad"));
    }

    @Test
    void asignacionValidaSeVinculaAlProyectoYColaborador() {
        Proyecto p = new Proyecto(); p.setIdProyecto(11);
        Colaborador c = colaborador(7, Colaborador.Seniority.JUNIOR);
        when(proyectos.findById(11)).thenReturn(Optional.of(p));
        when(colaboradores.findById(7)).thenReturn(Optional.of(c));
        when(asignaciones.findAll()).thenReturn(List.of());
        Asignacion nueva = asignacion(Asignacion.EstadoAsignacion.PLANIFICADA);
        Model model = new ExtendedModelMap();
        assertEquals("resource/asignaciones", controlador.guardarAsignacion(nueva, validacion, 11, 7, model));
        assertSame(p, nueva.getProyecto());
        assertSame(c, nueva.getColaborador());
        verify(asignaciones).save(nueva);
    }

    @Test
    void asignacionConErrorDeFormularioNoSeGuarda() {
        when(proyectos.findById(11)).thenReturn(Optional.of(new Proyecto()));
        when(colaboradores.findById(7)).thenReturn(Optional.of(new Colaborador()));
        when(validacion.hasErrors()).thenReturn(true);
        when(validacion.getAllErrors()).thenReturn(List.of(new ObjectError("asignacion", "Dedicación inválida")));
        Model model = new ExtendedModelMap();
        assertEquals("resource/asignacion-form", controlador.guardarAsignacion(
                new Asignacion(), validacion, 11, 7, model));
        assertEquals("Dedicación inválida", model.asMap().get("mensajeError"));
        verify(asignaciones, never()).save(any());
    }

    @Test
    void cambiarEstadoDeAsignacionRegistraLaActualizacion() {
        Asignacion a = asignacion(Asignacion.EstadoAsignacion.PLANIFICADA);
        when(asignaciones.findById(2)).thenReturn(Optional.of(a));
        when(asignaciones.findAll()).thenReturn(List.of(a));
        controlador.cambiarEstadoAsignacion(2, "ACTIVA", new ExtendedModelMap());
        assertEquals(Asignacion.EstadoAsignacion.ACTIVA, a.getEstado());
        verify(asignaciones).save(a);
    }

    @Test
    void estadoDeAsignacionNoReconocidoNoSeGuarda() {
        when(asignaciones.findById(2)).thenReturn(Optional.of(new Asignacion()));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.cambiarEstadoAsignacion(2, "INCORRECTO", new ExtendedModelMap()));
        verify(asignaciones, never()).save(any());
    }

    @Test
    void talentMatchingFiltraLasRecomendacionesPorProyecto() {
        Proyecto p1 = new Proyecto(); p1.setIdProyecto(1);
        Proyecto p2 = new Proyecto(); p2.setIdProyecto(2);
        RecomendacionIA r1 = new RecomendacionIA(); r1.setProyecto(p1);
        RecomendacionIA r2 = new RecomendacionIA(); r2.setProyecto(p2);
        when(recomendaciones.findAllByOrderByPorcentajeMatchDesc()).thenReturn(List.of(r1, r2));
        when(proyectos.findAll()).thenReturn(List.of(p1, p2));
        Model model = new ExtendedModelMap();
        assertEquals("resource/talent", controlador.talent(2, model));
        assertEquals(List.of(r2), model.asMap().get("recomendaciones"));
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
        a.setEstado(estado);
        return a;
    }
}
