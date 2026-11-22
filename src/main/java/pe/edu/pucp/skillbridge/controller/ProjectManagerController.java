package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadProyectoDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Controlador MVC que administra el portafolio de proyectos, asignaciones,
 * recomendaciones, foros, disponibilidad e informes de un Project Manager.
 */
@Controller
@RequestMapping("/pm")
public class ProjectManagerController {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final AsignacionRepository asignacionRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final RecomendacionIARepository recomendacionIARepository;
    private final ForoRepository foroRepository;
    private final HabilidadRepository habilidadRepository;
    private final RespuestaForoRepository respuestaForoRepository;

    /**
     * Crea el controlador e inicializa los repositorios usados por sus operaciones.
     *
     * @param usuarioRepository repositorio de usuarios
     * @param proyectoRepository repositorio de proyectos
     * @param asignacionRepository repositorio de asignaciones
     * @param colaboradorRepository repositorio de colaboradores
     * @param recomendacionIARepository repositorio de recomendaciones de IA
     * @param foroRepository repositorio de foros
     * @param habilidadRepository repositorio de habilidades
     * @param respuestaForoRepository repositorio de respuestas de foro
     */
    public ProjectManagerController(UsuarioRepository usuarioRepository,
                                    ProyectoRepository proyectoRepository,
                                    AsignacionRepository asignacionRepository,
                                    ColaboradorRepository colaboradorRepository,
                                    RecomendacionIARepository recomendacionIARepository,
                                    ForoRepository foroRepository,
                                    HabilidadRepository habilidadRepository,
                                    RespuestaForoRepository respuestaForoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.asignacionRepository = asignacionRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.recomendacionIARepository = recomendacionIARepository;
        this.foroRepository = foroRepository;
        this.habilidadRepository = habilidadRepository;
        this.respuestaForoRepository = respuestaForoRepository;
    }

