package pe.edu.pucp.skillbridge.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.entity.MensajeChat;
import pe.edu.pucp.skillbridge.entity.Notificacion;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.MensajeChatRepository;
import pe.edu.pucp.skillbridge.repository.NotificacionRepository;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService.Acceso;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/colaborador")
public class ComunicacionColaboradorController {
    private static final int POR_PAGINA = 5;

    private final AccesoProyectosService accesoProyectos;
    private final MensajeChatRepository mensajes;
    private final NotificacionRepository notificaciones;

    public ComunicacionColaboradorController(AccesoProyectosService accesoProyectos, MensajeChatRepository mensajes,
                                              NotificacionRepository notificaciones) {
        this.accesoProyectos = accesoProyectos;
        this.mensajes = mensajes;
        this.notificaciones = notificaciones;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "idProyecto", defaultValue = "") String proyecto,
                       @RequestAttribute("usuarioVerificado") Usuario usuario,
                       @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Map<Integer, Acceso> proyectos = accesoProyectos.consultar(usuario, bloqueados);
        Acceso actual = proyecto.isBlank()
                ? proyectos.values().stream().filter(Acceso::puedeParticipar).findFirst()
                    .orElseGet(() -> proyectos.values().stream().findFirst().orElse(null))
                : proyectos.get(enteroPositivo(proyecto));
        if (!proyecto.isBlank() && actual == null) {
            model.addAttribute("mensajeError", "Selecciona una conversación disponible.");
        }
        return cargarChat(model, proyectos, actual, usuario);
    }

    @PostMapping("/chat/enviar")
    public String enviar(@RequestParam(value = "idProyecto", defaultValue = "") String proyecto,
                         @RequestParam(value = "contenido", defaultValue = "") String contenido,
                         @RequestAttribute("usuarioVerificado") Usuario usuario,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                         Model model, RedirectAttributes redirect) {
        Map<Integer, Acceso> proyectos = accesoProyectos.consultar(usuario, bloqueados);
        Acceso actual = proyectos.get(enteroPositivo(proyecto));
        if (actual == null || !actual.puedeParticipar()) {
            model.addAttribute("mensajeError", "Solo puedes escribir en un proyecto donde tienes una asignación vigente.");
            return cargarChat(model, proyectos, actual, usuario);
        }

        String texto = contenido.strip();
        if (texto.isBlank() || texto.length() > 500) {
            model.addAttribute("errorMensaje", texto.isBlank() ? "Escribe un mensaje."
                    : "El mensaje admite hasta 500 caracteres.");
            model.addAttribute("borrador", contenido);
            return cargarChat(model, proyectos, actual, usuario);
        }
        MensajeChat mensaje = new MensajeChat();
        mensaje.setProyecto(actual.proyecto());
        mensaje.setUsuario(usuario);
        mensaje.setContenido(texto);
        mensajes.save(mensaje);
        redirect.addAttribute("idProyecto", actual.proyecto().getIdProyecto());
        redirect.addFlashAttribute("mensajeExito", "Mensaje enviado.");
        return "redirect:/colaborador/chat";
    }

    private String cargarChat(Model model, Map<Integer, Acceso> proyectos, Acceso actual, Usuario usuario) {
        Map<LocalDate, List<MensajeChat>> porFecha = new LinkedHashMap<>();
        if (actual != null) {
            for (MensajeChat mensaje : mensajes.findByProyectoIdProyectoOrderByFechaEnvioAscIdMensajeAsc(
                    actual.proyecto().getIdProyecto())) {
                LocalDate fecha = mensaje.getFechaEnvio() == null ? null : mensaje.getFechaEnvio().toLocalDate();
                porFecha.computeIfAbsent(fecha, dia -> new ArrayList<>()).add(mensaje);
            }
        }
        List<DiaChat> dias = porFecha.entrySet().stream()
                .map(dia -> new DiaChat(dia.getKey(), dia.getValue())).toList();
        model.addAttribute("titulo", "Chat");
        model.addAttribute("proyectosChat", proyectos.values().stream()
                .sorted(Comparator.comparing(Acceso::puedeParticipar).reversed()).toList());
        model.addAttribute("conversacion", actual);
        model.addAttribute("diasChat", dias);
        model.addAttribute("idUsuarioChat", usuario.getIdUsuario());
        return "colaborador/chat";
    }

    public record DiaChat(LocalDate fecha, List<MensajeChat> mensajes) {}

    @GetMapping("/notificaciones")
    public String notificaciones(@RequestParam(value = "filtro", defaultValue = "TODAS") String filtro,
                                 @RequestParam(value = "page", defaultValue = "1") String page,
                                 @RequestAttribute("usuarioVerificado") Usuario usuario, Model model) {
        Integer numero = enteroPositivo(page);
        if (numero == null || !filtroValido(filtro)) {
            model.addAttribute("mensajeError", "Selecciona un filtro y un número de página válidos.");
        }
        boolean soloNoLeidas = "NO_LEIDAS".equals(filtro);
        int idUsuario = usuario.getIdUsuario();
        long noLeidas = notificaciones.contarPendientes(idUsuario);
        Page<Notificacion> pagina = consultarNotificaciones(idUsuario, soloNoLeidas, 0);
        int ultima = Math.max(1, pagina.getTotalPages());
        int actual = numero == null ? 1 : Math.min(numero, ultima);
        long total = soloNoLeidas
                ? notificaciones.countByUsuarioIdUsuario(idUsuario) : pagina.getTotalElements();
        if (actual > 1) pagina = consultarNotificaciones(idUsuario, soloNoLeidas, actual - 1);

        model.addAttribute("titulo", "Notificaciones");
        model.addAttribute("notificaciones", pagina.getContent());
        model.addAttribute("totalNotificaciones", total);
        model.addAttribute("noLeidas", noLeidas);
        model.addAttribute("leidas", Math.max(0, total - noLeidas));
        model.addAttribute("paginaActual", actual);
        model.addAttribute("totalPaginas", ultima);
        model.addAttribute("filtro", soloNoLeidas ? "NO_LEIDAS" : "TODAS");
        model.addAttribute("totalFiltradas", pagina.getTotalElements());
        model.addAttribute("desde", (actual - 1L) * POR_PAGINA + 1);
        model.addAttribute("hasta", (actual - 1L) * POR_PAGINA + pagina.getNumberOfElements());
        return "colaborador/notificaciones";
    }

    private Page<Notificacion> consultarNotificaciones(int usuario, boolean soloNoLeidas, int numero) {
        PageRequest pagina = PageRequest.of(numero, POR_PAGINA);
        return soloNoLeidas
                ? notificaciones.buscarPendientes(usuario, pagina)
                : notificaciones.findByUsuarioIdUsuarioOrderByFechaCreacionDescIdNotificacionDesc(usuario, pagina);
    }

    @PostMapping("/notificaciones/{id}/leer")
    public String marcarLeida(@PathVariable String id,
                              @RequestParam(value = "page", defaultValue = "1") String page,
                              @RequestParam(value = "filtro", defaultValue = "TODAS") String filtro,
                              @RequestAttribute("usuarioVerificado") Usuario usuario, RedirectAttributes redirect) {
        Integer idNotificacion = enteroPositivo(id);
        Integer numero = enteroPositivo(page);
        if (!filtroValido(filtro) || numero == null || idNotificacion == null) {
            redirect.addFlashAttribute("mensajeError", "La solicitud de lectura no es válida.");
            return "redirect:/colaborador/notificaciones";
        }
        var notificacion = notificaciones.findByIdNotificacionAndUsuarioIdUsuario(idNotificacion, usuario.getIdUsuario());
        if (notificacion.isPresent()) {
            Notificacion n = notificacion.get();
            if (!Boolean.TRUE.equals(n.getLeida())) {
                n.setLeida(true);
                notificaciones.save(n);
            }
            redirect.addFlashAttribute("mensajeExito", "Notificación marcada como leída.");
        } else {
            redirect.addFlashAttribute("mensajeError", "La notificación no está disponible.");
        }
        redirect.addAttribute("page", numero);
        redirect.addAttribute("filtro", filtro);
        return "redirect:/colaborador/notificaciones";
    }

    @PostMapping("/notificaciones/leer-todas")
    public String marcarTodas(@RequestParam(value = "filtro", defaultValue = "TODAS") String filtro,
                              @RequestAttribute("usuarioVerificado") Usuario usuario, RedirectAttributes redirect) {
        if (!filtroValido(filtro)) {
            redirect.addFlashAttribute("mensajeError", "Selecciona un filtro válido.");
        } else {
            notificaciones.marcarTodasLeidas(usuario.getIdUsuario());
            redirect.addAttribute("filtro", filtro);
            redirect.addFlashAttribute("mensajeExito", "Todas tus notificaciones están leídas.");
        }
        return "redirect:/colaborador/notificaciones";
    }

    private boolean filtroValido(String filtro) {
        return "TODAS".equals(filtro) || "NO_LEIDAS".equals(filtro);
    }

    private Integer enteroPositivo(String valor) {
        try {
            int numero = Integer.parseInt(valor);
            return numero > 0 ? numero : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
