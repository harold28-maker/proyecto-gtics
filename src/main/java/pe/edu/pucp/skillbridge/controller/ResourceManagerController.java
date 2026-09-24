package pe.edu.pucp.skillbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.RecomendacionIARepository;

import java.util.ArrayList;
import java.util.List;

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

    @RequestMapping(value = "/inicio", method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        List<CargaColaboradorDTO> cargas = cargarDatos();
        model.addAttribute("titulo", "Inicio - Resource Manager");
        model.addAttribute("totalColaboradores", cargas.size());
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("sobrecargados", cargas.stream().filter(c -> c.getCarga() > c.getDisponibilidadBase()).count());
        model.addAttribute("cargaPromedio", cargas.isEmpty() ? 0
                : Math.round(cargas.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        model.addAttribute("cargas", cargas.stream().limit(6).toList());
        model.addAttribute("recomendaciones", recomendacionIARepository
                .findAllByOrderByPorcentajeMatchDesc().stream().limit(3).toList());
        return "resource/inicio";
    }

    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Colaborador> lista = (q == null || q.isBlank())
                ? colaboradorRepository.findAll()
                : colaboradorRepository.buscar(q);
        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaboradores", lista);
        model.addAttribute("q", q);
        return "resource/colaboradores";
    }

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "estado", required = false) String estado,
                               Model model) {
        List<Asignacion> lista = asignacionRepository.findAll();

        if (q != null && !q.isBlank()) {
            String texto = q.toLowerCase();
            lista = lista.stream().filter(a ->
                    a.getColaborador().getUsuario().getNombreCompleto().toLowerCase().contains(texto)
                            || a.getProyecto().getNombre().toLowerCase().contains(texto)
                            || (a.getRolProyecto() != null && a.getRolProyecto().toLowerCase().contains(texto))
            ).toList();
        }

        if (estado != null && !estado.isBlank()) {
            lista = lista.stream()
                    .filter(a -> a.getEstado().name().equalsIgnoreCase(estado))
                    .toList();
        }

        cargarAsignaciones(model, lista);
        model.addAttribute("q", q);
        model.addAttribute("estadoFiltro", estado);
        return "resource/asignaciones";
    }

    private void cargarAsignaciones(Model model, List<Asignacion> lista) {
        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
    }

    @GetMapping("/asignaciones/nueva")
    public String nuevaAsignacion(Model model) {
        Asignacion asignacion = new Asignacion();
        asignacion.setEstado(Asignacion.EstadoAsignacion.PLANIFICADA);
        prepararFormularioAsignacion(asignacion, "Nueva asignación", model);
        return "resource/asignacion-form";
    }

    @GetMapping("/asignaciones/editar/{id}")
    public String editarAsignacion(@PathVariable Integer id, Model model) {
        Asignacion asignacion = asignacionRepository.findById(id).orElseThrow();
        prepararFormularioAsignacion(asignacion, "Editar asignación", model);
        return "resource/asignacion-form";
    }

    private void prepararFormularioAsignacion(Asignacion asignacion, String titulo, Model model) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("asignacion", asignacion);
        model.addAttribute("proyectos", proyectoRepository.findAll());
        model.addAttribute("colaboradores", colaboradorRepository.findAll());
    }

    @PostMapping("/asignaciones/guardar")
    public String guardarAsignacion(Asignacion asignacion,
                                    @RequestParam("idProyecto") Integer idProyecto,
                                    @RequestParam("idColaborador") Integer idColaborador,
                                    RedirectAttributes redirectAttributes,
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
            return "resource/asignacion-form";
        }

        asignacionRepository.save(asignacion);
        redirectAttributes.addFlashAttribute("mensajeExito", "Asignación guardada correctamente.");
        return "redirect:/resource/asignaciones";
    }

    @PostMapping("/asignaciones/{id}/estado")
    public String cambiarEstadoAsignacion(@PathVariable Integer id,
                                           @RequestParam("estado") String estado,
                                           RedirectAttributes redirectAttributes) {
        Asignacion asignacion = asignacionRepository.findById(id).orElseThrow();
        asignacion.setEstado(Asignacion.EstadoAsignacion.valueOf(estado));
        asignacionRepository.save(asignacion);
        redirectAttributes.addFlashAttribute("mensajeExito", "Estado de la asignación actualizado.");
        return "redirect:/resource/asignaciones";
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

    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        List<CargaColaboradorDTO> lista = cargarDatos();
        model.addAttribute("titulo", "Disponibilidad");
        model.addAttribute("cargas", lista);
        model.addAttribute("disponibles", lista.stream().filter(c -> c.getDisponibilidad() >= 50).count());
        model.addAttribute("parciales", lista.stream()
                .filter(c -> c.getDisponibilidad() > 0 && c.getDisponibilidad() < 50).count());
        model.addAttribute("sinDisponibilidad", lista.stream().filter(c -> c.getDisponibilidad() == 0).count());
        return "resource/disponibilidad";
    }

    @GetMapping("/carga")
    public String carga(Model model) {
        List<CargaColaboradorDTO> lista = cargarDatos();
        model.addAttribute("titulo", "Carga de trabajo");
        model.addAttribute("cargas", lista);
        model.addAttribute("cargaPromedio", lista.isEmpty() ? 0
                : Math.round(lista.stream().mapToInt(CargaColaboradorDTO::getCarga).average().orElse(0)));
        return "resource/carga";
    }

    @GetMapping("/talent")
    public String talent(Model model) {
        model.addAttribute("titulo", "Talent Matching IA");
        model.addAttribute("recomendaciones", recomendacionIARepository.findAllByOrderByPorcentajeMatchDesc());
        model.addAttribute("proyectos", proyectoRepository.findAll());
        return "resource/talent";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        model.addAttribute("titulo", "Proyectos anteriores");
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("completados", proyectos.stream()
                .filter(p -> p.getEstado() == Proyecto.EstadoProyecto.COMPLETED).count());
        model.addAttribute("cancelados", proyectos.stream()
                .filter(p -> p.getEstado() == Proyecto.EstadoProyecto.CANCELLED).count());
        return "resource/historial";
    }
}
