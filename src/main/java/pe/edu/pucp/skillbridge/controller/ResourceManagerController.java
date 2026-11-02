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
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.RecomendacionIARepository;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/resource")
public class ResourceManagerController {

    private final ColaboradorRepository colaboradorRepository;
    private final RecomendacionIARepository recomendacionIARepository;
    private final AsignacionRepository asignacionRepository;
    private final ProyectoRepository proyectoRepository;

    public ResourceManagerController(ColaboradorRepository colaboradorRepository,
                                     RecomendacionIARepository recomendacionIARepository,
                                     AsignacionRepository asignacionRepository,
                                     ProyectoRepository proyectoRepository) {
        this.colaboradorRepository = colaboradorRepository;
        this.recomendacionIARepository = recomendacionIARepository;
        this.asignacionRepository = asignacionRepository;
        this.proyectoRepository = proyectoRepository;
    }

    private Integer intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private List<CargaColaboradorDTO> cargarDatos() {
        List<CargaColaboradorDTO> resultado = new ArrayList<>();
        for (Object[] fila : colaboradorRepository.obtenerCargaColaboradores()) {
            resultado.add(new CargaColaboradorDTO(
                    intValue(fila[0]),
                    String.valueOf(fila[1]),
                    fila[2] == null ? "-" : String.valueOf(fila[2]),
                    fila[3] == null ? "-" : String.valueOf(fila[3]),
                    intValue(fila[4]),
                    intValue(fila[5]),
                    intValue(fila[6])
            ));
        }
        return resultado;
    }

    @RequestMapping(value = {"/inicio", "/dashboard"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        List<CargaColaboradorDTO> cargas = cargarDatos();
        model.addAttribute("titulo", "Dashboard - Resource Manager");
        model.addAttribute("totalColaboradores", cargas.size());
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0 : Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("cargas", cargas.stream().limit(6).toList());
        model.addAttribute("recomendaciones", recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc().stream().limit(3).toList());
        return "resource/inicio";
    }

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

    @GetMapping("/asignaciones")
    public String asignaciones(Model model) {
        List<Asignacion> lista = asignacionRepository.findAll();
        cargarAsignaciones(model, lista);
        return "resource/asignaciones";
    }

    private void cargarAsignaciones(Model model, List<Asignacion> lista) {
        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
    }

    @GetMapping("/asignaciones/nueva")
    public String nuevaAsignacion(Model model) {
        prepararFormularioAsignacion(new Asignacion(), "Nueva asignación", model);
        return "resource/asignacion-form";
    }

    @GetMapping("/asignaciones/editar/{id}")
    public String editarAsignacion(@PathVariable Integer id, Model model) {
        prepararFormularioAsignacion(asignacionRepository.findById(id).orElseThrow(), "Editar asignación", model);
        return "resource/asignacion-form";
    }

    private void prepararFormularioAsignacion(Asignacion asignacion, String titulo, Model model) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("asignacion", asignacion);
        model.addAttribute("proyectos", proyectoRepository.findAll());
        model.addAttribute("colaboradores", colaboradorRepository.findAll());
    }

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

    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        List<CargaColaboradorDTO> lista = cargarDatos();
        model.addAttribute("titulo", "Disponibilidad");
        model.addAttribute("cargas", lista);
        model.addAttribute("disponibles", lista.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("parciales", lista.stream().filter(c -> c.getDisponibilidad() > 0 && c.getDisponibilidad() < 50).count());
        model.addAttribute("sinDisponibilidad", lista.stream().filter(c -> c.getDisponibilidad() == 0).count());
        return "resource/disponibilidad";
    }

    @GetMapping("/carga")
    public String carga(Model model) {
        List<CargaColaboradorDTO> lista = cargarDatos();
        model.addAttribute("titulo", "Carga de trabajo");
        model.addAttribute("cargas", lista);
        model.addAttribute("cargaPromedio", lista.isEmpty() ? 0 : Math.round(lista.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        return "resource/carga";
    }

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

    @GetMapping("/historial")
    public String historial(Model model) {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        model.addAttribute("titulo", "Proyectos anteriores");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("completados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
        model.addAttribute("cancelados", proyectos.stream().filter(p -> p.getEstado() == Proyecto.EstadoProyecto.CANCELLED).count());
        return "resource/historial";
    }

    @GetMapping("/reportes")
    public String reportes(@RequestParam(value = "reporte", required = false) String reporte,
                           Model model) {
        List<CargaColaboradorDTO> cargas = cargarDatos();
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
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("sinDisponibilidad", cargas.stream().filter(c -> c.getDisponibilidad() == 0).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0 :
                Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("asignacionesActivas", asignaciones.stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("proyectosActivos", proyectos.stream()
                .filter(p -> p.getEstado() == Proyecto.EstadoProyecto.ACTIVE).count());

        return "resource/reportes";
    }


    @GetMapping(value = "/reportes/exportar", produces = "text/csv")
    public ResponseEntity<String> exportarReporte(@RequestParam("tipo") String tipo) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        String nombreArchivo;

        switch (tipo == null ? "" : tipo.toLowerCase()) {
            case "capacidad" -> {
                nombreArchivo = "reporte_capacidad.csv";
                csv.append("Colaborador,Cargo,Area,Disponibilidad base,Carga,Disponibilidad\n");
                for (CargaColaboradorDTO c : cargarDatos()) {
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


    private String csv(Object valor) {
        String texto = valor == null ? "" : String.valueOf(valor);
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

}
