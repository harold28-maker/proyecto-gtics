package pe.edu.pucp.skillbridge.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.Clasificacion;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.EstadoCarga;
import pe.edu.pucp.skillbridge.dto.TablaReporte;
import pe.edu.pucp.skillbridge.entity.Proyecto.EstadoProyecto;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService.CargaEquipo;
import pe.edu.pucp.skillbridge.service.ReporteAdministrativoService.DedicacionProyecto;
import pe.edu.pucp.skillbridge.service.ReporteAdministrativoService.MarcaDedicacion;
import pe.edu.pucp.skillbridge.service.SesionTalentoService.Consulta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class ReporteRecursosService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Comparator<CargaColaboradorDTO> ORDEN_CARGA = Comparator
            .comparingInt(CargaColaboradorDTO::getCarga).reversed()
            .thenComparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CargaColaboradorDTO::getIdColaborador);

    private final CapacidadColaboradorService capacidad;
    private final AsignacionRepository asignaciones;
    private final ProyectoRepository proyectos;
    private final ReporteAdministrativoService dedicacion;

    public ReporteRecursosService(CapacidadColaboradorService capacidad, AsignacionRepository asignaciones,
                                 ProyectoRepository proyectos, ReporteAdministrativoService dedicacion) {
        this.capacidad = capacidad;
        this.asignaciones = asignaciones;
        this.proyectos = proyectos;
        this.dedicacion = dedicacion;
    }

    public record Resumen(List<CargaColaboradorDTO> cargas, long disponibles, long parciales,
                          long sinDisponibilidad, long sobrecargados, long cargaPromedio,
                          double finDisponibles, double finParciales, int asignacionesVigentes,
                          long proyectosActivos, Grafico grafico) {}

    public record Grafico(List<DedicacionProyecto> proyectos, List<Columna> columnas,
                          List<MarcaDedicacion> marcas, int anchoMinimo) {}

    public record Columna(Integer idColaborador, String colaborador, int total, double altura, List<Segmento> segmentos) {}

    public record Segmento(String proyecto, int valor, double altura, String color) {}

    public Resumen consultarResumen(LocalDate hoy, boolean verCapacidad, boolean verAsignaciones,
                                     boolean verProyectos) {
        CargaEquipo equipo = verCapacidad || verAsignaciones ? consultarEquipo(hoy)
                : new CargaEquipo(Map.of(), List.of());
        List<CargaColaboradorDTO> cargas = verCapacidad ? ordenar(equipo) : List.of();
        long disponibles = contar(cargas, Clasificacion.DISPONIBLE);
        long parciales = contar(cargas, Clasificacion.PARCIAL);
        double finDisponibles = cargas.isEmpty() ? 0 : 100.0 * disponibles / cargas.size();
        double finParciales = cargas.isEmpty() ? 0 : 100.0 * (disponibles + parciales) / cargas.size();
        long sobrecargados = cargas.stream().filter(c -> c.getEstadoCarga() == EstadoCarga.SOBRECARGA).count();
        long promedio = Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0));
        int vigentes = verAsignaciones ? equipo.asignacionesVigentes().size() : 0;
        long activos = verProyectos ? proyectos.countByEstado(EstadoProyecto.ACTIVE) : 0;
        Grafico grafico = verCapacidad && verAsignaciones ? grafico(equipo, cargas) : null;
        return new Resumen(cargas, disponibles, parciales, contar(cargas, Clasificacion.SIN_DISPONIBILIDAD),
                sobrecargados, promedio, finDisponibles, finParciales, vigentes, activos, grafico);
    }

    private long contar(List<CargaColaboradorDTO> cargas, Clasificacion clasificacion) {
        return cargas.stream().filter(c -> c.getClasificacion() == clasificacion).count();
    }

    private CargaEquipo consultarEquipo(LocalDate hoy) {
        return capacidad.consultarCargaEquipo(capacidad.listarPerfilesActivos(), hoy);
    }

    private List<CargaColaboradorDTO> ordenar(CargaEquipo equipo) {
        return equipo.capacidades().values().stream().sorted(ORDEN_CARGA).toList();
    }

    private Grafico grafico(CargaEquipo equipo, List<CargaColaboradorDTO> cargas) {
        var datos = dedicacion.agruparDedicacion(equipo.asignacionesVigentes());
        int maximo = cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).max().orElse(0);
        int paso = Math.max(25, (int) Math.ceil(maximo / 20.0) * 5);
        int limite = 4 * paso;
        List<MarcaDedicacion> marcas = IntStream.rangeClosed(0, 4)
                .mapToObj(i -> new MarcaDedicacion(limite - i * paso, i * 25.0))
                .toList();
        List<Columna> columnas = new ArrayList<>();
        for (CargaColaboradorDTO carga : cargas) {
            int total = carga.getCarga();
            Integer id = carga.getIdColaborador();
            List<Segmento> segmentos = new ArrayList<>();
            for (DedicacionProyecto proyecto : datos.proyectos()) {
                int valor = proyecto.porcentajes().getOrDefault(id, 0);
                if (valor > 0) {
                    segmentos.add(new Segmento(proyecto.nombre(), valor, 100.0 * valor / total, proyecto.color()));
                }
            }
            columnas.add(new Columna(id, carga.getColaborador(), total, 100.0 * total / limite, List.copyOf(segmentos)));
        }
        return new Grafico(datos.proyectos(), List.copyOf(columnas), marcas, 52 + 63 * columnas.size());
    }

    public TablaReporte tablaCapacidad(LocalDate hoy) {
        return tablaCapacidad(ordenar(consultarEquipo(hoy)));
    }

    public TablaReporte tablaCapacidad(List<CargaColaboradorDTO> cargas) {
        var filas = cargas.stream().map(c -> List.<Object>of(c.getColaborador(), texto(c.getCargo()),
                texto(c.getArea()), porcentaje(c.getDisponibilidadBase()), porcentaje(c.getCarga()),
                porcentaje(c.getDisponibilidad()))).toList();
        return new TablaReporte("Capacidad y carga",
                List.of("Colaborador", "Cargo", "Área", "Capacidad base (%)", "Carga vigente (%)", "Disponible (%)"), filas);
    }

    public TablaReporte tablaAsignaciones() {
        var filas = asignaciones.findAll(Sort.by("idAsignacion")).stream()
                .map(a -> List.<Object>of(a.getProyecto().getNombre(), a.getColaborador().getUsuario().getNombreCompleto(),
                        texto(a.getRolProyecto()), porcentaje(a.getPorcentajeDedicacion()), a.getEstado().getEtiqueta(),
                        valor(a.getFechaInicio()), valor(a.getFechaFin())))
                .toList();
        return new TablaReporte("Asignaciones",
                List.of("Proyecto", "Colaborador", "Rol", "Dedicación (%)", "Estado", "Inicio", "Fin"), filas);
    }

    public TablaReporte tablaProyectos() {
        var filas = proyectos.findAll(Sort.by("nombre", "idProyecto")).stream()
                .map(p -> List.<Object>of(p.getNombre(), estadoProyecto(p.getEstado()), texto(p.getPrioridad()),
                        porcentaje(p.getProgreso()), valor(p.getFechaInicio()), valor(p.getFechaFin())))
                .toList();
        return new TablaReporte("Proyectos", List.of("Proyecto", "Estado", "Prioridad", "Avance (%)", "Inicio", "Fin"), filas);
    }

    public TablaReporte tablaTalento(Consulta consulta, boolean verCapacidad, LocalDate hoy) {
        List<String> columnas = new ArrayList<>(List.of("Proyecto", "Colaborador", "Cargo", "Nivel", "Coincidencia",
                "Fecha o período evaluado"));
        if (verCapacidad) columnas.add("Disponible (%)");
        columnas.add("Criterios y evidencia");
        List<List<Object>> filas = new ArrayList<>();
        if (consulta != null) {
            String periodo = consulta.desde() == null ? FECHA.format(hoy)
                    : FECHA.format(consulta.desde()) + " al " + FECHA.format(consulta.hasta());
            for (var candidato : consulta.resultado().candidatos()) {
                List<Object> fila = new ArrayList<>(List.of(consulta.proyecto(), candidato.nombre(),
                        texto(candidato.cargo()), candidato.nivel(), candidato.completo() ? "Completa" : "Parcial", periodo));
                if (verCapacidad) fila.add(porcentaje(candidato.disponible()));
                fila.add(candidato.evidencias().stream().map(e -> e.etiqueta() + " · "
                                + (e.cumple() ? "Cumple" : "Pendiente") + " · " + e.motivo())
                        .collect(Collectors.joining("\n")));
                filas.add(List.copyOf(fila));
            }
        }
        return new TablaReporte("Talent Matching", List.copyOf(columnas), List.copyOf(filas));
    }

    private String estadoProyecto(EstadoProyecto estado) {
        return switch (estado) {
            case PLANNING -> "En planificación";
            case ACTIVE -> "En curso";
            case ON_HOLD -> "En pausa";
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
        };
    }

    private Object porcentaje(Integer entero) {
        return entero == null ? "" : entero / 100.0;
    }

    private Object valor(Object dato) {
        return dato == null ? "" : dato;
    }

    private String texto(Object dato) {
        return valor(dato).toString();
    }
}
