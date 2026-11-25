package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.dto.ConsultaTalentoFormulario;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.ConsultaFallida;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.Interpretacion;
import pe.edu.pucp.skillbridge.service.GestionAsignacionesService;
import pe.edu.pucp.skillbridge.service.TalentoMatchingService;
import pe.edu.pucp.skillbridge.service.TalentoMatchingService.Resultado;
import pe.edu.pucp.skillbridge.service.SesionTalentoService;
import pe.edu.pucp.skillbridge.service.SesionTalentoService.Conversacion;
import pe.edu.pucp.skillbridge.service.SesionTalentoService.Turno;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping({"/resource/talent", "/pm/recomendaciones"})
public class TalentoController {
    private static final int TURNOS_VISIBLES = 8;

    private final GeminiTalentoService gemini;
    private final TalentoMatchingService matching;
    private final GestionAsignacionesService asignaciones;
    private final SesionTalentoService sesiones;

    public TalentoController(GeminiTalentoService gemini, TalentoMatchingService matching,
                             GestionAsignacionesService asignaciones, SesionTalentoService sesiones) {
        this.gemini = gemini;
        this.matching = matching;
        this.asignaciones = asignaciones;
        this.sesiones = sesiones;
    }

    @InitBinder("consultaTalento")
    public void limpiarMensaje(WebDataBinder binder) {
        binder.setAllowedFields("idProyecto", "desde", "hasta", "dedicacion", "mensaje");
        binder.registerCustomEditor(String.class, "mensaje", new StringTrimmerEditor(true));
    }

    @GetMapping
    public String inicio(@RequestParam(value = "proyecto", defaultValue = "") String proyecto,
                         @RequestAttribute("usuarioVerificado") Usuario gestor,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                         HttpSession sesion, Model model) {
        ConsultaTalentoFormulario formulario = new ConsultaTalentoFormulario();
        List<Proyecto> proyectos = asignaciones.proyectosVigentes(gestor);
        Conversacion actual = sesiones.obtener(sesion, gestor);
        Integer id = proyecto.isBlank() && actual != null ? actual.idProyecto() : leerId(proyecto);
        boolean valido = proyectoDisponible(proyectos, id);
        if (!proyecto.isBlank() && !valido) {
            model.addAttribute("mensajeError", "Selecciona un proyecto vigente de la lista.");
        }
        if (valido) formulario.setIdProyecto(id);

        Conversacion previa = valido && actual != null && actual.idProyecto().equals(id) ? actual : null;
        if (previa != null) {
            formulario.setDesde(previa.desde());
            formulario.setHasta(previa.hasta());
            formulario.setDedicacion(previa.dedicacion());
        }
        model.addAttribute("consultaTalento", formulario);
        return vista(gestor, bloqueados, proyectos, previa, model);
    }

    @PostMapping
    public String consultar(@Valid @ModelAttribute("consultaTalento") ConsultaTalentoFormulario formulario,
                            BindingResult errores, @RequestAttribute("usuarioVerificado") Usuario gestor,
                            @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                            HttpSession sesion, Model model) {
        List<Proyecto> proyectos = asignaciones.proyectosVigentes(gestor);
        boolean proyectoValido = proyectoDisponible(proyectos, formulario.getIdProyecto());
        if (!errores.hasFieldErrors("idProyecto") && !proyectoValido) {
            errores.rejectValue("idProyecto", "talento.proyecto", "Selecciona un proyecto vigente de la lista.");
        }
        Conversacion actual = sesiones.obtener(sesion, gestor);
        Conversacion previa = proyectoValido && actual != null && actual.idProyecto().equals(formulario.getIdProyecto())
                ? actual : null;
        if (errores.hasErrors()) return vista(gestor, bloqueados, proyectos, previa, model);

        try {
            Interpretacion interpretacion = gemini.interpretar(formulario.getMensaje(),
                    previa == null ? null : previa.criterios(), matching.catalogoActivo());
            Integer dedicacion = interpretacion.dedicacion() == null
                    ? formulario.getDedicacion() : Integer.valueOf(interpretacion.dedicacion().intValueExact());
            Resultado resultado = matching.buscar(interpretacion, formulario.getDesde(), formulario.getHasta(), dedicacion,
                    !bloqueados.contains("historial"));
            List<Turno> turnos = new ArrayList<>(previa == null ? List.of() : previa.turnos());
            turnos.add(new Turno(formulario.getMensaje(), resultado.respuesta()));
            int inicio = Math.max(0, turnos.size() - TURNOS_VISIBLES);
            Interpretacion criterios = interpretacion.pideAclaracion() && previa != null
                    ? previa.criterios() : interpretacion;
            Conversacion nueva = new Conversacion(UUID.randomUUID().toString(), gestor.getIdUsuario(), formulario.getIdProyecto(),
                    formulario.getDesde(), formulario.getHasta(), dedicacion, criterios, interpretacion,
                    List.copyOf(turnos.subList(inicio, turnos.size())));
            synchronized (sesion) {
                if (sesiones.obtener(sesion, gestor) == actual) {
                    sesiones.guardar(sesion, gestor, nueva);
                    return "redirect:" + ruta(gestor);
                }
            }
            model.addAttribute("mensajeError", "La conversación se actualizó durante la consulta. Vuelve a enviar el mensaje.");
            Conversacion reciente = sesiones.obtener(sesion, gestor);
            previa = reciente != null && reciente.idProyecto().equals(formulario.getIdProyecto()) ? reciente : null;
        } catch (ConsultaFallida ex) {
            model.addAttribute("mensajeError", ex.getMessage());
        }
        return vista(gestor, bloqueados, proyectos, previa, model);
    }

