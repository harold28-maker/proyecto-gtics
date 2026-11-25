package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
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

    @BeforeEach
    void prepararAsignacion() {
        Rol rol = new Rol();
        rol.setNombre("RESOURCE_MANAGER");
        gestor.setRol(rol);
        Usuario cuenta = new Usuario();
        cuenta.setEstado(Usuario.EstadoUsuario.ACTIVO);
        colaborador.setUsuario(cuenta);
        Proyecto proyecto = new Proyecto();
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
