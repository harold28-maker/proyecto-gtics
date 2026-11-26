package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GestionAsignacionesServiceTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);

    private final AsignacionRepository asignaciones = mock(AsignacionRepository.class);
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final ColaboradorRepository colaboradores = mock(ColaboradorRepository.class);
    private final CapacidadColaboradorService capacidad = mock(CapacidadColaboradorService.class);
    private final GestionAsignacionesService servicio =
            new GestionAsignacionesService(asignaciones, proyectos, colaboradores, capacidad);

    private final Usuario gestor = new Usuario();
    private final Colaborador colaborador = new Colaborador();
    private final Proyecto proyecto = new Proyecto();

    @BeforeEach
    void prepararAsignacion() {
        Rol rol = new Rol();
        rol.setNombre("RESOURCE_MANAGER");
        gestor.setRol(rol);
        Usuario cuenta = new Usuario();
        cuenta.setEstado(Usuario.EstadoUsuario.ACTIVO);
        colaborador.setIdColaborador(7);
        colaborador.setUsuario(cuenta);
        proyecto.setEstado(Proyecto.EstadoProyecto.ACTIVE);
        when(proyectos.findById(5)).thenReturn(Optional.of(proyecto));
        when(colaboradores.buscarParaActualizar(7)).thenReturn(Optional.of(colaborador));
        when(capacidad.consultarPeriodo(colaborador, INICIO, null, null))
                .thenReturn(new CapacidadColaboradorService.CapacidadPeriodo(INICIO, null, 40, 40));
    }

    @Test
    void aceptaUnaDedicacionIgualALaDisponible() {
        servicio.guardar(formulario(40), gestor);

        verify(asignaciones).save(argThat(asignacion -> asignacion.getPorcentajeDedicacion() == 40
                && INICIO.equals(asignacion.getFechaInicio())));
    }

    @Test
    void rechazaUnaDedicacionQueSuperaLoDisponibleAntesDeGuardar() {
        var error = assertThrows(GestionAsignacionesService.CampoInvalido.class,
                () -> servicio.guardar(formulario(41), gestor));

        assertEquals("porcentajeDedicacion", error.getCampo());
        verifyNoInteractions(asignaciones);
    }

    @Test
    void rechazaCrearUnaAsignacionCerrada() {
        AsignacionFormulario formulario = formulario(40);
        formulario.setEstado(EstadoAsignacion.FINALIZADA);

        var error = assertThrows(GestionAsignacionesService.CampoInvalido.class,
                () -> servicio.guardar(formulario, gestor));

        assertEquals("estado", error.getCampo());
        verifyNoInteractions(asignaciones);
    }

    @Test
    void rechazaFinalizarUnaAsignacionPlanificada() {
        Asignacion asignacion = asignacionPlanificada();

        var error = assertThrows(GestionAsignacionesService.CampoInvalido.class,
                () -> servicio.cambiarEstado(9, EstadoAsignacion.FINALIZADA, gestor));

        assertEquals("estado", error.getCampo());
        assertEquals(EstadoAsignacion.PLANIFICADA, asignacion.getEstado());
    }

    @Test
    void compruebaLaCapacidadVigenteAntesDeActivarUnaPlanificada() {
        Asignacion asignacion = asignacionPlanificada();
        when(capacidad.consultarPeriodo(colaborador, INICIO, null, 9))
                .thenReturn(new CapacidadColaboradorService.CapacidadPeriodo(INICIO, null, 60, 20));

        var error = assertThrows(GestionAsignacionesService.CampoInvalido.class,
                () -> servicio.cambiarEstado(9, EstadoAsignacion.ACTIVA, gestor));

        assertEquals("porcentajeDedicacion", error.getCampo());
        assertEquals(EstadoAsignacion.PLANIFICADA, asignacion.getEstado());
        verify(capacidad).consultarPeriodo(colaborador, INICIO, null, 9);
    }

    @Test
    void elFormularioDeEdicionConservaElEstadoRegistrado() {
        Asignacion asignacion = asignacionPlanificada();
        AsignacionFormulario formulario = formulario(40);
        formulario.setIdAsignacion(9);
        formulario.setEstado(EstadoAsignacion.CANCELADA);

        var error = assertThrows(GestionAsignacionesService.CampoInvalido.class,
                () -> servicio.guardar(formulario, gestor));

        assertEquals("estado", error.getCampo());
        assertEquals(EstadoAsignacion.PLANIFICADA, asignacion.getEstado());
    }

    private Asignacion asignacionPlanificada() {
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(9);
        asignacion.setProyecto(proyecto);
        asignacion.setColaborador(colaborador);
        asignacion.setFechaInicio(INICIO);
        asignacion.setPorcentajeDedicacion(40);
        when(asignaciones.buscarParaActualizar(9)).thenReturn(Optional.of(asignacion));
        return asignacion;
    }

    private AsignacionFormulario formulario(int dedicacion) {
        AsignacionFormulario formulario = new AsignacionFormulario();
        formulario.setIdColaborador(7);
        formulario.setIdProyecto(5);
        formulario.setFechaInicio(INICIO);
        formulario.setRolProyecto("Desarrollo");
        formulario.setPorcentajeDedicacion(dedicacion);
        return formulario;
    }
}
