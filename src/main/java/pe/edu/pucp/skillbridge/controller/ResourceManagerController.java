package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.Clasificacion;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.RecomendacionIARepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import pe.edu.pucp.skillbridge.util.ReportePdf;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Controlador MVC que administra recursos, asignaciones, disponibilidad,
 * talento, historial y reportes.
 */
@Controller
@RequestMapping("/resource")
public class ResourceManagerController {

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
    public String inicio(Model model) {
        List<CargaColaboradorDTO> cargas = capacidadColaboradorService.consultarEquipo();
        model.addAttribute("titulo", "Inicio");
        model.addAttribute("totalColaboradores", cargas.size());
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0 : Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("cargas", cargas.stream().limit(6).toList());
        model.addAttribute("recomendaciones", recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc().stream().limit(3).toList());
        return "resource/inicio";
    }

    /**
     * Lista los colaboradores aplicando filtros opcionales por texto y seniority.
     *
     * @param q texto de búsqueda opcional
     * @param nivel seniority seleccionado opcionalmente
     * @param model modelo utilizado para cargar los datos de la vista
     * @return nombre de la plantilla de colaboradores
     */
    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(value = "q", required = false) String q,
                                @RequestParam(value = "nivel", required = false) String nivel,
                                Model model) {
        List<Colaborador> lista = (q == null || q.isBlank())
                ? colaboradorRepository.findAll()
                : colaboradorRepository.buscar(q);
        if (nivel != null && !nivel.isBlank() && !"TODOS".equals(nivel)) {
            lista = lista.stream()
                    .filter(c -> c.getSeniority() != null && nivel.equals(c.getSeniority().name()))
                    .toList();
        }
        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaboradores", lista);
        model.addAttribute("q", q);
        model.addAttribute("nivelSeleccionado", nivel);
        return "resource/colaboradores";
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
     * Prepara el formulario vacío para crear una asignación.
     *
     * @param model modelo utilizado para cargar los datos del formulario
     * @return nombre de la plantilla del formulario
     */
    @GetMapping("/asignaciones/nueva")
    public String nuevaAsignacion(Model model) {
        prepararFormularioAsignacion(new Asignacion(), "Nueva asignación", model);
        return "resource/asignacion-form";
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
        return "resource/asignacion-form";
    }

    /**
     * Carga una asignación y las listas necesarias en el formulario.
     *
     * @param asignacion asignación que se editará o creará
     * @param titulo título que se mostrará en el formulario
     * @param model modelo utilizado para cargar los datos del formulario
     */
    private void prepararFormularioAsignacion(Asignacion asignacion, String titulo, Model model) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("asignacion", asignacion);
        model.addAttribute("proyectos", proyectoRepository.findAll());
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
            return "resource/asignacion-form";
        }
        asignacionRepository.save(asignacion);
        cargarAsignaciones(model, asignacionRepository.findAll());
        model.addAttribute("mensajeExito", "Asignación guardada correctamente.");
        return "resource/asignaciones";
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
        cargarAsignaciones(model, asignacionRepository.findAll());
        model.addAttribute("mensajeExito", "Estado de la asignación actualizado.");
        return "resource/asignaciones";
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
     * Genera PDF usando los datos ya preparados para la exportación CSV.
     * Respeta la sesión del rol y los permisos del interceptor de reportes.
     */
    @GetMapping(value = "/reportes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@RequestParam("tipo") String tipo) {
        String clave = tipo == null ? "" : tipo.toLowerCase(java.util.Locale.ROOT);
        String titulo = switch (clave) {
            case "capacidad" -> "Capacidad del equipo";
            case "asignaciones" -> "Asignaciones de recursos";
            case "talento" -> "Talent Matching";
            case "proyectos" -> "Proyectos de recursos";
            default -> null;
        };
        if (titulo == null) return ResponseEntity.badRequest().build();
        ResponseEntity<String> exportacion = exportarReporte(clave);
        if (!exportacion.getStatusCode().is2xxSuccessful() || exportacion.getBody() == null) {
            return ResponseEntity.status(exportacion.getStatusCode()).build();
        }
        byte[] pdf = ReportePdf.desdeCsv(titulo, exportacion.getBody());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_resource_" + clave + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(pdf);
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
