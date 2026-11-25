package pe.edu.pucp.skillbridge.roles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceManagerCapacidadUnitTest {
    @Mock AsignacionRepository asignaciones;
    @Mock ColaboradorRepository colaboradores;

    private CapacidadColaboradorService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CapacidadColaboradorService(asignaciones, colaboradores);
    }

    @Test
    void sumaLasDedicacionesDeUnMismoColaborador() {
        Colaborador ana = colaborador(1, "Ana", 100);
        when(asignaciones.buscarVigentes(any(LocalDate.class), isNull(), eq(Asignacion.EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(asignacion(ana, 30), asignacion(ana, 20)));
        when(colaboradores.listarConUsuario()).thenReturn(List.of(ana));
        CargaColaboradorDTO dato = servicio.consultarEquipo().get(0);
        assertEquals(50, dato.getCarga().intValue());
        assertEquals(50, dato.getDisponibilidad().intValue());
    }

    @Test
    void disponibilidadNuncaEsNegativaCuandoLaCargaExcedeLaBase() {
        Colaborador ana = colaborador(1, "Ana", 60);
        when(asignaciones.buscarVigentes(any(LocalDate.class), isNull(), eq(Asignacion.EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(asignacion(ana, 80)));
        when(colaboradores.listarConUsuario()).thenReturn(List.of(ana));
        assertEquals(0, servicio.consultarEquipo().get(0).getDisponibilidad().intValue());
    }

    @Test
    void sinAsignacionesMantieneTodaLaCapacidadBase() {
        Colaborador ana = colaborador(1, "Ana", 75);
        when(colaboradores.listarConUsuario()).thenReturn(List.of(ana));
        CargaColaboradorDTO dato = servicio.consultarEquipo().get(0);
        assertEquals(0, dato.getCarga().intValue());
        assertEquals(75, dato.getDisponibilidad().intValue());
    }

    @Test
    void porcentajeNuloDeAsignacionSeInterpretaComoCero() {
        Colaborador ana = colaborador(1, "Ana", 80);
        when(asignaciones.buscarVigentes(any(LocalDate.class), isNull(), eq(Asignacion.EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(asignacion(ana, null)));
        when(colaboradores.listarConUsuario()).thenReturn(List.of(ana));
        assertEquals(0, servicio.consultarEquipo().get(0).getCarga().intValue());
    }

    @Test
    void ordenaColaboradoresPorMayorCargaActual() {
        Colaborador ana = colaborador(1, "Ana", 100);
        Colaborador luis = colaborador(2, "Luis", 100);
        when(asignaciones.buscarVigentes(any(LocalDate.class), isNull(), eq(Asignacion.EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(asignacion(ana, 20), asignacion(luis, 75)));
        when(colaboradores.listarConUsuario()).thenReturn(List.of(ana, luis));
        assertEquals(List.of(2, 1), servicio.consultarEquipo().stream()
                .map(CargaColaboradorDTO::getIdColaborador).toList());
    }

    @Test
    void resumenPersonalConsultaSoloLasAsignacionesDelColaborador() {
        Colaborador ana = colaborador(3, "Ana", 100);
        when(asignaciones.buscarVigentes(any(LocalDate.class), eq(3), eq(Asignacion.EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(asignacion(ana, 35)));
        var resumen = servicio.consultarPersonal(ana);
        assertEquals(1, resumen.totalVigentes());
        assertEquals(35, resumen.capacidad().getCarga().intValue());
        assertEquals(65, resumen.capacidad().getDisponibilidad().intValue());
        verify(asignaciones).buscarProximas(eq(3), any(LocalDate.class),
                eq(Asignacion.EstadoAsignacion.PLANIFICADA), eq(Asignacion.EstadoAsignacion.ACTIVA),
                anyList(), any(Pageable.class));
    }

    private Colaborador colaborador(int id, String nombre, Integer base) {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombre);
        usuario.setApellidos("Prueba");
        Colaborador c = new Colaborador();
        c.setIdColaborador(id);
        c.setUsuario(usuario);
        c.setDisponibilidadBase(base);
        return c;
    }

    private Asignacion asignacion(Colaborador c, Integer porcentaje) {
        Asignacion a = new Asignacion();
        a.setColaborador(c);
        a.setPorcentajeDedicacion(porcentaje);
        return a;
    }
}
