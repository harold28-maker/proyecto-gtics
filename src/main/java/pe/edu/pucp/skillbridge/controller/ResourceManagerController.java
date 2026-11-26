package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.Clasificacion;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.EstadoCarga;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Proyecto.EstadoProyecto;
import pe.edu.pucp.skillbridge.entity.RecomendacionIA;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.RecomendacionIARepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final int PROYECTOS_POR_PAGINA = 6;
    private static final List<EstadoProyecto> ESTADOS_HISTORIAL = List.of(EstadoProyecto.COMPLETED, EstadoProyecto.CANCELLED);

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
     * Lista las asignaciones con filtros y un resumen del estado del equipo.
     *
     * @param q texto opcional para buscar por colaborador, proyecto o rol
     * @param proyecto identificador opcional del proyecto
     * @param estado estado opcional de la asignación
     * @param model modelo con los resultados y los indicadores
     * @return nombre de la plantilla de asignaciones
     */
    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(value = "q", defaultValue = "") String q,
                               @RequestParam(value = "proyecto", defaultValue = "") String proyecto,
                               @RequestParam(value = "estado", defaultValue = "TODOS") String estado,
                               Model model) {
        String busqueda = q.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (busqueda.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");

        List<Proyecto> proyectos = proyectoRepository.findAll(Sort.by("nombre", "idProyecto"));
        Integer proyectoElegido = leerId(proyecto.strip());
        if (!proyecto.isBlank() && proyectos.stream().noneMatch(p -> p.getIdProyecto().equals(proyectoElegido))) {
            errores.put("proyecto", "Selecciona un proyecto de la lista.");
        }
        EstadoAsignacion estadoElegido = leerFiltro(estado, EstadoAsignacion.class,
                "estado", "Selecciona un estado de asignación de la lista.", errores);

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        List<Asignacion> todas = asignacionRepository.findAll(Sort.by("idAsignacion"));
        List<Asignacion> resultados = List.of();
        if (errores.isEmpty()) {
            String texto = normalizarBusqueda(busqueda);
            resultados = todas.stream()
                    .filter(a -> proyectoElegido == null || proyectoElegido.equals(a.getProyecto().getIdProyecto()))
                    .filter(a -> estadoElegido == null || estadoElegido == a.getEstado())
                    .filter(a -> texto.isEmpty()
                            || normalizarBusqueda(a.getColaborador().getUsuario().getNombreCompleto()).contains(texto)
                            || normalizarBusqueda(a.getProyecto().getNombre()).contains(texto)
                            || normalizarBusqueda(a.getRolProyecto()).contains(texto))
                    .toList();
        }

        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("asignaciones", resultados);
        model.addAttribute("q", busqueda);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("proyectoSeleccionado", proyectoElegido);
        model.addAttribute("estados", EstadoAsignacion.values());
        model.addAttribute("estadoSeleccionado", estadoElegido);
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("hoy", hoy);
        model.addAttribute("totalAsignaciones", todas.size());
        model.addAttribute("vigentes", todas.stream().filter(a -> a.estaVigente(hoy)).count());
        model.addAttribute("activasVencidas", todas.stream()
                .filter(a -> a.getEstado() == EstadoAsignacion.ACTIVA && a.tieneFinVencido(hoy)).count());
        model.addAttribute("planificadas", todas.stream().filter(a -> a.getEstado() == EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("cerradas", todas.stream()
                .filter(a -> a.getEstado() != null && a.getEstado().isCerrada()).count());
        return "resource/asignaciones";
    }

    private String normalizarBusqueda(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Consulta la disponibilidad de las cuentas activas del equipo.
     *
     * @param modo consulta del día o de un período
     * @param desde fecha de inicio del período
     * @param hasta fecha de fin del período, inclusive
     * @param q nombre o cargo que se desea buscar
     * @param clasificacion clasificación opcional de la disponibilidad
     * @param bloqueados permisos deshabilitados para el rol actual
     * @param model modelo utilizado para cargar los datos de disponibilidad
     * @return nombre de la plantilla de disponibilidad
     */
    @GetMapping("/disponibilidad")
    public String disponibilidad(@RequestParam(value = "modo", defaultValue = "hoy") String modo,
                                 @RequestParam(value = "desde", required = false) String desde,
                                 @RequestParam(value = "hasta", required = false) String hasta,
                                 @RequestParam(value = "q", defaultValue = "") String q,
                                 @RequestParam(value = "clasificacion", defaultValue = "TODOS") String clasificacion,
                                 @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                                 Model model) {
        String vista = modo.strip().toLowerCase(Locale.ROOT);
        boolean porPeriodo = "periodo".equals(vista);
        boolean consultaSolicitada = !porPeriodo || desde != null || hasta != null;
        String busqueda = q.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (!porPeriodo && !"hoy".equals(vista)) errores.put("modo", "Selecciona Hoy o Por período.");
        if (busqueda.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");
        Clasificacion clasificacionElegida = leerFiltro(clasificacion, Clasificacion.class,
                "clasificacion", "Selecciona una clasificación de la lista.", errores);

        LocalDate inicio = null;
        LocalDate fin = null;
        if (porPeriodo && consultaSolicitada) {
            inicio = leerFechaDisponibilidad(desde, "desde", "Selecciona la fecha de inicio.", errores);
            fin = leerFechaDisponibilidad(hasta, "hasta", "Selecciona la fecha de fin.", errores);
            if (inicio != null && fin != null && fin.isBefore(inicio)) {
                errores.put("hasta", "La fecha de fin debe ser igual o posterior a la de inicio.");
            }
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        boolean consultaValida = consultaSolicitada && errores.isEmpty();
        List<CargaColaboradorDTO> equipo = List.of();
        List<CargaColaboradorDTO> resultados = List.of();
        if (consultaValida) {
            List<Colaborador> activos = capacidadColaboradorService.listarPerfilesActivos();
            Map<Integer, CargaColaboradorDTO> capacidades = porPeriodo
                    ? capacidadColaboradorService.consultarCapacidadesPeriodo(activos, inicio, fin)
                    : capacidadColaboradorService.consultarCapacidades(activos, hoy);
            equipo = List.copyOf(capacidades.values());
            String texto = normalizarBusqueda(busqueda);
            resultados = equipo.stream()
                    .filter(c -> clasificacionElegida == null || c.getClasificacion() == clasificacionElegida)
                    .filter(c -> coincideConColaborador(c, texto))
                    .sorted(Comparator.comparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(CargaColaboradorDTO::getIdColaborador))
                    .toList();
        }

        model.addAttribute("titulo", "Disponibilidad");
        model.addAttribute("hoy", hoy);
        model.addAttribute("porPeriodo", porPeriodo);
        model.addAttribute("consultaValida", consultaValida);
        model.addAttribute("q", busqueda);
        model.addAttribute("desde", inicio == null ? desde : inicio.toString());
        model.addAttribute("hasta", fin == null ? hasta : fin.toString());
        model.addAttribute("clasificaciones", Clasificacion.values());
        model.addAttribute("clasificacionSeleccionada", clasificacionElegida);
        model.addAttribute("verPerfil", !bloqueados.contains("colaboradores"));
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("cargas", resultados);
        model.addAttribute("totalPerfiles", equipo.size());
        model.addAttribute("disponibles", equipo.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("parciales", equipo.stream().filter(c -> c.getClasificacion() == Clasificacion.PARCIAL).count());
        model.addAttribute("sinDisponibilidad", equipo.stream()
                .filter(c -> c.getClasificacion() == Clasificacion.SIN_DISPONIBILIDAD).count());
        model.addAttribute("sinCargaHoy", !porPeriodo && !equipo.isEmpty() && equipo.stream().allMatch(c -> c.getCarga() == 0));
        return "resource/disponibilidad";
    }

    private LocalDate leerFechaDisponibilidad(String valor, String campo, String obligatorio, Map<String, String> errores) {
        if (valor == null || valor.isBlank()) {
            errores.put(campo, obligatorio);
            return null;
        }
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.getYear() >= 1000 && fecha.getYear() <= 9999) return fecha;
        } catch (DateTimeParseException ex) {
            // Se devuelve el mismo error para fechas inexistentes y años fuera del rango de la base de datos.
        }
        errores.put(campo, "Ingresa una fecha válida.");
        return null;
    }

    /**
     * Presenta la carga actual de los perfiles activos y el detalle del colaborador seleccionado.
     *
     * @param q nombre o cargo que se desea buscar
     * @param estado estado opcional de la carga
     * @param colaborador identificador del colaborador que se desea consultar
     * @param bloqueados permisos deshabilitados para el rol actual
     * @param model modelo utilizado para cargar los datos de carga
     * @return nombre de la plantilla de carga
     */
    @GetMapping("/carga")
    public String carga(@RequestParam(value = "q", defaultValue = "") String q,
                        @RequestParam(value = "estado", defaultValue = "TODOS") String estado,
                        @RequestParam(value = "colaborador", defaultValue = "") String colaborador,
                        @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        String busqueda = q.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (busqueda.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");
        EstadoCarga estadoElegido = leerFiltro(estado, EstadoCarga.class,
                "estado", "Selecciona un estado de carga de la lista.", errores);
        boolean filtrosValidos = errores.isEmpty();
        boolean seleccionSolicitada = !colaborador.isBlank();
        Integer idElegido = seleccionSolicitada ? leerId(colaborador.strip()) : null;
        if (seleccionSolicitada && idElegido == null) {
            errores.put("colaborador", "Selecciona un colaborador de los resultados.");
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        boolean verAsignaciones = !bloqueados.contains("asignaciones");
        List<CargaColaboradorDTO> equipo = List.of();
        List<CargaColaboradorDTO> resultados = List.of();
        List<Asignacion> asignacionesVigentes = List.of();
        CargaColaboradorDTO detalle = null;
        if (filtrosValidos) {
            var resumen = capacidadColaboradorService.consultarCargaEquipo(capacidadColaboradorService.listarPerfilesActivos(), hoy);
            equipo = List.copyOf(resumen.capacidades().values());
            String texto = normalizarBusqueda(busqueda);
            resultados = equipo.stream()
                    .filter(c -> estadoElegido == null || c.getEstadoCarga() == estadoElegido)
                    .filter(c -> coincideConColaborador(c, texto))
                    .sorted(Comparator.comparingInt(CargaColaboradorDTO::getCarga).reversed()
                            .thenComparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(CargaColaboradorDTO::getIdColaborador))
                    .toList();

            detalle = resultados.stream()
                    .filter(c -> !seleccionSolicitada || c.getIdColaborador().equals(idElegido))
                    .findFirst().orElse(null);
            if (seleccionSolicitada && detalle == null) {
                errores.put("colaborador", "Selecciona un colaborador de los resultados.");
            }
            if (detalle != null && verAsignaciones) {
                Integer idDetalle = detalle.getIdColaborador();
                asignacionesVigentes = resumen.asignacionesVigentes().stream()
                        .filter(a -> a.getColaborador().getIdColaborador().equals(idDetalle))
                        .toList();
            }
        }

        int cargaMaxima = equipo.stream().mapToInt(CargaColaboradorDTO::getCarga).max().orElse(0);
        // Cuatro intervalos de al menos 25 puntos, ampliados en múltiplos de cinco si hay sobrecarga.
        int paso = Math.max(25, (int) Math.ceil(cargaMaxima / 20.0) * 5);
        model.addAttribute("titulo", "Carga de trabajo");
        model.addAttribute("hoy", hoy);
        model.addAttribute("q", busqueda);
        model.addAttribute("estados", EstadoCarga.values());
        model.addAttribute("estadoSeleccionado", estadoElegido);
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("filtrosValidos", filtrosValidos);
        model.addAttribute("cargas", resultados);
        model.addAttribute("detalle", detalle);
        model.addAttribute("verPerfil", !bloqueados.contains("colaboradores"));
        model.addAttribute("verAsignaciones", verAsignaciones);
        model.addAttribute("asignacionesVigentes", asignacionesVigentes);
        model.addAttribute("totalPerfiles", equipo.size());
        model.addAttribute("cargaPromedio", Math.round(equipo.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("alLimite", equipo.stream().filter(c -> c.getEstadoCarga() == EstadoCarga.AL_LIMITE).count());
        model.addAttribute("sobrecargados", equipo.stream().filter(c -> c.getEstadoCarga() == EstadoCarga.SOBRECARGA).count());
        model.addAttribute("sinCargaHoy", !equipo.isEmpty() && cargaMaxima == 0);
        model.addAttribute("maximoEscala", 4 * paso);
        model.addAttribute("marcasEscala", List.of(0, paso, 2 * paso, 3 * paso, 4 * paso));
        return "resource/carga";
    }

    private boolean coincideConColaborador(CargaColaboradorDTO colaborador, String texto) {
        return texto.isEmpty() || normalizarBusqueda(colaborador.getColaborador()).contains(texto)
                || normalizarBusqueda(colaborador.getCargo()).contains(texto);
    }

    /**
     * Consulta los proyectos cerrados y su equipo, con filtros y paginación.
     */
    @GetMapping("/historial")
    public String historial(@RequestParam(value = "q", defaultValue = "") String q,
                            @RequestParam(value = "estado", defaultValue = "TODOS") String estado,
                            @RequestParam(value = "page", defaultValue = "1") String pagina,
                            @RequestParam(value = "proyecto", defaultValue = "") String proyecto,
                            @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                            HttpServletResponse respuesta, Model model) {
        String busqueda = q.strip();
        Map<String, String> errores = new LinkedHashMap<>();
        if (busqueda.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");

        String mensajeEstado = "Selecciona Completados, Cancelados o Todos los cerrados.";
        EstadoProyecto estadoElegido = leerFiltro(estado, EstadoProyecto.class, "estado", mensajeEstado, errores);
        if (estadoElegido != null && !ESTADOS_HISTORIAL.contains(estadoElegido)) {
            errores.put("estado", mensajeEstado);
        }
        Integer paginaPedida = leerId(pagina.strip());
        if (paginaPedida == null) errores.put("page", "Selecciona un número de página válido.");
        Integer idDetalle = leerId(proyecto.strip());
        if (!proyecto.isBlank() && idDetalle == null) {
            errores.put("proyecto", "Selecciona un proyecto cerrado del historial.");
        }

        List<Proyecto> cerrados = proyectoRepository.findByEstadoInOrderByFechaFinDescIdProyectoDesc(ESTADOS_HISTORIAL);
        long completados = cerrados.stream().filter(p -> p.getEstado() == EstadoProyecto.COMPLETED).count();
        List<Proyecto> resultados = List.of();
        Proyecto detalle = null;
        if (errores.isEmpty()) {
            String texto = normalizarBusqueda(busqueda);
            EstadoProyecto filtro = estadoElegido;
            resultados = cerrados.stream()
                    .filter(p -> filtro == null || p.getEstado() == filtro)
                    .filter(p -> texto.isEmpty() || normalizarBusqueda(p.getNombre()).contains(texto)
                            || normalizarBusqueda(p.getProjectManager().getNombreCompleto()).contains(texto))
                    .toList();
            if (idDetalle != null) {
                detalle = cerrados.stream().filter(p -> p.getIdProyecto().equals(idDetalle)).findFirst().orElse(null);
                if (detalle == null) {
                    respuesta.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    model.addAttribute("errorDetalle", "Selecciona un proyecto cerrado del historial para ver su equipo.");
                }
            }
        } else {
            respuesta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        int totalPaginas = Math.max(1, (resultados.size() + PROYECTOS_POR_PAGINA - 1) / PROYECTOS_POR_PAGINA);
        int paginaActual = paginaPedida == null ? 1 : Math.min(paginaPedida, totalPaginas);
        int desde = (paginaActual - 1) * PROYECTOS_POR_PAGINA;
        int hasta = Math.min(desde + PROYECTOS_POR_PAGINA, resultados.size());
        List<Proyecto> proyectosPagina = resultados.subList(desde, hasta);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        Map<Integer, List<Asignacion>> participaciones = consultarParticipaciones(proyectosPagina, detalle, hoy);
        Map<Integer, Long> integrantes = participaciones.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().stream().map(a -> a.getColaborador().getIdColaborador()).distinct().count()));

        model.addAttribute("titulo", "Proyectos anteriores");
        model.addAttribute("proyectos", proyectosPagina);
        model.addAttribute("totalCerrados", cerrados.size());
        model.addAttribute("completados", completados);
        model.addAttribute("cancelados", cerrados.size() - completados);
        model.addAttribute("q", busqueda);
        model.addAttribute("estadoSeleccionado", estadoElegido != null && ESTADOS_HISTORIAL.contains(estadoElegido) ? estadoElegido : null);
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("resultados", resultados.size());
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("desde", desde + 1);
        model.addAttribute("hasta", hasta);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("verPerfil", !bloqueados.contains("colaboradores"));
        if (detalle != null) {
            model.addAttribute("detalleProyecto", detalle);
            model.addAttribute("equipo", participaciones.getOrDefault(detalle.getIdProyecto(), List.of()).stream()
                    .map(a -> FilaAsignacionDTO.desde(a, hoy)).toList());
        }
        return "resource/historial";
    }

    private Map<Integer, List<Asignacion>> consultarParticipaciones(List<Proyecto> pagina, Proyecto detalle, LocalDate hoy) {
        Set<Integer> ids = pagina.stream().map(Proyecto::getIdProyecto).collect(Collectors.toCollection(LinkedHashSet::new));
        if (detalle != null) ids.add(detalle.getIdProyecto());
        if (ids.isEmpty()) return Map.of();

        return asignacionRepository.findByProyectoIdProyectoInOrderByFechaInicioDescIdAsignacionDesc(ids).stream()
                .filter(a -> a.registraExperiencia(hoy))
                .collect(Collectors.groupingBy(a -> a.getProyecto().getIdProyecto()));
    }

}
