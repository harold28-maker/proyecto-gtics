package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadProyectoDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.util.ArrayList;
import java.util.List;

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

    private int intValue(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

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

    @RequestMapping(value = "/inicio", method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of()
                : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        List<Asignacion> asignaciones = pm == null ? List.of()
                : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());

        model.addAttribute("titulo", "Workspace de proyectos");
        model.addAttribute("pm", pm);
        model.addAttribute("proyectosActivos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("proyectosPlanning", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.PLANNING).count());
        model.addAttribute("asignaciones", asignaciones.size());
        model.addAttribute("colaboradores", asignaciones.stream().map(a -> a.getColaborador().getIdColaborador()).distinct().count());
        model.addAttribute("proyectos", proyectos.stream().limit(5).toList());
        model.addAttribute("ultimasAsignaciones", asignaciones.stream().limit(5).toList());
        return "pm/inicio";
    }

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

    private void cargarProyectos(Model model, List<Proyecto> proyectos, String q) {
        model.addAttribute("titulo", "Portafolio de proyectos");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("q", q);
        model.addAttribute("activos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("planning", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.PLANNING).count());
        model.addAttribute("completados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
    }

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

    @GetMapping("/proyectos/editar/{id}")
    public String editarProyecto(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("titulo", "Editar proyecto");
        model.addAttribute("proyecto", proyectoRepository.findById(id).orElseThrow());
        return "pm/proyecto-form";
    }

    @PostMapping("/proyectos/guardar")
    public String guardarProyecto(Proyecto proyecto, Model model) {
        proyecto.setProjectManager(pmDemo());
        proyectoRepository.save(proyecto);
        cargarProyectos(model,
                proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pmDemo().getIdUsuario()), null);
        model.addAttribute("mensajeExito", "Proyecto guardado correctamente.");
        return "pm/proyectos";
    }

    // Baja lógica del proyecto: conserva historial y relaciones.
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

    @GetMapping("/proyectos/{id}/habilidades")
    public String habilidadesRequeridas(@PathVariable Integer id, Model model) {
        Proyecto proyecto = proyectoRepository.findById(id).orElseThrow();
        model.addAttribute("titulo", "Habilidades requeridas");
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("requerimientos", habilidadesProyecto(id));
        model.addAttribute("catalogo", habilidadRepository.findByEstadoTrueOrderByNombreAsc());
        return "pm/proyecto-habilidades";
    }

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

    @PostMapping("/proyectos/{id}/habilidades/{idHabilidad}/eliminar")
    public String eliminarHabilidadProyecto(@PathVariable Integer id,
                                             @PathVariable Integer idHabilidad,
                                             Model model) {
        proyectoRepository.eliminarHabilidadRequerida(id, idHabilidad);
        model.addAttribute("mensajeExito", "Habilidad requerida retirada del proyecto.");
        return habilidadesRequeridas(id, model);
    }

    @GetMapping("/asignaciones")
    public String asignaciones(Model model) {
        Usuario pm = pmDemo();
        List<Asignacion> lista = pm == null ? List.of()
                : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());
        cargarAsignaciones(model, lista);
        return "pm/asignaciones";
    }

    private void cargarAsignaciones(Model model, List<Asignacion> lista) {
        model.addAttribute("titulo", "Asignaciones del equipo");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
    }

    @GetMapping("/asignaciones/nueva")
    public String nuevaAsignacion(Model model) {
        prepararFormularioAsignacion(new Asignacion(), "Nueva asignación", model);
        return "pm/asignacion-form";
    }

    @GetMapping("/asignaciones/editar/{id}")
    public String editarAsignacion(@PathVariable Integer id, Model model) {
        prepararFormularioAsignacion(asignacionRepository.findById(id).orElseThrow(), "Editar asignación", model);
        return "pm/asignacion-form";
    }

    private void prepararFormularioAsignacion(Asignacion asignacion, String titulo, Model model) {
        Usuario pm = pmDemo();
        model.addAttribute("titulo", titulo);
        model.addAttribute("asignacion", asignacion);
        model.addAttribute("proyectos", pm == null ? List.of()
                : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario()));
        model.addAttribute("colaboradores", colaboradorRepository.findAll());
    }

    @PostMapping("/asignaciones/guardar")
    public String guardarAsignacion(Asignacion asignacion,
                                    @RequestParam("idProyecto") Integer idProyecto,
                                    @RequestParam("idColaborador") Integer idColaborador,
                                    Model model) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto).orElseThrow();
        Colaborador colaborador = colaboradorRepository.findById(idColaborador).orElseThrow();
        asignacion.setProyecto(proyecto);
        asignacion.setColaborador(colaborador);

        String error = validarAsignacion(asignacion, colaborador);
        if (error != null) {
            prepararFormularioAsignacion(asignacion,
                    asignacion.getIdAsignacion() == null ? "Nueva asignación" : "Editar asignación", model);
            model.addAttribute("mensajeError", error);
            return "pm/asignacion-form";
        }

        asignacionRepository.save(asignacion);
        cargarAsignaciones(model,
                asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pmDemo().getIdUsuario()));
        model.addAttribute("mensajeExito", "Asignación guardada correctamente.");
        return "pm/asignaciones";
    }

    private String validarAsignacion(Asignacion asignacion, Colaborador colaborador) {
        if (asignacion.getPorcentajeDedicacion() == null
                || asignacion.getPorcentajeDedicacion() < 1
                || asignacion.getPorcentajeDedicacion() > 100) {
            return "El porcentaje de dedicación debe estar entre 1 y 100%.";
        }

        if (asignacion.getFechaInicio() == null) {
            return "La fecha de inicio es obligatoria.";
        }

        if (asignacion.getFechaFin() != null
                && asignacion.getFechaFin().isBefore(asignacion.getFechaInicio())) {
            return "La fecha de fin no puede ser anterior a la fecha de inicio.";
        }

        if (asignacion.getEstado() == Asignacion.EstadoAsignacion.ACTIVA
                || asignacion.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA) {
            int cargaActual = asignacionRepository
                    .findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador())
                    .stream()
                    .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA
                            || a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA)
                    .filter(a -> asignacion.getIdAsignacion() == null
                            || !asignacion.getIdAsignacion().equals(a.getIdAsignacion()))
                    .mapToInt(a -> a.getPorcentajeDedicacion() == null ? 0 : a.getPorcentajeDedicacion())
                    .sum();

            int limite = colaborador.getDisponibilidadBase() == null ? 100 : colaborador.getDisponibilidadBase();
            if (cargaActual + asignacion.getPorcentajeDedicacion() > limite) {
                return "La asignación supera la disponibilidad del colaborador. Disponible: "
                        + Math.max(limite - cargaActual, 0) + "%.";
            }
        }

        return null;
    }

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

    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Colaborador> lista = (q == null || q.isBlank()) ? colaboradorRepository.findAll() : colaboradorRepository.buscar(q);
        model.addAttribute("titulo", "Directorio de colaboradores");
        model.addAttribute("colaboradores", lista);
        model.addAttribute("q", q);
        return "pm/colaboradores";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        List<CargaColaboradorDTO> lista = cargas();
        model.addAttribute("titulo", "Disponibilidad de colaboradores");
        model.addAttribute("cargas", lista);
        model.addAttribute("disponibles", lista.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("ocupados", lista.stream().filter(c -> c.getDisponibilidad() < 50).count());
        return "pm/disponibilidad";
    }

    @GetMapping("/recomendaciones")
    public String recomendaciones(Model model) {
        model.addAttribute("titulo", "Recomendaciones IA");
        model.addAttribute("recomendaciones", recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc());
        model.addAttribute("proyectos", proyectoRepository.findAll());
        return "pm/recomendaciones";
    }

    @GetMapping("/foros")
    public String foros(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "categoria", required = false) String categoria,
                        @RequestParam(value = "estado", required = false) String estado,
                        Model model) {
        List<Foro> foros = filtrarForos(q, categoria, estado);
        cargarForos(model, foros, q, categoria, estado);
        return "pm/foros";
    }

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

    @GetMapping("/reportes")
    public String reportes(Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of() : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        List<Asignacion> asignaciones = pm == null ? List.of() : asignacionRepository.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());
        model.addAttribute("titulo", "Reportes");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("totalProyectos", proyectos.size());
        model.addAttribute("proyectosActivos", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());
        model.addAttribute("proyectosCompletados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
        model.addAttribute("totalAsignaciones", asignaciones.size());
        return "pm/reportes";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        Usuario pm = pmDemo();
        List<Proyecto> proyectos = pm == null ? List.of() : proyectoRepository.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
        model.addAttribute("titulo", "Historial de proyectos");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("finalizados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED || p.getEstado() == Proyecto.EstadoProyecto.CANCELLED).count());
        return "pm/historial";
    }
}
