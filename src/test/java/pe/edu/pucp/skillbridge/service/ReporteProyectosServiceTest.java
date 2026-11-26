package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService.Dedicacion;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService.Grupo;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService.Tipo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteProyectosServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 2);
    private static final int RESPONSABLE = 44;

    private final AsignacionRepository asignaciones = mock(AsignacionRepository.class);
    private final ReporteProyectosService servicio =
            new ReporteProyectosService(asignaciones, mock(CapacidadColaboradorService.class));

    @Test
    void agrupaLaDedicacionVigenteYPlanificadaYExportaLosMismosValores() {
        Proyecto web = proyecto(1, "Plataforma Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto otro = proyecto(2, "Otro proyecto", Proyecto.EstadoProyecto.ACTIVE);
        Colaborador ana = colaborador(7, "Ana", "Torres");
        Colaborador carlos = colaborador(5, "Carlos", "Ramírez");
        asignaciones(
                asignacion(web, carlos, EstadoAsignacion.ACTIVA, 30, FECHA.minusDays(2), null),
                asignacion(web, ana, EstadoAsignacion.ACTIVA, 40, FECHA.minusDays(2), FECHA),
                asignacion(web, ana, EstadoAsignacion.PLANIFICADA, 20, FECHA, FECHA.plusDays(5)),
                asignacion(web, ana, EstadoAsignacion.PLANIFICADA, 10, FECHA, null),
                asignacion(otro, carlos, EstadoAsignacion.ACTIVA, 40, FECHA.minusDays(2), null));

        var datos = servicio.consultar(RESPONSABLE, List.of(web), FECHA, Grupo.TODAS, true);

        assertEquals(List.of("Ana Torres", "Carlos Ramírez"), datos.matriz().personas().stream()
                .map(ReporteAdministrativoService.Eje::nombre).toList());
        assertEquals(Map.of(7, new Dedicacion(40, 30), 5, new Dedicacion(30, 0)),
                datos.matriz().filas().get(0).dedicaciones());
        assertEquals(List.of(List.of("Plataforma Web", "Ana Torres", 0.4, 0.3),
                List.of("Plataforma Web", "Carlos Ramírez", 0.3, 0.0)), servicio.tabla(Tipo.DEDICACION, datos).filas());
        verify(asignaciones).findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(RESPONSABLE);
    }

    @Test
    void respetaLaFechaYElTipoDeAsignacionSeleccionados() {
        Proyecto web = proyecto(1, "Plataforma Web", Proyecto.EstadoProyecto.ACTIVE);
        Proyecto terminado = proyecto(2, "Terminado", Proyecto.EstadoProyecto.COMPLETED);
        Colaborador ana = colaborador(7, "Ana", "Torres");
        asignaciones(
                asignacion(web, ana, EstadoAsignacion.ACTIVA, 40, FECHA.minusDays(5), FECHA),
                asignacion(web, ana, EstadoAsignacion.ACTIVA, 20, FECHA.minusDays(5), FECHA.minusDays(1)),
                asignacion(web, ana, EstadoAsignacion.PLANIFICADA, 30, FECHA, null),
                asignacion(web, ana, EstadoAsignacion.PLANIFICADA, 15, FECHA.plusDays(1), null),
                asignacion(web, ana, EstadoAsignacion.CANCELADA, 30, FECHA.minusDays(5), null),
                asignacion(terminado, ana, EstadoAsignacion.ACTIVA, 10, FECHA.minusDays(5), null));

        var propias = List.of(web, terminado);
        var vigentes = servicio.consultar(RESPONSABLE, propias, FECHA, Grupo.VIGENTES, true);
        var planificadas = servicio.consultar(RESPONSABLE, propias, FECHA, Grupo.PLANIFICADAS, true);

        assertEquals(List.of(List.of("Plataforma Web", "Ana Torres", 0.4, 0.0)),
                servicio.tabla(Tipo.DEDICACION, vigentes).filas());
        assertEquals(List.of(List.of("Plataforma Web", "Ana Torres", 0.0, 0.3)),
                servicio.tabla(Tipo.DEDICACION, planificadas).filas());
        assertEquals("Terminado", servicio.tabla(Tipo.HISTORIAL, vigentes).filas().get(0).get(0));
    }

    private void asignaciones(Asignacion... filas) {
        when(asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(RESPONSABLE))
                .thenReturn(List.of(filas));
    }

    private Proyecto proyecto(int id, String nombre, Proyecto.EstadoProyecto estado) {
        Proyecto proyecto = new Proyecto();
        proyecto.setIdProyecto(id);
        proyecto.setNombre(nombre);
        proyecto.setEstado(estado);
        return proyecto;
    }

    private Colaborador colaborador(int id, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombre);
        usuario.setApellidos(apellido);
        Colaborador colaborador = new Colaborador();
        colaborador.setIdColaborador(id);
        colaborador.setUsuario(usuario);
        return colaborador;
    }

    private Asignacion asignacion(Proyecto proyecto, Colaborador colaborador, EstadoAsignacion estado,
                                  int porcentaje, LocalDate inicio, LocalDate fin) {
        Asignacion asignacion = new Asignacion();
        asignacion.setProyecto(proyecto);
        asignacion.setColaborador(colaborador);
        asignacion.setEstado(estado);
        asignacion.setPorcentajeDedicacion(porcentaje);
        asignacion.setFechaInicio(inicio);
        asignacion.setFechaFin(fin);
        return asignacion;
    }
}
