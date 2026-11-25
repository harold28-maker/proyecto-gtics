package pe.edu.pucp.skillbridge.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Consulta la capacidad del día a partir de las asignaciones y su período de vigencia.
 */
@Service
@Transactional(readOnly = true)
public class CapacidadColaboradorService {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final List<Proyecto.EstadoProyecto> PROYECTOS_VIGENTES = List.of(
            Proyecto.EstadoProyecto.PLANNING, Proyecto.EstadoProyecto.ACTIVE, Proyecto.EstadoProyecto.ON_HOLD);

    private static final List<Asignacion.EstadoAsignacion> ESTADOS_CON_DEDICACION = List.of(
            Asignacion.EstadoAsignacion.ACTIVA, Asignacion.EstadoAsignacion.PLANIFICADA);

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

    public List<Colaborador> listarPerfilesActivos() {
        return colaboradores.listarConUsuario().stream()
                .filter(c -> c.getUsuario().getEstado() == Usuario.EstadoUsuario.ACTIVO)
                .toList();
    }

    public List<CargaColaboradorDTO> consultarEquipo() {
        return consultarCapacidades(colaboradores.listarConUsuario()).values().stream()
                .sorted(Comparator.comparingInt(CargaColaboradorDTO::getCarga).reversed()
                        .thenComparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CargaColaboradorDTO::getIdColaborador))
                .toList();
    }

    public Map<Integer, CargaColaboradorDTO> consultarCapacidades(List<Colaborador> perfiles) {
        return consultarCapacidades(perfiles, LocalDate.now(ZONA_HORARIA));
    }

    public Map<Integer, CargaColaboradorDTO> consultarCapacidades(List<Colaborador> perfiles, LocalDate fecha) {
        return consultarCargaEquipo(perfiles, fecha).capacidades();
    }

    public record CargaEquipo(Map<Integer, CargaColaboradorDTO> capacidades, List<Asignacion> asignacionesVigentes) {}

    public CargaEquipo consultarCargaEquipo(List<Colaborador> perfiles, LocalDate fecha) {
        if (perfiles.isEmpty()) return new CargaEquipo(Map.of(), List.of());

        List<Asignacion> vigentes = vigentesEn(fecha, null);
        Map<Integer, Integer> cargaPorColaborador = new HashMap<>();
        for (Asignacion asignacion : vigentes) {
            cargaPorColaborador.merge(asignacion.getColaborador().getIdColaborador(),
                    dedicacion(asignacion), Integer::sum);
        }
        Map<Integer, CargaColaboradorDTO> capacidades = perfiles.stream().collect(Collectors.toMap(
                Colaborador::getIdColaborador,
                c -> capacidad(c, cargaPorColaborador.getOrDefault(c.getIdColaborador(), 0))));
        List<Asignacion> asignacionesEquipo = vigentes.stream()
                .filter(a -> capacidades.containsKey(a.getColaborador().getIdColaborador()))
                .toList();
        return new CargaEquipo(capacidades, asignacionesEquipo);
    }

    public List<Asignacion> vigentesHoy() {
        return vigentesEn(LocalDate.now(ZONA_HORARIA), null);
    }

    public List<Asignacion> activasYPlanificadas() {
        return asignaciones.findByEstadoInAndProyectoEstadoIn(ESTADOS_CON_DEDICACION, PROYECTOS_VIGENTES);
    }

    public CargaColaboradorDTO consultarActual(Colaborador colaborador) {
        int carga = vigentesEn(LocalDate.now(ZONA_HORARIA), colaborador.getIdColaborador())
                .stream().mapToInt(this::dedicacion).sum();
        return capacidad(colaborador, carga);
    }

    public record CapacidadPeriodo(LocalDate inicio, LocalDate fin, int cargaMaxima, int disponible) {}

    public CapacidadPeriodo consultarPeriodo(Colaborador colaborador, LocalDate inicio, LocalDate fin,
                                            Integer idAsignacionEditada) {
        List<Asignacion> compromisos = asignaciones.buscarCompromisosEnPeriodo(List.of(colaborador.getIdColaborador()),
                ESTADOS_CON_DEDICACION, PROYECTOS_VIGENTES, inicio, fin);
        int carga = calcularCargaMaxima(compromisos, inicio, fin, idAsignacionEditada);
        return new CapacidadPeriodo(inicio, fin, carga, Math.max(capacidadBase(colaborador) - carga, 0));
    }

    public Map<Integer, CargaColaboradorDTO> consultarCapacidadesPeriodo(List<Colaborador> perfiles, LocalDate inicio,
                                                                        LocalDate fin) {
        if (perfiles.isEmpty()) return Map.of();

        List<Integer> ids = perfiles.stream().map(Colaborador::getIdColaborador).toList();
        Map<Integer, List<Asignacion>> compromisos = asignaciones.buscarCompromisosEnPeriodo(ids,
                        ESTADOS_CON_DEDICACION, PROYECTOS_VIGENTES, inicio, fin).stream()
                .collect(Collectors.groupingBy(a -> a.getColaborador().getIdColaborador()));

        return perfiles.stream().collect(Collectors.toMap(Colaborador::getIdColaborador, c -> capacidad(c,
                calcularCargaMaxima(compromisos.getOrDefault(c.getIdColaborador(), List.of()), inicio, fin, null))));
    }

    private int calcularCargaMaxima(List<Asignacion> compromisos, LocalDate inicio, LocalDate fin,
                                    Integer idAsignacionEditada) {
        // El fin es inclusivo. La dedicación se libera al día siguiente.
        Map<LocalDate, Integer> cambios = new TreeMap<>();
        for (Asignacion asignacion : compromisos) {
            if (idAsignacionEditada != null && idAsignacionEditada.equals(asignacion.getIdAsignacion())) continue;
            LocalDate desde = asignacion.getFechaInicio().isBefore(inicio) ? inicio : asignacion.getFechaInicio();
            int dedicacion = dedicacion(asignacion);
            cambios.merge(desde, dedicacion, Integer::sum);
            LocalDate hasta = asignacion.getFechaFin();
            if (hasta != null && (fin == null || hasta.isBefore(fin))) {
                cambios.merge(hasta.plusDays(1), -dedicacion, Integer::sum);
            }
        }
        int carga = 0;
        int cargaMaxima = 0;
        for (int cambio : cambios.values()) {
            carga += cambio;
            cargaMaxima = Math.max(cargaMaxima, carga);
        }
        return cargaMaxima;
    }

    static boolean ocupaCapacidad(Asignacion.EstadoAsignacion estado) {
        return estado != null && ESTADOS_CON_DEDICACION.contains(estado);
    }

    static boolean proyectoVigente(Proyecto proyecto) {
        return proyecto.getEstado() != null && PROYECTOS_VIGENTES.contains(proyecto.getEstado());
    }

    private List<Asignacion> vigentesEn(LocalDate fecha, Integer idColaborador) {
        return asignaciones.buscarVigentes(fecha, idColaborador, Asignacion.EstadoAsignacion.ACTIVA,
                PROYECTOS_VIGENTES);
    }

    private int dedicacion(Asignacion asignacion) {
        return asignacion.getPorcentajeDedicacion() == null ? 0 : asignacion.getPorcentajeDedicacion();
    }

    private int capacidadBase(Colaborador colaborador) {
        return colaborador.getDisponibilidadBase() == null ? 0 : colaborador.getDisponibilidadBase();
    }

    private CargaColaboradorDTO capacidad(Colaborador colaborador, int carga) {
        int base = capacidadBase(colaborador);
        return new CargaColaboradorDTO(colaborador.getIdColaborador(), colaborador.getUsuario().getNombreCompleto(),
                colaborador.getCargo() == null ? "-" : colaborador.getCargo(),
                colaborador.getArea() == null ? "-" : colaborador.getArea(),
                base, carga, Math.max(base - carga, 0));
    }
}
