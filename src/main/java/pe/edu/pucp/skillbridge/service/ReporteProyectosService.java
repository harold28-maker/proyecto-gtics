package pe.edu.pucp.skillbridge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.TablaReporte;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.service.ReporteAdministrativoService.Eje;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReporteProyectosService {
    private final AsignacionRepository asignaciones;
    private final CapacidadColaboradorService capacidad;

    public ReporteProyectosService(AsignacionRepository asignaciones, CapacidadColaboradorService capacidad) {
        this.asignaciones = asignaciones;
        this.capacidad = capacidad;
    }

    public enum Grupo {
        TODAS("Vigentes y planificadas"), VIGENTES("Vigentes"), PLANIFICADAS("Planificadas");

        private final String etiqueta;
        Grupo(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    public enum Tipo {
        DEDICACION("dedicacion", "Dedicación por proyecto", "asignaciones"),
        PROYECTOS("proyectos", "Avance de proyectos", "proyectos"),
        ASIGNACIONES("asignaciones", "Asignaciones por proyecto", "asignaciones"),
        CARGA("carga", "Carga de trabajo", "disponibilidad"),
        HISTORIAL("historial", "Historial", "historial");

        private final String clave;
        private final String titulo;
        private final String permiso;

        Tipo(String clave, String titulo, String permiso) {
            this.clave = clave;
            this.titulo = titulo;
            this.permiso = permiso;
        }

        public String getClave() { return clave; }
        public String getTitulo() { return titulo; }
        public String getPermiso() { return permiso; }

        public static Tipo desde(String clave) {
            return Arrays.stream(values()).filter(tipo -> tipo.clave.equalsIgnoreCase(clave)).findFirst().orElse(null);
        }
    }

    public record Dedicacion(int vigente, int planificada) {
        Dedicacion sumar(Dedicacion otra) { return new Dedicacion(vigente + otra.vigente, planificada + otra.planificada); }
    }
    public record FilaProyecto(Proyecto proyecto, Map<Integer, Dedicacion> dedicaciones) {}
    public record Matriz(List<Eje> personas, List<FilaProyecto> filas) {}
    public record Datos(LocalDate fecha, List<Proyecto> proyectos, List<Asignacion> asignaciones, Matriz matriz) {}

    public Datos consultar(Integer responsable, List<Proyecto> proyectos, LocalDate fecha, Grupo grupo,
                           boolean incluirAsignaciones) {
        List<Asignacion> filas = List.of();
        if (incluirAsignaciones && !proyectos.isEmpty()) {
            Set<Integer> ids = proyectos.stream().map(Proyecto::getIdProyecto).collect(Collectors.toSet());
            filas = asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(responsable).stream()
                    .filter(a -> ids.contains(a.getProyecto().getIdProyecto()))
                    .filter(a -> incluir(a, fecha, grupo)).toList();
        }
        return new Datos(fecha, proyectos, filas, agrupar(proyectos, filas));
    }

    private boolean incluir(Asignacion a, LocalDate fecha, Grupo grupo) {
        if (a.getPorcentajeDedicacion() == null || a.getPorcentajeDedicacion() <= 0) return false;
        if (a.getEstado() == EstadoAsignacion.ACTIVA) return grupo != Grupo.PLANIFICADAS && a.estaVigente(fecha);
        return grupo != Grupo.VIGENTES && a.getEstado() == EstadoAsignacion.PLANIFICADA
                && a.getFechaInicio() != null && !a.getFechaInicio().isAfter(fecha) && !a.esHistorica(fecha);
    }

    private Matriz agrupar(List<Proyecto> proyectos, List<Asignacion> asignaciones) {
        Map<Integer, Eje> personas = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Dedicacion>> porProyecto = new LinkedHashMap<>();
        for (Asignacion asignacion : asignaciones) {
            Colaborador persona = asignacion.getColaborador();
            Integer id = persona.getIdColaborador();
            personas.putIfAbsent(id, new Eje(id, persona.getUsuario().getNombreCompleto()));
            int porcentaje = asignacion.getPorcentajeDedicacion();
            Dedicacion valor = asignacion.getEstado() == EstadoAsignacion.ACTIVA
                    ? new Dedicacion(porcentaje, 0) : new Dedicacion(0, porcentaje);
            porProyecto.computeIfAbsent(asignacion.getProyecto().getIdProyecto(), clave -> new LinkedHashMap<>())
                    .merge(id, valor, Dedicacion::sumar);
        }
        return new Matriz(personas.values().stream()
                .sorted(Comparator.comparing(Eje::nombre, String.CASE_INSENSITIVE_ORDER).thenComparing(Eje::id)).toList(),
                proyectos.stream().filter(p -> porProyecto.containsKey(p.getIdProyecto()))
                        .map(p -> new FilaProyecto(p, Map.copyOf(porProyecto.get(p.getIdProyecto())))).toList());
    }

    public TablaReporte tabla(Tipo tipo, Datos datos) {
        return switch (tipo) {
            case DEDICACION -> new TablaReporte("Dedicación", List.of("Proyecto", "Colaborador", "Vigente", "Planificada"),
                    datos.matriz().filas().stream().flatMap(f -> datos.matriz().personas().stream()
                            .filter(p -> f.dedicaciones().containsKey(p.id())).map(p -> {
                                Dedicacion valor = f.dedicaciones().get(p.id());
                                return List.<Object>of(f.proyecto().getNombre(), p.nombre(),
                                        valor.vigente() / 100.0, valor.planificada() / 100.0);
                            })).toList());
            case PROYECTOS, HISTORIAL -> new TablaReporte(tipo == Tipo.HISTORIAL ? "Historial" : "Proyectos",
                    List.of("Proyecto", "Estado", "Prioridad", "Avance", "Inicio", "Fin", "Personas requeridas", "Tecnologías"),
                    datos.proyectos().stream().filter(p -> tipo == Tipo.PROYECTOS || p.isCerrado())
                            .map(p -> Arrays.<Object>asList(p.getNombre(), p.getEstado() == null ? null : p.getEstado().getEtiqueta(),
                                    p.getPrioridad() == null ? null : p.getPrioridad().getEtiqueta(),
                                    p.getProgreso() == null ? null : p.getProgreso() / 100.0, p.getFechaInicio(), p.getFechaFin(),
                                    p.getVacantes(), p.getTecnologias())).toList());
            case ASIGNACIONES -> new TablaReporte("Asignaciones", List.of("Proyecto", "Colaborador", "Rol", "Dedicación", "Estado", "Inicio", "Fin"),
                    datos.asignaciones().stream().map(a -> Arrays.<Object>asList(a.getProyecto().getNombre(),
                            a.getColaborador().getUsuario().getNombreCompleto(), a.getRolProyecto(),
                            a.getPorcentajeDedicacion() / 100.0, a.getEstado().getEtiqueta(), a.getFechaInicio(), a.getFechaFin())).toList());
            case CARGA -> tablaCarga(datos);
        };
    }

    private TablaReporte tablaCarga(Datos datos) {
        var equipo = datos.asignaciones().stream().map(Asignacion::getColaborador)
                .collect(Collectors.toMap(Colaborador::getIdColaborador, c -> c, (a, b) -> a, LinkedHashMap::new));
        var cargas = capacidad.consultarCapacidades(List.copyOf(equipo.values()), datos.fecha());
        return new TablaReporte("Carga de trabajo", List.of("Colaborador", "Capacidad base", "Carga vigente", "Disponible"),
                cargas.values().stream().sorted(Comparator.comparing(CargaColaboradorDTO::getColaborador,
                                String.CASE_INSENSITIVE_ORDER).thenComparing(CargaColaboradorDTO::getIdColaborador))
                        .map(c -> List.<Object>of(c.getColaborador(), c.getDisponibilidadBase() / 100.0,
                                c.getCarga() / 100.0, c.getDisponibilidad() / 100.0)).toList());
    }
}
