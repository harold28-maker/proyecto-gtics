package pe.edu.pucp.skillbridge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.MensajeChat;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.MensajeChatRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;
import pe.edu.pucp.skillbridge.service.PermisosRolService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat humano por proyecto para los cuatro roles.
 * No se debe confundir con el chatbot de Gemini.
 * La autorización comprueba que el usuario puede ver el proyecto antes de cada lectura/escritura.
 */
@Controller
@RequestMapping("/mensajes")
public class ChatProyectoController {

    private final UsuarioRepository usuarios;
    private final ProyectoRepository proyectos;
    private final ColaboradorRepository colaboradores;
    private final AsignacionRepository asignaciones;
    private final MensajeChatRepository mensajes;
    private final PermisosRolService permisos;

    public ChatProyectoController(UsuarioRepository usuarios, ProyectoRepository proyectos,
                                  ColaboradorRepository colaboradores, AsignacionRepository asignaciones,
                                  MensajeChatRepository mensajes, PermisosRolService permisos) {
        this.usuarios = usuarios;
        this.proyectos = proyectos;
        this.colaboradores = colaboradores;
        this.asignaciones = asignaciones;
        this.mensajes = mensajes;
        this.permisos = permisos;
    }

    private Usuario usuarioActual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarios.findByCorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no reconocido"));
        if (usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO || usuario.getRol() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta inactiva o sin rol");
        }
        // Si la administración desactivó el permiso Chat para Colaborador, no permitir
        // reingresar por la ruta transversal /mensajes.
        if ("COLABORADOR".equals(usuario.getRol().getNombre())
                && permisos.deshabilitados(usuario.getRol().getIdRol()).contains("chat")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chat deshabilitado para tu rol");
        }
        return usuario;
    }

    /** Solo proyectos autorizados a ese usuario. Nunca aceptar cualquier id sin verificarlo. */
    public List<Proyecto> proyectosVisibles(Usuario usuario) {
        String rol = usuario.getRol().getNombre();
        if ("ADMINISTRADOR".equals(rol) || "RESOURCE_MANAGER".equals(rol)) {
            return proyectos.findAll().stream()
                    .sorted(Comparator.comparing(Proyecto::getIdProyecto)).toList();
        }
        if ("PROJECT_MANAGER".equals(rol)) {
            return proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario());
        }
        if ("COLABORADOR".equals(rol)) {
            Colaborador colaborador = colaboradores.findByUsuarioIdUsuario(usuario.getIdUsuario()).orElse(null);
            if (colaborador == null) return List.of();
            Map<Integer, Proyecto> unico = new LinkedHashMap<>();
            for (Asignacion asignacion : asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador())) {
                Proyecto proyecto = asignacion.getProyecto();
                if (proyecto != null) unico.put(proyecto.getIdProyecto(), proyecto);
            }
            return List.copyOf(unico.values());
        }
        return List.of();
    }

    private Proyecto proyectoAutorizado(Usuario usuario, Integer idProyecto) {
        return proyectosVisibles(usuario).stream()
                .filter(proyecto -> proyecto.getIdProyecto().equals(idProyecto))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No tienes acceso a los mensajes de este proyecto"));
    }

    @GetMapping
    public String elegirProyecto(Model model, Authentication auth) {
        Usuario usuario = usuarioActual(auth);
        model.addAttribute("titulo", "Chat por proyecto");
        model.addAttribute("proyectos", proyectosVisibles(usuario));
        model.addAttribute("usuarioActual", usuario);
        return "mensajes/proyectos";
    }

    @GetMapping("/proyecto/{idProyecto}")
    public String mostrar(@PathVariable Integer idProyecto, Model model, Authentication auth) {
        Usuario usuario = usuarioActual(auth);
        Proyecto proyecto = proyectoAutorizado(usuario, idProyecto);
        List<MensajeChat> lista = mensajes.findByProyectoIdProyectoOrderByFechaEnvioAscIdMensajeAsc(idProyecto);
        if (lista.size() > 100) lista = lista.subList(lista.size() - 100, lista.size());
        model.addAttribute("titulo", "Chat por proyecto");
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("mensajes", lista);
        model.addAttribute("usuarioActual", usuario);
        return "mensajes/conversacion";
    }

    @PostMapping("/proyecto/{idProyecto}")
    public String enviar(@PathVariable Integer idProyecto,
                         @RequestParam("contenido") String contenido,
                         Authentication auth, RedirectAttributes redirect) {
        Usuario usuario = usuarioActual(auth);
        Proyecto proyecto = proyectoAutorizado(usuario, idProyecto);
        // Validación de negocio en servidor, no confiar solo en maxlength del navegador.
        String texto = contenido == null ? "" : contenido.trim();
        if (texto.isEmpty() || texto.length() > 500) {
            redirect.addFlashAttribute("error", "El mensaje debe tener entre 1 y 500 caracteres.");
            return "redirect:/mensajes/proyecto/" + idProyecto;
        }
        MensajeChat mensaje = new MensajeChat();
        mensaje.setProyecto(proyecto);
        mensaje.setUsuario(usuario);
        mensaje.setContenido(texto);
        mensajes.save(mensaje);
        redirect.addFlashAttribute("exito", "Mensaje enviado.");
        return "redirect:/mensajes/proyecto/" + idProyecto;
    }
}
