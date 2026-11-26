package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.EstadoCarga;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapacidadEquipoTest {
    @Test
    void calculaCargaYDetalleALaMismaFechaYComparaCadaCapacidadBase() {
        AsignacionRepository asignaciones = mock(AsignacionRepository.class);
        CapacidadColaboradorService servicio = new CapacidadColaboradorService(asignaciones, mock(ColaboradorRepository.class));
        LocalDate hoy = LocalDate.of(2026, 11, 25);
        Colaborador conMargen = colaborador(1, 90);
        Colaborador alLimite = colaborador(2, 80);
        Colaborador conSobrecarga = colaborador(3, 80);
        Colaborador sinCapacidad = colaborador(4, 0);
        List<Asignacion> vigentes = List.of(
                asignacion(conMargen, 70), asignacion(alLimite, 80),
                asignacion(conSobrecarga, 60), asignacion(conSobrecarga, 50));
        Asignacion deOtroPerfil = asignacion(colaborador(5, 100), 100);
        when(asignaciones.buscarVigentes(eq(hoy), isNull(), eq(EstadoAsignacion.ACTIVA), anyList()))
                .thenReturn(List.of(vigentes.get(0), vigentes.get(1), vigentes.get(2), vigentes.get(3), deOtroPerfil));

        var equipo = servicio.consultarCargaEquipo(List.of(conMargen, alLimite, conSobrecarga, sinCapacidad), hoy);

        assertEquals(vigentes, equipo.asignacionesVigentes());
        assertEquals(EstadoCarga.CON_MARGEN, equipo.capacidades().get(1).getEstadoCarga());
        assertEquals(20, equipo.capacidades().get(1).getDisponibilidad());
        assertEquals(EstadoCarga.AL_LIMITE, equipo.capacidades().get(2).getEstadoCarga());
        assertEquals(EstadoCarga.SOBRECARGA, equipo.capacidades().get(3).getEstadoCarga());
        assertEquals(110, equipo.capacidades().get(3).getCarga());
        assertEquals(0, equipo.capacidades().get(3).getDisponibilidad());
        assertEquals(EstadoCarga.SIN_CAPACIDAD, equipo.capacidades().get(4).getEstadoCarga());
        verify(asignaciones).buscarVigentes(eq(hoy), isNull(), eq(EstadoAsignacion.ACTIVA), anyList());
    }

    private Colaborador colaborador(int id, int base) {
        Colaborador colaborador = new Colaborador();
        colaborador.setIdColaborador(id);
        colaborador.setDisponibilidadBase(base);
        colaborador.setUsuario(new Usuario());
        return colaborador;
    }

    private Asignacion asignacion(Colaborador colaborador, int dedicacion) {
        Asignacion asignacion = new Asignacion();
        asignacion.setColaborador(colaborador);
        asignacion.setPorcentajeDedicacion(dedicacion);
        return asignacion;
    }
}