    /**
     * Obtiene el Project Manager asociado a la sesión o un usuario de demostración disponible.
     *
     * @return usuario con rol Project Manager o el primer usuario disponible
     */
    private Usuario pmDemo() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null && session.getAttribute("usuarioSesionId") instanceof Integer idUsuario) {
                Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
                if (usuario != null && usuario.getRol() != null
                        && "PROJECT_MANAGER".equals(usuario.getRol().getNombre())) {
                    return usuario;
                }
            }
        }

        return usuarioRepository.findFirstByRolNombreOrderByIdUsuarioAsc("PROJECT_MANAGER")
                .orElseGet(() -> usuarioRepository.findAll().stream().findFirst().orElse(null));
    }

    /**
     * Convierte un valor numérico a entero o devuelve cero cuando es nulo.
     *
     * @param value valor obtenido de una consulta
     * @return valor convertido a entero
     */
    private int intValue(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

    /**
     * Obtiene los datos de carga de los colaboradores para las vistas del controlador.
     *
     * @return lista de cargas de colaboradores
     */
    private List<CargaColaboradorDTO> cargas() {
        List<CargaColaboradorDTO> resultado = new ArrayList<>();
        for (Object[] fila : colaboradorRepository.obtenerCargaColaboradores()) {
            resultado.add(new CargaColaboradorDTO(
                    intValue(fila[0]), String.valueOf(fila[1]),
                    fila[2] == null ? "-" : String.valueOf(fila[2]),
                    fila[3] == null ? "-" : String.valueOf(fila[3]),
                    intValue(fila[4]), intValue(fila[5]), intValue(fila[6])
            ));
        }
        return resultado;
    }

    /**
     * Recupera las habilidades requeridas por un proyecto.
     *
     * @param idProyecto identificador del proyecto
     * @return lista de requisitos de habilidad del proyecto
     */
    private List<HabilidadProyectoDTO> habilidadesProyecto(Integer idProyecto) {
        List<HabilidadProyectoDTO> lista = new ArrayList<>();
        for (Object[] fila : proyectoRepository.obtenerHabilidadesRequeridas(idProyecto)) {
            lista.add(new HabilidadProyectoDTO(
                    intValue(fila[0]), String.valueOf(fila[1]),
                    fila[2] == null ? "Sin categoría" : String.valueOf(fila[2]),
                    String.valueOf(fila[3]), intValue(fila[4])
            ));
        }
        return lista;
    }

    /**
     * Presenta el dashboard de proyectos, asignaciones y colaboradores del manager.
     *
     * @param model modelo utilizado para cargar los datos del dashboard
     * @return nombre de la plantilla de inicio
     */
    @RequestMapping(value = {"/inicio", "/dashboard"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of()
                : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        List<Asignacion> asignaciones = pm == null ? List.of()
                : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());

        model.addAttribute("titulo", "Inicio");
        model.addAttribute("pm", pm);
        model.addAttribute("proyectosActivos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("proyectosPlanning", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.PLANNING).count());
        model.addAttribute("asignaciones", asignaciones.size());
        model.addAttribute("colaboradores", asignaciones.stream().map(a -> a.getColaborador().getIdColaborador()).distinct().count());
        model.addAttribute("proyectos", proyectos.stream().limit(5).toList());
        model.addAttribute("ultimasAsignaciones", asignaciones.stream().limit(5).toList());
        return "pm/inicio";
    }

    /**
     * Lista los proyectos del Project Manager aplicando una búsqueda opcional por nombre.
     *
     * @param q texto opcional usado como filtro
     * @param model modelo utilizado para cargar los proyectos
     * @return nombre de la plantilla de proyectos
     */
    @GetMapping("/proyectos")
    public String proyectos(@RequestParam(value = "q", required = false) String q, Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of()
                : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        if (q != null && !q.isBlank()) {
            String texto = q.toLowerCase();
            proyectos = proyectos.stream().filter(p -> p.getNombre().toLowerCase().contains(texto)).toList();
        }
        cargarProyectos(model, proyectos, q);
        return "pm/proyectos";
    }

    /**
     * Agrega al modelo la lista de proyectos y sus indicadores por estado.
     *
     * @param model modelo utilizado para cargar los atributos
     * @param proyectos proyectos que se mostrarán
     * @param q texto de búsqueda aplicado
     */
    private void cargarProyectos(Model model, List<Proyecto> proyectos, String q) {
        model.addAttribute("titulo", "Portafolio de proyectos");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("q", q);
        model.addAttribute("activos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("planning", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.PLANNING).count());
        model.addAttribute("completados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
    }

    /**
     * Prepara el formulario con valores iniciales para crear un proyecto.
     *
     * @param model modelo utilizado para cargar el proyecto nuevo
     * @return nombre de la plantilla del formulario
     */
    @GetMapping("/proyectos/nuevo")
    public String nuevoProyecto(Model model) {
        Proyecto proyecto = new Proyecto();
        proyecto.setEstado(Proyecto.EstadoProyecto.PLANNING);
        proyecto.setPrioridad(Proyecto.Prioridad.MEDIA);
        proyecto.setProgreso(0);
        model.addAttribute("titulo", "Nuevo proyecto");
        model.addAttribute("proyecto", proyecto);
        return "pm/proyecto-form";
    }

    /**
     * Carga un proyecto existente en el formulario de edición.
     *
     * @param id identificador del proyecto
     * @param model modelo utilizado para cargar el proyecto
     * @return nombre de la plantilla del formulario
     * @throws java.util.NoSuchElementException si el proyecto no existe
     */
    @GetMapping("/proyectos/editar/{id}")
    public String editarProyecto(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("titulo", "Editar proyecto");
        model.addAttribute("proyecto", proyectoRepository.findById(id).orElseThrow());
        return "pm/proyecto-form";
    }

    /**
     * Valida y guarda un proyecto asignándolo al Project Manager de la sesión.
     *
     * @param proyecto proyecto enviado por el formulario
     * @param bindingResult resultado de validación del proyecto
     * @param model modelo utilizado para cargar el resultado
     * @return plantilla de proyectos o del formulario si existen errores
     */
    @PostMapping("/proyectos/guardar")
    public String guardarProyecto(@Valid @ModelAttribute("proyecto") Proyecto proyecto,
                                  BindingResult bindingResult,
                                  Model model) {
        proyecto.setProjectManager(pmDemo());
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", proyecto.getIdProyecto() == null ? "Nuevo proyecto" : "Editar proyecto");
            model.addAttribute("mensajeError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "pm/proyecto-form";
        }
        proyectoRepository.save(proyecto);
        cargarProyectos(model,
                proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pmDemo().getIdUsuario()), null);
        model.addAttribute("mensajeExito", "Proyecto guardado correctamente.");
        return "pm/proyectos";
    }

    // Baja lógica del proyecto: conserva historial y relaciones.
    /**
     * Marca un proyecto como cancelado sin eliminar su historial ni sus relaciones.
     *
     * @param id identificador del proyecto
     * @param model modelo utilizado para cargar la lista actualizada
     * @return nombre de la plantilla de proyectos
     * @throws java.util.NoSuchElementException si el proyecto no existe
     */
    @PostMapping("/proyectos/{id}/cancelar")
    public String cancelarProyecto(@PathVariable Integer id, Model model) {
        Proyecto proyecto = proyectoRepository.findById(id).orElseThrow();
        proyecto.setEstado(Proyecto.EstadoProyecto.CANCELLED);
        proyectoRepository.save(proyecto);
        cargarProyectos(model,
                proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pmDemo().getIdUsuario()), null);
        model.addAttribute("mensajeExito", "Proyecto marcado como cancelado.");
        return "pm/proyectos";
    }

    /**
     * Presenta las habilidades requeridas por un proyecto y el catálogo disponible.
     *
     * @param id identificador del proyecto
     * @param model modelo utilizado para cargar el proyecto y las habilidades
     * @return nombre de la plantilla de habilidades del proyecto
     * @throws java.util.NoSuchElementException si el proyecto no existe
     */
    @GetMapping("/proyectos/{id}/habilidades")
    public String habilidadesRequeridas(@PathVariable Integer id, Model model) {
        Proyecto proyecto = proyectoRepository.findById(id).orElseThrow();
        model.addAttribute("titulo", "Habilidades requeridas");
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("requerimientos", habilidadesProyecto(id));
        model.addAttribute("catalogo", habilidadRepository.findByEstadoTrueOrderByNombreAsc());
        return "pm/proyecto-habilidades";
    }

    /**
     * Guarda o actualiza un requerimiento de habilidad para un proyecto.
     *
     * @param id identificador del proyecto
     * @param idHabilidad identificador de la habilidad
     * @param nivel nivel requerido
     * @param vacantes cantidad de vacantes
     * @param model modelo utilizado para cargar la vista actualizada
     * @return plantilla de habilidades requeridas
     */
    @PostMapping("/proyectos/{id}/habilidades/guardar")
    public String guardarHabilidadProyecto(@PathVariable Integer id,
                                            @RequestParam("idHabilidad") Integer idHabilidad,
                                            @RequestParam("nivel") String nivel,
                                            @RequestParam("vacantes") Integer vacantes,
                                            Model model) {
        proyectoRepository.guardarHabilidadRequerida(id, idHabilidad, nivel, vacantes);
        model.addAttribute("mensajeExito", "Requerimiento técnico actualizado.");
        return habilidadesRequeridas(id, model);
    }

    /**
     * Elimina un requerimiento de habilidad asociado a un proyecto.
     *
     * @param id identificador del proyecto
     * @param idHabilidad identificador de la habilidad a retirar
     * @param model modelo utilizado para cargar la vista actualizada
     * @return plantilla de habilidades requeridas
     */
    @PostMapping("/proyectos/{id}/habilidades/{idHabilidad}/eliminar")
    public String eliminarHabilidadProyecto(@PathVariable Integer id,
                                             @PathVariable Integer idHabilidad,
                                             Model model) {
        proyectoRepository.eliminarHabilidadRequerida(id, idHabilidad);
        model.addAttribute("mensajeExito", "Habilidad requerida retirada del proyecto.");
        return habilidadesRequeridas(id, model);
    }

    /**
     * Presenta las asignaciones de los proyectos administrados por el Project Manager.
     *
     * @param model modelo utilizado para cargar las asignaciones
     * @return nombre de la plantilla de asignaciones
     */
    @GetMapping("/asignaciones")
    public String asignaciones(Model model) {
        Usuario pm = pmDemo();
        List<Asignacion> lista = pm == null ? List.of()
                : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());
        cargarAsignaciones(model, lista);
        return "pm/asignaciones";
    }

    /**
     * Agrega al modelo la lista de asignaciones y sus conteos por estado.
     *
     * @param model modelo utilizado para cargar los atributos
     * @param lista asignaciones que se mostrarán
     */
    private void cargarAsignaciones(Model model, List<Asignacion> lista) {
        model.addAttribute("titulo", "Asignaciones del equipo");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
    }

    /**
     * Prepara el formulario vacío para crear una asignación.
     *
     * @param model modelo utilizado para cargar los datos del formulario
     * @return nombre de la plantilla del formulario
     */
    @GetMapping("/asignaciones/nueva")
    public String nuevaAsignacion(Model model) {
        prepararFormularioAsignacion(new Asignacion(), "Nueva asignación", model);
        return "pm/asignacion-form";
    }

    /**
     * Carga una asignación existente en el formulario de edición.
     *
     * @param id identificador de la asignación
     * @param model modelo utilizado para cargar la asignación
     * @return nombre de la plantilla del formulario
     * @throws java.util.NoSuchElementException si la asignación no existe
     */
    @GetMapping("/asignaciones/editar/{id}")
    public String editarAsignacion(@PathVariable Integer id, Model model) {
        prepararFormularioAsignacion(asignacionRepository.findById(id).orElseThrow(), "Editar asignación", model);
        return "pm/asignacion-form";
    }

    /**
     * Carga una asignación y las opciones disponibles en el formulario.
     *
     * @param asignacion asignación que se editará o creará
     * @param titulo título que se mostrará en el formulario
     * @param model modelo utilizado para cargar los datos del formulario
     */
    private void prepararFormularioAsignacion(Asignacion asignacion, String titulo, Model model) {
        Usuario pm = pmDemo();
        model.addAttribute("titulo", titulo);
        model.addAttribute("asignacion", asignacion);
        model.addAttribute("proyectos", pm == null ? List.of()
                : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario()));
        model.addAttribute("colaboradores", colaboradorRepository.findAll());
    }

    /**
     * Valida y guarda una asignación asociada a un proyecto y colaborador.
     *
     * @param asignacion asignación enviada por el formulario
     * @param bindingResult resultado de validación de la asignación
     * @param idProyecto identificador del proyecto seleccionado
     * @param idColaborador identificador del colaborador seleccionado
     * @param model modelo utilizado para cargar el resultado
     * @return plantilla de asignaciones o del formulario si existen errores
     * @throws java.util.NoSuchElementException si el proyecto o colaborador no existe
     */
    @PostMapping("/asignaciones/guardar")
    public String guardarAsignacion(@Valid @ModelAttribute("asignacion") Asignacion asignacion,
                                    BindingResult bindingResult,
                                    @RequestParam("idProyecto") Integer idProyecto,
                                    @RequestParam("idColaborador") Integer idColaborador,
                                    Model model) {
        asignacion.setProyecto(proyectoRepository.findById(idProyecto).orElseThrow());
        asignacion.setColaborador(colaboradorRepository.findById(idColaborador).orElseThrow());
        if (bindingResult.hasErrors()) {
            prepararFormularioAsignacion(asignacion,
                    asignacion.getIdAsignacion() == null ? "Nueva asignación" : "Editar asignación", model);
            model.addAttribute("mensajeError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "pm/asignacion-form";
        }
        asignacionRepository.save(asignacion);
        cargarAsignaciones(model,
                asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pmDemo().getIdUsuario()));
        model.addAttribute("mensajeExito", "Asignación guardada correctamente.");
        return "pm/asignaciones";
    }

    /**
     * Actualiza el estado de una asignación existente.
     *
     * @param id identificador de la asignación
     * @param estado nuevo estado textual
     * @param model modelo utilizado para cargar la lista actualizada
     * @return nombre de la plantilla de asignaciones
     * @throws java.util.NoSuchElementException si la asignación no existe
     * @throws IllegalArgumentException si el estado no corresponde a un valor válido
     */
    @PostMapping("/asignaciones/{id}/estado")
    public String cambiarEstadoAsignacion(@PathVariable Integer id,
                                           @RequestParam("estado") String estado,
                                           Model model) {
        Asignacion asignacion = asignacionRepository.findById(id).orElseThrow();
        asignacion.setEstado(Asignacion.EstadoAsignacion.valueOf(estado));
        asignacionRepository.save(asignacion);
        cargarAsignaciones(model,
                asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pmDemo().getIdUsuario()));
        model.addAttribute("mensajeExito", "Estado de la asignación actualizado.");
        return "pm/asignaciones";
    }

    /**
     * Lista colaboradores aplicando filtros opcionales por texto y seniority.
     *
     * @param q texto de búsqueda opcional
     * @param nivel seniority seleccionado opcionalmente
     * @param model modelo utilizado para cargar la lista y los filtros
     * @return nombre de la plantilla de colaboradores
     */
    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(value = "q", required = false) String q,
                                @RequestParam(value = "nivel", required = false) String nivel,
                                Model model) {
        List<Colaborador> lista = (q == null || q.isBlank()) ? colaboradorRepository.findAll() : colaboradorRepository.buscar(q);
        if (nivel != null && !nivel.isBlank() && !"TODOS".equals(nivel)) {
            lista = lista.stream()
                    .filter(c -> c.getSeniority() != null && nivel.equals(c.getSeniority().name()))
                    .toList();
        }
        model.addAttribute("titulo", "Directorio de colaboradores");
        model.addAttribute("colaboradores", lista);
        model.addAttribute("q", q);
        model.addAttribute("nivelSeleccionado", nivel);
        return "pm/colaboradores";
    }

    /**
     * Presenta la disponibilidad y ocupación de los colaboradores.
     *
     * @param model modelo utilizado para cargar los datos de disponibilidad
     * @return nombre de la plantilla de disponibilidad
     */
    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        List<CargaColaboradorDTO> lista = cargas();
        model.addAttribute("titulo", "Disponibilidad de colaboradores");
        model.addAttribute("cargas", lista);
        model.addAttribute("disponibles", lista.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("ocupados", lista.stream().filter(c -> c.getDisponibilidad() < 50).count());
        return "pm/disponibilidad";
    }

    /**
     * Presenta las recomendaciones de talento y permite filtrarlas por proyecto.
     *
     * @param idProyecto identificador opcional del proyecto
     * @param model modelo utilizado para cargar recomendaciones y proyectos
     * @return nombre de la plantilla de recomendaciones
     */
    @GetMapping("/recomendaciones")
    public String recomendaciones(@RequestParam(value = "proyecto", required = false) Integer idProyecto,
                                  Model model) {
        var recomendaciones = recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc();
        if (idProyecto != null) {
            recomendaciones = recomendaciones.stream()
                    .filter(r -> r.getProyecto() != null && idProyecto.equals(r.getProyecto().getIdProyecto()))
                    .toList();
        }
        model.addAttribute("titulo", "Recomendaciones IA");
        model.addAttribute("recomendaciones", recomendaciones);
        model.addAttribute("proyectos", proyectoRepository.findAll());
        model.addAttribute("proyectoSeleccionado", idProyecto);
        return "pm/recomendaciones";
    }

    /**
     * Lista los foros aplicando filtros opcionales de texto, categoría y estado.
     *
     * @param q texto de búsqueda opcional
     * @param categoria categoría seleccionada opcionalmente
     * @param estado estado seleccionado opcionalmente
     * @param model modelo utilizado para cargar los resultados y filtros
     * @return nombre de la plantilla de foros
     */
    @GetMapping("/foros")
    public String foros(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "categoria", required = false) String categoria,
                        @RequestParam(value = "estado", required = false) String estado,
                        Model model) {
        List<Foro> foros = filtrarForos(q, categoria, estado);
        cargarForos(model, foros, q, categoria, estado);
        return "pm/foros";
    }

    /**
     * Filtra los foros por texto, categoría y estado.
     *
     * @param q texto de búsqueda opcional
     * @param categoria categoría requerida opcionalmente
     * @param estado estado requerido opcionalmente
     * @return lista de foros que cumplen los filtros
     */
    private List<Foro> filtrarForos(String q, String categoria, String estado) {
        return foroRepository.findAllByOrderByFechaCreacionDesc().stream().filter(f -> {
            boolean coincideTexto = q == null || q.isBlank()
                    || f.getTitulo().toLowerCase().contains(q.toLowerCase())
                    || f.getContenido().toLowerCase().contains(q.toLowerCase());
            boolean coincideCategoria = categoria == null || categoria.isBlank() || "TODAS".equals(categoria)
                    || categoria.equalsIgnoreCase(f.getCategoria());
            boolean coincideEstado = estado == null || estado.isBlank() || "TODOS".equals(estado)
                    || estado.equalsIgnoreCase(f.getEstado().name());
            return coincideTexto && coincideCategoria && coincideEstado;
        }).toList();
    }

    /**
     * Agrega al modelo los foros filtrados, opciones de filtro e indicadores.
     *
     * @param model modelo utilizado para cargar los atributos
     * @param foros foros que se mostrarán
     * @param q texto de búsqueda aplicado
     * @param categoria categoría aplicada
     * @param estado estado aplicado
     */
    private void cargarForos(Model model, List<Foro> foros, String q, String categoria, String estado) {
        model.addAttribute("titulo", "Foro del proyecto");
        model.addAttribute("foros", foros);
        model.addAttribute("q", q);
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("categoriasForo", ForoController.CATEGORIAS_FORO);
        model.addAttribute("totalForos", foroRepository.count());
        model.addAttribute("abiertos", foroRepository.findAll().stream().filter(f -> f.getEstado() == Foro.EstadoForo.ABIERTO).count());
        model.addAttribute("resueltos", foroRepository.findAll().stream().filter(f -> f.getEstado() == Foro.EstadoForo.RESUELTO).count());
        model.addAttribute("conteoRespuestas", foros.stream().collect(java.util.stream.Collectors.toMap(
                Foro::getIdForo, f -> respuestaForoRepository.countByForoIdForo(f.getIdForo()))));
    }

    /**
     * Presenta los proyectos, asignaciones y cargas consolidados en reportes.
     *
     * @param reporte nombre opcional del reporte seleccionado
     * @param model modelo utilizado para cargar los datos del reporte
     * @return nombre de la plantilla de reportes
     */
    @GetMapping("/reportes")
    public String reportes(@RequestParam(value = "reporte", required = false) String reporte,
                           Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of() : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        List<Asignacion> asignaciones = pm == null ? List.of() : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());
        model.addAttribute("titulo", "Reportes");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("totalProyectos", proyectos.size());
        model.addAttribute("proyectosActivos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("proyectosCompletados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
        model.addAttribute("totalAsignaciones", asignaciones.size());
        model.addAttribute("asignaciones", asignaciones);
        model.addAttribute("cargas", cargas());
        model.addAttribute("reporteSeleccionado", reporte);
        return "pm/reportes";
    }

    /**
     * Presenta el historial de proyectos administrados por el Project Manager.
     *
     * @param model modelo utilizado para cargar los proyectos e indicadores
     * @return nombre de la plantilla de historial
     */
    @GetMapping("/historial")
    public String historial(Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of() : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        model.addAttribute("titulo", "Historial de proyectos");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("finalizados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED || p.getEstado() == Proyecto.EstadoProyecto.CANCELLED).count());
        return "pm/historial";
    }

    /**
     * Exporta como CSV un reporte de proyectos, asignaciones, carga o historial.
     *
     * @param tipo tipo de reporte solicitado
     * @return respuesta CSV, prohibición de acceso o error si el tipo no es válido
     */
    @GetMapping(value = "/reportes/exportar", produces = "text/csv")
    public ResponseEntity<String> exportarReporte(@RequestParam("tipo") String tipo) {
        Usuario pm = pmDemo();
        if (pm == null) {
            return ResponseEntity.status(403).body("No se pudo identificar al Project Manager");
        }

        List<Proyecto> proyectos = proyectoRepository
                .findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        List<Asignacion> asignaciones = asignacionRepository
                .findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());

        StringBuilder csv = new StringBuilder("\uFEFF");
        String nombreArchivo;

        switch (tipo == null ? "" : tipo.toLowerCase()) {
            case "proyectos" -> {
                nombreArchivo = "reporte_proyectos_pm.csv";
                csv.append("Proyecto,Estado,Prioridad,Progreso,Vacantes,Inicio,Fin\n");
                for (Proyecto p : proyectos) {
                    csv.append(csv(p.getNombre())).append(',')
                            .append(csv(p.getEstado())).append(',')
                            .append(csv(p.getPrioridad())).append(',')
                            .append(csv(p.getProgreso())).append(',')
                            .append(csv(p.getVacantes())).append(',')
                            .append(csv(p.getFechaInicio())).append(',')
                            .append(csv(p.getFechaFin())).append('\n');
                }
            }
            case "asignaciones" -> {
                nombreArchivo = "reporte_asignaciones_pm.csv";
                csv.append("Proyecto,Colaborador,Rol,Dedicacion,Estado,Inicio,Fin\n");
                for (Asignacion a : asignaciones) {
                    csv.append(csv(a.getProyecto() == null ? "" : a.getProyecto().getNombre())).append(',')
                            .append(csv(a.getColaborador() == null || a.getColaborador().getUsuario() == null ? "" : a.getColaborador().getUsuario().getNombreCompleto())).append(',')
                            .append(csv(a.getRolProyecto())).append(',')
                            .append(csv(a.getPorcentajeDedicacion())).append(',')
                            .append(csv(a.getEstado())).append(',')
                            .append(csv(a.getFechaInicio())).append(',')
                            .append(csv(a.getFechaFin())).append('\n');
                }
            }
            case "carga" -> {
                nombreArchivo = "reporte_carga_pm.csv";
                csv.append("Colaborador,Cargo,Area,Carga,Disponibilidad\n");
                for (CargaColaboradorDTO c : cargas()) {
                    csv.append(csv(c.getColaborador())).append(',')
                            .append(csv(c.getCargo())).append(',')
                            .append(csv(c.getArea())).append(',')
                            .append(csv(c.getCarga())).append(',')
                            .append(csv(c.getDisponibilidad())).append('\n');
                }
            }
            case "historial" -> {
                nombreArchivo = "reporte_historial_pm.csv";
                csv.append("Proyecto,Estado,Progreso,Inicio,Fin\n");
                for (Proyecto p : proyectos) {
                    if (p.getEstado() == Proyecto.EstadoProyecto.COMPLETED
                            || p.getEstado() == Proyecto.EstadoProyecto.CANCELLED) {
                        csv.append(csv(p.getNombre())).append(',')
                                .append(csv(p.getEstado())).append(',')
                                .append(csv(p.getProgreso())).append(',')
                                .append(csv(p.getFechaInicio())).append(',')
                                .append(csv(p.getFechaFin())).append('\n');
                    }
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
