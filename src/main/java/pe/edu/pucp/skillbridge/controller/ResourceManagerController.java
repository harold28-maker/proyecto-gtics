package pe.edu.pucp.skillbridge.controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.Clasificacion;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.RecomendacionIA;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.RecomendacionIARepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controlador MVC que administra recursos, asignaciones, disponibilidad,
 * talento, historial y reportes.
 */
@Controller
@RequestMapping("/resource")
public class ResourceManagerController {

    private static final int COLABORADORES_POR_PAGINA = 6;

    private final ColaboradorRepository colaboradorRepository;
    private final RecomendacionIARepository recomendacionIARepository;
    private final AsignacionRepository asignacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final CapacidadColaboradorService capacidadColaboradorService;

    /**
     * Crea el controlador e inicializa los repositorios necesarios para las operaciones de recursos.
     *
     * @param colaboradorRepository repositorio de colaboradores
     * @param recomendacionIARepository repositorio de recomendaciones de IA
     * @param asignacionRepository repositorio de asignaciones
     * @param proyectoRepository repositorio de proyectos
     * @param capacidadColaboradorService servicio de carga vigente
     */
    public ResourceManagerController(ColaboradorRepository colaboradorRepository,
                                     RecomendacionIARepository recomendacionIARepository,
                                     AsignacionRepository asignacionRepository,
                                     ProyectoRepository proyectoRepository,
                                     CapacidadColaboradorService capacidadColaboradorService) {
        this.colaboradorRepository = colaboradorRepository;
        this.recomendacionIARepository = recomendacionIARepository;
        this.asignacionRepository = asignacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.capacidadColaboradorService = capacidadColaboradorService;
    }