    @PostMapping("/nueva")
    public String nueva(@RequestAttribute("usuarioVerificado") Usuario gestor, HttpSession sesion) {
        synchronized (sesion) {
            sesiones.limpiar(sesion, gestor);
        }
        return "redirect:" + ruta(gestor);
    }

    @PostMapping("/preparar")
    public String preparar(@RequestParam(value = "idColaborador", defaultValue = "") String idColaborador,
                           @RequestParam(value = "consulta", defaultValue = "") String consulta,
                           @RequestAttribute("usuarioVerificado") Usuario gestor,
                           @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                           HttpSession sesion, RedirectAttributes redirect) {
        if (bloqueados.contains("asignaciones")) {
            redirect.addFlashAttribute("mensajeError", "Contacta a Administración para habilitar la gestión de asignaciones.");
            return "redirect:" + ruta(gestor);
        }
        Conversacion actual = sesiones.obtener(sesion, gestor);
        Integer id = leerId(idColaborador);
        if (actual == null || !actual.revision().equals(consulta) || id == null
                || !proyectoDisponible(asignaciones.proyectosVigentes(gestor), actual.idProyecto())
                || sesiones.resultado(actual, !bloqueados.contains("historial")).candidatos().stream().noneMatch(c -> c.idColaborador().equals(id))) {
            redirect.addFlashAttribute("mensajeError", "Elige un candidato disponible en la última consulta.");
            return "redirect:" + ruta(gestor);
        }

        AsignacionFormulario formulario = new AsignacionFormulario();
        formulario.setIdProyecto(actual.idProyecto());
        formulario.setIdColaborador(id);
        formulario.setFechaInicio(actual.desde());
        formulario.setFechaFin(actual.hasta());
        formulario.setPorcentajeDedicacion(actual.dedicacion());
        redirect.addFlashAttribute("asignacion", formulario);
        return "redirect:" + (esProjectManager(gestor) ? "/pm" : "/resource") + "/asignaciones/nueva";
    }

    private String vista(Usuario gestor, Set<String> bloqueados, List<Proyecto> proyectos, Conversacion actual,
                         Model model) {
        boolean pm = esProjectManager(gestor);
        model.addAttribute("titulo", pm ? "Recomendaciones" : "Asignación de talento");
        model.addAttribute("rolVista", pm ? "PM" : "RM");
        model.addAttribute("rutaTalento", ruta(gestor));
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("iaConfigurada", gemini.configurado());
        model.addAttribute("verCapacidad", !bloqueados.contains(pm ? "disponibilidad" : "capacidad"));
        model.addAttribute("verColaboradores", !bloqueados.contains("colaboradores"));
        model.addAttribute("puedeAsignar", !bloqueados.contains("asignaciones"));
        model.addAttribute("turnos", actual == null ? List.of() : actual.turnos());
        if (actual != null) {
            model.addAttribute("resultado", sesiones.resultado(actual, !bloqueados.contains("historial")));
            model.addAttribute("desdeConsultado", actual.desde());
            model.addAttribute("hastaConsultado", actual.hasta());
            model.addAttribute("consultaActual", actual.revision());
        }
        return pm ? "pm/recomendaciones" : "resource/talent";
    }

    private boolean proyectoDisponible(List<Proyecto> proyectos, Integer id) {
        return id != null && proyectos.stream().anyMatch(p -> p.getIdProyecto().equals(id));
    }

    private Integer leerId(String valor) {
        try {
            int id = Integer.parseInt(valor.strip());
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean esProjectManager(Usuario gestor) {
        return "PROJECT_MANAGER".equals(gestor.getRol().getNombre());
    }

    private String ruta(Usuario gestor) {
        return esProjectManager(gestor) ? "/pm/recomendaciones" : "/resource/talent";
    }
}
