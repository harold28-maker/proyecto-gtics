package pe.edu.pucp.skillbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService.Acceso;
import pe.edu.pucp.skillbridge.service.AsistenteForoService;

import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/colaborador/asistente")
public class AsistenteColaboradorController {
    private final AccesoProyectosService accesoProyectos;
    private final AsistenteForoService asistente;

    public AsistenteColaboradorController(AccesoProyectosService accesoProyectos, AsistenteForoService asistente) {
        this.accesoProyectos = accesoProyectos;
        this.asistente = asistente;
    }

    @GetMapping
    public String inicio(@RequestAttribute("usuarioVerificado") Usuario usuario,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        prepararVista(usuario, bloqueados, model);
        return "colaborador/asistente";
    }

    @PostMapping
    public String consultar(@RequestParam(value = "idProyecto", defaultValue = "") String idProyecto,
                            @RequestParam(value = "pregunta", defaultValue = "") String pregunta,
                            @RequestAttribute("usuarioVerificado") Usuario usuario,
                            @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Map<Integer, Acceso> disponibles = prepararVista(usuario, bloqueados, model);
        Acceso acceso = proyectoSeleccionado(disponibles, idProyecto);
        String texto = pregunta.strip();
        model.addAttribute("pregunta", pregunta);
        if (acceso == null) {
            model.addAttribute("errorProyecto", "Selecciona uno de los proyectos disponibles.");
        }
        if (texto.isBlank() || texto.length() > 1000) {
            model.addAttribute("errorPregunta", texto.isBlank() ? "Escribe una pregunta."
                    : "La pregunta admite hasta 1000 caracteres.");
        }
        if (acceso != null) {
            model.addAttribute("proyectoSeleccionado", acceso.proyecto());
            if (!model.containsAttribute("errorPregunta")) {
                model.addAttribute("resultado", asistente.consultar(acceso.proyecto(), texto));
            }
        }
        return "colaborador/asistente";
    }

    private Map<Integer, Acceso> prepararVista(Usuario usuario, Set<String> bloqueados, Model model) {
        boolean puedeConsultar = !bloqueados.contains("foros");
        Map<Integer, Acceso> disponibles = puedeConsultar ? accesoProyectos.consultar(usuario, bloqueados) : Map.of();
        model.addAttribute("titulo", "Asistente de IA");
        model.addAttribute("proyectos", disponibles.values().stream().map(Acceso::proyecto).toList());
        model.addAttribute("puedeConsultarForo", puedeConsultar);
        return disponibles;
    }

    private Acceso proyectoSeleccionado(Map<Integer, Acceso> disponibles, String id) {
        try {
            return disponibles.get(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