    /**
     * Presenta el resumen principal del dashboard de gestión de recursos.
     *
     * @param model modelo utilizado para cargar los datos de la vista
     * @return nombre de la plantilla del dashboard
     */
    @RequestMapping(value = {"/inicio", "/dashboard"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(@RequestParam(value = "proyecto", defaultValue = "") String proyecto,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        List<CargaColaboradorDTO> cargas = bloqueados.contains("capacidad")
                ? List.of() : capacidadColaboradorService.consultarEquipo();
        model.addAttribute("titulo", "Inicio");
        model.addAttribute("totalColaboradores", cargas.size());
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0 : Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("cargas", cargas.stream().limit(6).toList());
        model.addAttribute("capacidadPorColaborador", cargas.stream()
                .collect(Collectors.toMap(CargaColaboradorDTO::getIdColaborador, Function.identity())));
        if (!bloqueados.contains("talento")) cargarTalentoInicio(proyecto, model);
        return "resource/inicio";
    }

    private void cargarTalentoInicio(String proyecto, Model model) {
        List<Proyecto> opciones = proyectoRepository.findAll(Sort.by("nombre", "idProyecto"));
        Integer id = proyecto.isBlank() ? recomendacionIARepository
                .findFirstByProyectoIsNotNullOrderByPorcentajeMatchDescIdRecomendacionDesc()
                .map(r -> r.getProyecto().getIdProyecto()).orElse(null) : leerId(proyecto);
        Proyecto seleccionado = opciones.stream().filter(p -> p.getIdProyecto().equals(id)).findFirst().orElse(null);
        if (seleccionado == null && proyecto.isBlank() && !opciones.isEmpty()) seleccionado = opciones.get(0);
        if (seleccionado == null && !proyecto.isBlank()) {
            model.addAttribute("errorProyecto", "Selecciona uno de los proyectos de la lista.");
        }
        List<RecomendacionIA> candidatos = seleccionado == null ? List.of() : recomendacionIARepository
                .findTop3ByProyectoIdProyectoOrderByPorcentajeMatchDescIdRecomendacionDesc(seleccionado.getIdProyecto());
        model.addAttribute("proyectosTalento", opciones);
        model.addAttribute("proyectoTalento", seleccionado);
        model.addAttribute("candidatos", candidatos);
    }

    private Integer leerId(String valor) {
        try {
            int id = Integer.parseInt(valor);
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(value = "q", defaultValue = "") String q,
                                @RequestParam(value = "nivel", defaultValue = "TODOS") String nivel,
                                @RequestParam(value = "disponibilidad", defaultValue = "TODOS") String disponibilidad,
                                @RequestParam(value = "estado", defaultValue = "TODOS") String estado,
                                @RequestParam(value = "page", defaultValue = "1") String pagina,
                                @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                                Model model) {
        String texto = q.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (texto.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");
        Colaborador.Seniority nivelElegido = leerFiltro(nivel, Colaborador.Seniority.class,
                "nivel", "Selecciona un nivel profesional de la lista.", errores);
        Usuario.EstadoUsuario estadoElegido = leerFiltro(estado, Usuario.EstadoUsuario.class,
                "estado", "Selecciona un estado de cuenta de la lista.", errores);
        Clasificacion disponibilidadElegida = leerFiltro(disponibilidad, Clasificacion.class,
                "disponibilidad", "Selecciona una opción de disponibilidad de la lista.", errores);
        boolean verCapacidad = !bloqueados.contains("capacidad");
        if (!verCapacidad && disponibilidadElegida != null) {
            errores.put("disponibilidad", "La consulta de disponibilidad está deshabilitada para tu rol.");
        }
        Integer paginaPedida = leerId(pagina);
        if (paginaPedida == null) errores.put("page", "Selecciona un número de página válido.");

        List<Colaborador> lista = List.of();
        Map<Integer, CargaColaboradorDTO> capacidades = Map.of();
        if (errores.isEmpty()) {
            List<Colaborador> encontrados = texto.isEmpty()
                    ? colaboradorRepository.listarConUsuario() : colaboradorRepository.buscar(texto);
            lista = encontrados.stream()
                    .filter(c -> nivelElegido == null || c.getSeniority() == nivelElegido)
                    .filter(c -> estadoElegido == null || c.getUsuario().getEstado() == estadoElegido)
                    .sorted(Comparator.comparing(Colaborador::getIdColaborador))
                    .toList();
            if (verCapacidad) capacidades = capacidadColaboradorService.consultarCapacidades(lista);
            if (disponibilidadElegida != null) {
                Map<Integer, CargaColaboradorDTO> resumen = capacidades;
                lista = lista.stream()
                        .filter(c -> resumen.get(c.getIdColaborador()).getClasificacion() == disponibilidadElegida)
                        .toList();
            }
        }
        int resultados = lista.size();
        int totalPaginas = Math.max(1, (resultados + COLABORADORES_POR_PAGINA - 1) / COLABORADORES_POR_PAGINA);
        int paginaActual = paginaPedida == null ? 1 : Math.min(paginaPedida, totalPaginas);
        int desde = (paginaActual - 1) * COLABORADORES_POR_PAGINA;
        int hasta = Math.min(desde + COLABORADORES_POR_PAGINA, resultados);

        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaboradores", lista.subList(desde, hasta));
        model.addAttribute("verCapacidad", verCapacidad);
        model.addAttribute("capacidades", capacidades);
        model.addAttribute("q", texto);
        model.addAttribute("niveles", Colaborador.Seniority.values());
        model.addAttribute("nivelSeleccionado", nivelElegido);
        model.addAttribute("estados", Usuario.EstadoUsuario.values());
        model.addAttribute("estadoSeleccionado", estadoElegido);
        model.addAttribute("disponibilidadSeleccionada", disponibilidadElegida);
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("resultados", resultados);
        model.addAttribute("desde", desde + 1);
        model.addAttribute("hasta", hasta);
        return "resource/colaboradores";
    }

    private <T extends Enum<T>> T leerFiltro(String valor, Class<T> tipo, String campo, String mensaje,
                                             Map<String, String> errores) {
        String codigo = valor.strip().toUpperCase(Locale.ROOT);
        if (codigo.isEmpty() || "TODOS".equals(codigo)) return null;
        try {
            return Enum.valueOf(tipo, codigo);
        } catch (IllegalArgumentException ex) {
            errores.put(campo, mensaje);
            return null;
        }
    }

    @GetMapping("/colaboradores/{id}")
    public String perfilColaborador(@PathVariable Integer id,
                                    @RequestParam(value = "desde", defaultValue = "") String desde,
                                    @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Colaborador colaborador = colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El colaborador no está disponible."));
        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("desdeInicio", "inicio".equals(desde));
        model.addAttribute("puedeAsignar", !bloqueados.contains("asignaciones")
                && colaborador.getUsuario().getEstado() == Usuario.EstadoUsuario.ACTIVO);
        model.addAttribute("habilidades", colaboradorRepository.obtenerHabilidades(id).stream()
                .map(HabilidadColaboradorDTO::desdeFila).toList());

        if (!bloqueados.contains("capacidad")) {
            model.addAttribute("capacidad", capacidadColaboradorService.consultarActual(colaborador));
        }
        boolean verActuales = !bloqueados.contains("asignaciones");
        boolean verHistorial = !bloqueados.contains("historial");
        if (verActuales || verHistorial) {
            LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
            List<Asignacion> asignaciones = asignacionRepository.findByColaboradorIdColaboradorOrderByFechaInicioDesc(id);
            if (verActuales) {
                model.addAttribute("asignacionesActuales", asignaciones.stream().filter(a -> a.estaVigente(hoy))
                        .map(a -> FilaAsignacionDTO.desde(a, hoy)).toList());
            }
            if (verHistorial) {
                model.addAttribute("proyectosAnteriores", asignaciones.stream()
                        .filter(a -> !a.getFechaInicio().isAfter(hoy) && a.esHistorica(hoy))
                        .map(a -> FilaAsignacionDTO.desde(a, hoy))
                        .sorted(Comparator.comparing(FilaAsignacionDTO::fechaOrden, Comparator.reverseOrder())
                                .thenComparing(fila -> fila.asignacion().getIdAsignacion(), Comparator.reverseOrder()))
                        .toList());
            }
        }
        return "resource/colaborador-perfil";
    }

    /**
     * Presenta todas las asignaciones y sus indicadores por estado.
     *
     * @param model modelo utilizado para cargar los datos de la vista
     * @return nombre de la plantilla de asignaciones
     */
    @GetMapping("/asignaciones")
    public String asignaciones(Model model) {
        List<Asignacion> lista = asignacionRepository.findAll();
        cargarAsignaciones(model, lista);
        return "resource/asignaciones";
    }

    /**
     * Agrega al modelo la lista de asignaciones y sus conteos por estado.
     *
     * @param model modelo utilizado para cargar los atributos
     * @param lista asignaciones que se mostrarán
     */
    private void cargarAsignaciones(Model model, List<Asignacion> lista) {
        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
    }

    /**
     * Presenta el estado de disponibilidad de los colaboradores.
     *
     * @param model modelo utilizado para cargar los datos de disponibilidad
     * @return nombre de la plantilla de disponibilidad
     */
    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        List<CargaColaboradorDTO> lista = capacidadColaboradorService.consultarEquipo();
        model.addAttribute("titulo", "Disponibilidad");
        model.addAttribute("cargas", lista);
        model.addAttribute("disponibles", lista.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("parciales", lista.stream().filter(c -> c.getClasificacion() == Clasificacion.PARCIAL).count());
        model.addAttribute("sinDisponibilidad", lista.stream().filter(c -> c.getClasificacion() == Clasificacion.SIN_DISPONIBILIDAD).count());
        return "resource/disponibilidad";
    }

    /**
     * Presenta la carga de trabajo de los colaboradores y su promedio.
     *
     * @param model modelo utilizado para cargar los datos de carga
     * @return nombre de la plantilla de carga
     */
    @GetMapping("/carga")
    public String carga(Model model) {
        List<CargaColaboradorDTO> lista = capacidadColaboradorService.consultarEquipo();
        model.addAttribute("titulo", "Carga de trabajo");
        model.addAttribute("cargas", lista);
        model.addAttribute("cargaPromedio", lista.isEmpty() ? 0 : Math.round(lista.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        return "resource/carga";
    }

    /**
     * Presenta las recomendaciones de talento y permite filtrarlas por proyecto.
     *
     * @param idProyecto identificador opcional del proyecto
     * @param model modelo utilizado para cargar recomendaciones y proyectos
     * @return nombre de la plantilla de talento
     */
    @GetMapping("/talent")
    public String talent(@RequestParam(value = "proyecto", required = false) Integer idProyecto,
                         Model model) {
        var recomendaciones = recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc();
        if (idProyecto != null) {
            recomendaciones = recomendaciones.stream()
                    .filter(r -> r.getProyecto() != null && idProyecto.equals(r.getProyecto().getIdProyecto()))
                    .toList();
        }
        model.addAttribute("titulo", "Talent Matching IA");
        model.addAttribute("recomendaciones", recomendaciones);
        model.addAttribute("proyectos", proyectoRepository.findAll());
        model.addAttribute("proyectoSeleccionado", idProyecto);
        return "resource/talent";
    }

    /**
     * Presenta el historial de proyectos completados o cancelados.
     *
     * @param model modelo utilizado para cargar los proyectos e indicadores
     * @return nombre de la plantilla de historial
     */
    @GetMapping("/historial")
    public String historial(Model model) {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        model.addAttribute("titulo", "Proyectos anteriores");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("completados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
        model.addAttribute("cancelados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.CANCELLED).count());
        return "resource/historial";
    }

    /**
     * Presenta los datos consolidados disponibles para los reportes de recursos.
     *
     * @param reporte nombre opcional del reporte seleccionado
     * @param model modelo utilizado para cargar cargas, asignaciones y proyectos
     * @return nombre de la plantilla de reportes
     */
    @GetMapping("/reportes")
    public String reportes(@RequestParam(value = "reporte", required = false) String reporte,
                           Model model) {
        List<CargaColaboradorDTO> cargas = capacidadColaboradorService.consultarEquipo();
        List<Asignacion> asignaciones = asignacionRepository.findAll();
        List<Proyecto> proyectos = proyectoRepository.findAll();
        var recomendaciones = recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc();

        model.addAttribute("titulo", "Reportes de recursos");
        model.addAttribute("reporteSeleccionado", reporte);
        model.addAttribute("cargas", cargas);
        model.addAttribute("asignaciones", asignaciones);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("recomendaciones", recomendaciones);
        model.addAttribute("totalColaboradores", cargas.size());
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("sinDisponibilidad", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.SIN_DISPONIBILIDAD).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0 :
                Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("asignacionesActivas", asignaciones.stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("proyectosActivos", proyectos.stream()
                .filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());

        return "resource/reportes";
    }


    /**
     * Exporta como CSV el tipo de reporte solicitado por el usuario.
     *
     * @param tipo tipo de reporte: capacidad, asignaciones, talento o proyectos
     * @return respuesta CSV con el archivo generado o un error si el tipo no es válido
     */
    @GetMapping(value = "/reportes/exportar", produces = "text/csv")
    public ResponseEntity<String> exportarReporte(@RequestParam("tipo") String tipo) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        String nombreArchivo;

        switch (tipo == null ? "" : tipo.toLowerCase()) {
            case "capacidad" -> {
                nombreArchivo = "reporte_capacidad.csv";
                csv.append("Colaborador,Cargo,Area,Disponibilidad base,Carga,Disponibilidad\n");
                for (CargaColaboradorDTO c : capacidadColaboradorService.consultarEquipo()) {
                    csv.append(csv(c.getColaborador())).append(',')
                            .append(csv(c.getCargo())).append(',')
                            .append(csv(c.getArea())).append(',')
                            .append(csv(c.getDisponibilidadBase())).append(',')
                            .append(csv(c.getCarga())).append(',')
                            .append(csv(c.getDisponibilidad())).append('\n');
                }
            }
            case "asignaciones" -> {
                nombreArchivo = "reporte_asignaciones_recursos.csv";
                csv.append("Proyecto,Colaborador,Rol,Dedicacion,Estado,Inicio,Fin\n");
                for (Asignacion a : asignacionRepository.findAll()) {
                    csv.append(csv(a.getProyecto() == null ? "" : a.getProyecto().getNombre())).append(',')
                            .append(csv(a.getColaborador() == null || a.getColaborador().getUsuario() == null ? "" : a.getColaborador().getUsuario().getNombreCompleto())).append(',')
                            .append(csv(a.getRolProyecto())).append(',')
                            .append(csv(a.getPorcentajeDedicacion())).append(',')
                            .append(csv(a.getEstado())).append(',')
                            .append(csv(a.getFechaInicio())).append(',')
                            .append(csv(a.getFechaFin())).append('\n');
                }
            }
            case "talento" -> {
                nombreArchivo = "reporte_talent_matching.csv";
                csv.append("Proyecto,Colaborador,Match,Justificacion,Fecha\n");
                for (var r : recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc()) {
                    csv.append(csv(r.getProyecto() == null ? "" : r.getProyecto().getNombre())).append(',')
                            .append(csv(r.getColaborador() == null || r.getColaborador().getUsuario() == null ? "" : r.getColaborador().getUsuario().getNombreCompleto())).append(',')
                            .append(csv(r.getPorcentajeMatch())).append(',')
                            .append(csv(r.getJustificacion())).append(',')
                            .append(csv(r.getFechaRecomendacion())).append('\n');
                }
            }
            case "proyectos" -> {
                nombreArchivo = "reporte_proyectos_recursos.csv";
                csv.append("Proyecto,Estado,Prioridad,Progreso,Inicio,Fin\n");
                for (Proyecto p : proyectoRepository.findAll()) {
                    csv.append(csv(p.getNombre())).append(',')
                            .append(csv(p.getEstado())).append(',')
                            .append(csv(p.getPrioridad())).append(',')
                            .append(csv(p.getProgreso())).append(',')
                            .append(csv(p.getFechaInicio())).append(',')
                            .append(csv(p.getFechaFin())).append('\n');
                }
            }
            default -> {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString());
    }


    /**
     * Convierte un valor a texto CSV seguro y evita fórmulas al inicio del campo.
     *
     * @param valor valor que se incluirá en una celda CSV
     * @return texto del valor delimitado y escapado para CSV
     */
    private String csv(Object valor) {
        String texto = valor == null ? "" : String.valueOf(valor);
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

}
