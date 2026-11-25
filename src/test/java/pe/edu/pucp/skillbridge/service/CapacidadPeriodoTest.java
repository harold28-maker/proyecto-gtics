package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapacidadPeriodoTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN = INICIO.plusDays(14);

    private final AsignacionRepository asignaciones = mock(AsignacionRepository.class);
    private final CapacidadColaboradorService servicio =
            new CapacidadColaboradorService(asignaciones, mock(ColaboradorRepository.class));
    private final Colaborador colaborador = new Colaborador();

    @BeforeEach
    void prepararColaborador() {
        colaborador.setIdColaborador(7);
        colaborador.setDisponibilidadBase(80);
    }

    @Test
    void tomaLaMayorCargaDeLosPeriodosQueNoCoinciden() {
        compromisos(FIN,
                asignacion(1, INICIO.minusDays(5), INICIO.plusDays(6), 40),
                asignacion(2, INICIO.plusDays(7), FIN, 50));

        var periodo = servicio.consultarPeriodo(colaborador, INICIO, FIN, null);

        assertEquals(50, periodo.cargaMaxima());
        assertEquals(30, periodo.disponible());
    }

    @Test
    void sumaLaDedicacionCuandoUnaTerminaElDiaEnQueOtraInicia() {
        compromisos(FIN,
                asignacion(1, INICIO, INICIO.plusDays(6), 40),
                asignacion(2, INICIO.plusDays(6), FIN, 30));

        var periodo = servicio.consultarPeriodo(colaborador, INICIO, FIN, null);

        assertEquals(70, periodo.cargaMaxima());
        assertEquals(10, periodo.disponible());
    }

    @Test
    void consideraCompromisosFuturosAlConsultarUnPeriodoSinFechaDeFin() {
        compromisos(null,
                asignacion(1, INICIO.minusDays(2), null, 40),
                asignacion(2, INICIO.plusDays(20), INICIO.plusDays(25), 50));

        var periodo = servicio.consultarPeriodo(colaborador, INICIO, null, null);

        assertEquals(90, periodo.cargaMaxima());
        assertEquals(0, periodo.disponible());
    }

    @Test
    void excluyeLaAsignacionQueSeEstaEditando() {
        compromisos(FIN,
                asignacion(1, INICIO, FIN, 50),
                asignacion(2, INICIO, FIN, 30));

        var periodo = servicio.consultarPeriodo(colaborador, INICIO, FIN, 1);

        assertEquals(30, periodo.cargaMaxima());
        assertEquals(50, periodo.disponible());
    }

    @Test
    void calculaLaCargaMaximaDeCadaColaboradorSinMezclarSusAsignaciones() {
        colaborador.setUsuario(new Usuario());
        Colaborador otro = new Colaborador();
        otro.setIdColaborador(8);
        otro.setDisponibilidadBase(100);
        otro.setUsuario(new Usuario());

        Asignacion activa = asignacion(1, INICIO, FIN, 40);
        activa.setColaborador(colaborador);
        Asignacion planificada = asignacion(2, INICIO.plusDays(3), FIN, 30);
        planificada.setColaborador(colaborador);
        Asignacion compromisoOtro = asignacion(3, INICIO, FIN, 20);
        compromisoOtro.setColaborador(otro);
        when(asignaciones.buscarCompromisosEnPeriodo(eq(List.of(7, 8)),
                eq(List.of(Asignacion.EstadoAsignacion.ACTIVA, Asignacion.EstadoAsignacion.PLANIFICADA)),
                anyList(), eq(INICIO), eq(FIN))).thenReturn(List.of(activa, planificada, compromisoOtro));

        var equipo = servicio.consultarCapacidadesPeriodo(List.of(colaborador, otro), INICIO, FIN);

        assertEquals(70, equipo.get(7).getCarga());
        assertEquals(10, equipo.get(7).getDisponibilidad());
        assertEquals(20, equipo.get(8).getCarga());
        assertEquals(80, equipo.get(8).getDisponibilidad());
    }

    private void compromisos(LocalDate fin, Asignacion... valores) {
        when(asignaciones.buscarCompromisosEnPeriodo(eq(List.of(7)),
                eq(List.of(Asignacion.EstadoAsignacion.ACTIVA, Asignacion.EstadoAsignacion.PLANIFICADA)),
                anyList(), eq(INICIO), eq(fin))).thenReturn(List.of(valores));
    }

    private Asignacion asignacion(int id, LocalDate desde, LocalDate hasta, int dedicacion) {
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(id);
        asignacion.setFechaInicio(desde);
        asignacion.setFechaFin(hasta);
        asignacion.setPorcentajeDedicacion(dedicacion);
        return asignacion;
    }
}
