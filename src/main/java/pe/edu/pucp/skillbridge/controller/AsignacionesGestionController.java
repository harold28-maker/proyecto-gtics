package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;
import pe.edu.pucp.skillbridge.service.GestionAsignacionesService;
import pe.edu.pucp.skillbridge.service.GestionAsignacionesService.CampoInvalido;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping({"/resource/asignaciones", "/pm/asignaciones"})
public class AsignacionesGestionController {
    private final GestionAsignacionesService gestion;
    private final ColaboradorRepository colaboradores;
    private final CapacidadColaboradorService capacidad;

    public AsignacionesGestionController(GestionAsignacionesService gestion, ColaboradorRepository colaboradores,
                                         CapacidadColaboradorService capacidad) {
        this.gestion = gestion;
        this.colaboradores = colaboradores;
        this.capacidad = capacidad;
    }

    @InitBinder("asignacion")
    public void limpiarRol(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "rolProyecto", new StringTrimmerEditor(true));
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(value = "idColaborador", defaultValue = "") String idColaborador,
                        @RequestAttribute("usuarioVerificado") Usuario gestor, Model model) {
        AsignacionFormulario valores = model.getAttribute("asignacion") instanceof AsignacionFormulario preparada
                ? preparada : new AsignacionFormulario();
        if (valores.getIdColaborador() == null && !idColaborador.isBlank()) {
            try {
                int id = Integer.parseInt(idColaborador);
                if (id > 0 && colaboradores.existsById(id)) valores.setIdColaborador(id);
            } catch (NumberFormatException ex) {
                // El valor inválido se informa en el formulario.
            }
            if (valores.getIdColaborador() == null) {
                model.addAttribute("mensajeError", "Selecciona un colaborador registrado.");
            }
        }
        model.addAttribute("asignacion", valores);
        return vistaFormulario(valores, null, gestor, model);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario gestor, Model model) {
        AsignacionFormulario valores = AsignacionFormulario.desde(gestion.consultarAsignacion(id, gestor));
        model.addAttribute("asignacion", valores);
        return vistaFormulario(valores, null, gestor, model);
    }

    @PostMapping("/consultar")
    public String consultar(@Validated(AsignacionFormulario.ConsultaCapacidad.class)
                            @ModelAttribute("asignacion") AsignacionFormulario formulario, BindingResult errores,
                            @RequestAttribute("usuarioVerificado") Usuario gestor, Model model) {
        return vistaFormulario(formulario, errores, gestor, model);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("asignacion") AsignacionFormulario formulario, BindingResult errores,
                           @RequestAttribute("usuarioVerificado") Usuario gestor, Model model, RedirectAttributes redirect) {
        if (!errores.hasErrors()) {
            try {
                gestion.guardar(formulario, gestor);
                redirect.addFlashAttribute("mensajeExito", "Asignación guardada correctamente.");
                return "redirect:" + ruta(gestor);
            } catch (CampoInvalido ex) {
                errores.rejectValue(ex.getCampo(), "asignacion.invalida", ex.getMessage());
            }
        }
        return vistaFormulario(formulario, errores, gestor, model);
    }

    @GetMapping("/{id}/estado")
    public String confirmarEstado(@PathVariable Integer id, @RequestParam(defaultValue = "") String estado,
                                  @RequestAttribute("usuarioVerificado") Usuario gestor, Model model,
                                  RedirectAttributes redirect) {
        try {
            EstadoAsignacion destino = EstadoAsignacion.valueOf(estado);
            Asignacion asignacion = gestion.consultarCambioEstado(id, destino, gestor);
            String titulo = switch (destino) {
                case ACTIVA -> "Activar asignación";
                case FINALIZADA -> "Finalizar asignación";
                case CANCELADA -> "Cancelar asignación";
                case PLANIFICADA -> throw new IllegalArgumentException();
            };
            if (destino == EstadoAsignacion.ACTIVA) {
                model.addAttribute("capacidadHoy", capacidad.consultarActual(asignacion.getColaborador()));
                model.addAttribute("capacidadPeriodo", gestion.consultarPeriodo(AsignacionFormulario.desde(asignacion), gestor));
            }
            model.addAttribute("asignacion", asignacion);
            model.addAttribute("titulo", titulo);
            model.addAttribute("destino", destino);
            model.addAttribute("rolVista", esResourceManager(gestor) ? "RM" : "PM");
            model.addAttribute("rutaAsignaciones", ruta(gestor));
            return "fragments/asignacion-confirmar";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("mensajeError", "Selecciona un estado de asignación válido.");
        } catch (CampoInvalido ex) {
            redirect.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:" + ruta(gestor);
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id, @RequestParam(defaultValue = "") String estado,
                                @RequestAttribute("usuarioVerificado") Usuario gestor, RedirectAttributes redirect) {
        try {
            gestion.cambiarEstado(id, EstadoAsignacion.valueOf(estado), gestor);
            redirect.addFlashAttribute("mensajeExito", "Estado de la asignación actualizado.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("mensajeError", "Selecciona un estado de asignación válido.");
        } catch (CampoInvalido ex) {
            redirect.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:" + ruta(gestor);
    }

    private String vistaFormulario(AsignacionFormulario formulario, BindingResult errores, Usuario gestor, Model model) {
        List<Colaborador> equipo = colaboradores.listarConUsuario().stream()
                .sorted(Comparator.comparing(c -> c.getUsuario().getNombreCompleto(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        Colaborador seleccionado = equipo.stream()
                .filter(c -> c.getIdColaborador().equals(formulario.getIdColaborador()))
                .findFirst().orElse(null);
        if (seleccionado != null) model.addAttribute("capacidadHoy", capacidad.consultarActual(seleccionado));

        if (puedeConsultarPeriodo(formulario, errores)) {
            try {
                model.addAttribute("capacidadPeriodo", gestion.consultarPeriodo(formulario, gestor));
            } catch (CampoInvalido ex) {
                if (errores != null) errores.rejectValue(ex.getCampo(), "asignacion.invalida", ex.getMessage());
                else model.addAttribute("mensajeError", ex.getMessage());
            }
        }
        model.addAttribute("rutaAsignaciones", ruta(gestor));
        model.addAttribute("titulo", formulario.getIdAsignacion() == null ? "Nueva asignación" : "Editar asignación");
        model.addAttribute("proyectos", gestion.proyectosDisponibles(gestor));
        model.addAttribute("colaboradores", equipo);
        return esResourceManager(gestor) ? "resource/asignacion-form" : "pm/asignacion-form";
    }

    private boolean puedeConsultarPeriodo(AsignacionFormulario formulario, BindingResult errores) {
        return formulario.getIdProyecto() != null && formulario.getIdColaborador() != null
                && formulario.getFechaInicio() != null && formulario.isPeriodoValido()
                && (errores == null || !(errores.hasFieldErrors("idAsignacion") || errores.hasFieldErrors("idProyecto")
                        || errores.hasFieldErrors("idColaborador") || errores.hasFieldErrors("fechaInicio")
                        || errores.hasFieldErrors("fechaFin") || errores.hasFieldErrors("estado")));
    }

    private String ruta(Usuario gestor) {
        return esResourceManager(gestor) ? "/resource/asignaciones" : "/pm/asignaciones";
    }

    private boolean esResourceManager(Usuario gestor) {
        return "RESOURCE_MANAGER".equals(gestor.getRol().getNombre());
    }
}
