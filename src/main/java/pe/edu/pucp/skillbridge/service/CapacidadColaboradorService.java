package pe.edu.pucp.skillbridge.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta la capacidad del día a partir de las asignaciones y su período de vigencia.
 */
@Service
@Transactional(readOnly = true)
public class CapacidadColaboradorService {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final List<Proyecto.EstadoProyecto> PROYECTOS_VIGENTES = List.of(
            Proyecto.EstadoProyecto.PLANNING, Proyecto.EstadoProyecto.ACTIVE, Proyecto.EstadoProyecto.ON_HOLD);

    private final AsignacionRepository asignaciones;
    private final ColaboradorRepository colaboradores;

    public CapacidadColaboradorService(AsignacionRepository asignaciones, ColaboradorRepository colaboradores) {
        this.asignaciones = asignaciones;
        this.colaboradores = colaboradores;
    }

    public record ResumenPersonal(LocalDate fecha, CargaColaboradorDTO capacidad, int totalVigentes,
                                  Asignacion asignacionPrincipal, Asignacion proximaAsignacion) {}

    public ResumenPersonal consultarPersonal(Colaborador colaborador) {
        LocalDate fecha = LocalDate.now(ZONA_HORARIA);
        Integer idColaborador = colaborador.getIdColaborador();
        List<Asignacion> vigentes = vigentesEn(fecha, idColaborador);
        int carga = vigentes.stream().mapToInt(this::dedicacion).sum();
        Asignacion principal = vigentes.stream().findFirst().orElse(null);
        Asignacion proxima = asignaciones.buscarProximas(idColaborador, fecha,
                Asignacion.EstadoAsignacion.PLANIFICADA, Asignacion.EstadoAsignacion.ACTIVA,
                PROYECTOS_VIGENTES, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        return new ResumenPersonal(fecha, capacidad(colaborador, carga), vigentes.size(), principal, proxima);
    }

    public List<CargaColaboradorDTO> consultarEquipo() {
        Map<Integer, Integer> cargaPorColaborador = new HashMap<>();
        for (Asignacion asignacion : vigentesHoy()) {
            cargaPorColaborador.merge(asignacion.getColaborador().getIdColaborador(),
                    dedicacion(asignacion), Integer::sum);
        }

        return colaboradores.listarConUsuario().stream()
                .map(c -> capacidad(c, cargaPorColaborador.getOrDefault(c.getIdColaborador(), 0)))
                .sorted(Comparator.comparingInt(CargaColaboradorDTO::getCarga).reversed()
                        .thenComparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CargaColaboradorDTO::getIdColaborador))
                .toList();
    }

    public List<Asignacion> vigentesHoy() {
        return vigentesEn(LocalDate.now(ZONA_HORARIA), null);
    }

    public List<Asignacion> activasYPlanificadas() {
        return asignaciones.findByEstadoInAndProyectoEstadoIn(
                List.of(Asignacion.EstadoAsignacion.ACTIVA, Asignacion.EstadoAsignacion.PLANIFICADA),
                PROYECTOS_VIGENTES);
    }

    private List<Asignacion> vigentesEn(LocalDate fecha, Integer idColaborador) {
        return asignaciones.buscarVigentes(fecha, idColaborador, Asignacion.EstadoAsignacion.ACTIVA,
                PROYECTOS_VIGENTES);
    }

    private int dedicacion(Asignacion asignacion) {
        return asignacion.getPorcentajeDedicacion() == null ? 0 : asignacion.getPorcentajeDedicacion();
    }

    private CargaColaboradorDTO capacidad(Colaborador colaborador, int carga) {
        int base = colaborador.getDisponibilidadBase() == null ? 0 : colaborador.getDisponibilidadBase();
        return new CargaColaboradorDTO(colaborador.getIdColaborador(), colaborador.getUsuario().getNombreCompleto(),
                colaborador.getCargo() == null ? "-" : colaborador.getCargo(),
                colaborador.getArea() == null ? "-" : colaborador.getArea(),
                base, carga, Math.max(base - carga, 0));
    }
}
