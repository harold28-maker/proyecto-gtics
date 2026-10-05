package pe.edu.pucp.skillbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("activas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA).count());
        model.addAttribute("planificadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA).count());
        model.addAttribute("finalizadas", lista.stream().filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA).count());
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
}
