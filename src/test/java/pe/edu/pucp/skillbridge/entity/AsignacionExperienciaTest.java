package pe.edu.pucp.skillbridge.entity;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Proyecto.EstadoProyecto;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsignacionExperienciaTest {
    private final LocalDate hoy = LocalDate.of(2026, 11, 25);

    @Test
    void proyectoCompletadoReconoceUnaAsignacionIniciada() {
        assertTrue(asignacion(EstadoProyecto.COMPLETED, EstadoAsignacion.ACTIVA, hoy.minusDays(2))
                .registraExperiencia(hoy));
        assertFalse(asignacion(EstadoProyecto.COMPLETED, EstadoAsignacion.PLANIFICADA, hoy.minusDays(2))
                .registraExperiencia(hoy));
    }

    @Test
    void proyectoCanceladoReconoceLasParticipacionesQueYaFinalizaron() {
        assertTrue(asignacion(EstadoProyecto.CANCELLED, EstadoAsignacion.FINALIZADA, hoy.minusDays(2))
                .registraExperiencia(hoy));
        assertFalse(asignacion(EstadoProyecto.CANCELLED, EstadoAsignacion.ACTIVA, hoy.minusDays(2))
                .registraExperiencia(hoy));
    }

    @Test
    void unaAsignacionConInicioFuturoAunNoAcreditaExperiencia() {
        assertFalse(asignacion(EstadoProyecto.COMPLETED, EstadoAsignacion.FINALIZADA, hoy.plusDays(1))
                .registraExperiencia(hoy));
    }

    private Asignacion asignacion(EstadoProyecto estadoProyecto, EstadoAsignacion estado, LocalDate inicio) {
        var proyecto = new Proyecto();
        proyecto.setEstado(estadoProyecto);
        var asignacion = new Asignacion();
        asignacion.setProyecto(proyecto);
        asignacion.setEstado(estado);
        asignacion.setFechaInicio(inicio);
        return asignacion;
    }
}
