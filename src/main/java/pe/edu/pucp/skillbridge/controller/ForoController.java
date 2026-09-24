package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/foros")
public class ForoController {

    public static final List<String> CATEGORIAS_FORO = List.of(
            "General",
            "Backend",
            "Frontend",
            "API e Integraciones",
            "Base de Datos",
            "DevOps / Cloud",
            "Testing / QA",
            "Arquitectura",
            "Gestión de Proyecto"
    );

    private final ForoRepository foroRepository;
    private final RespuestaForoRepository respuestaForoRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public ForoController(ForoRepository foroRepository,
                          RespuestaForoRepository respuestaForoRepository,
                          ProyectoRepository proyectoRepository,
                          UsuarioRepository usuarioRepository) {
        this.foroRepository = foroRepository;
        this.respuestaForoRepository = respuestaForoRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario autorDemo(String vista) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null && session.getAttribute("usuarioSesionId") instanceof Integer idUsuario) {
                Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
                if (usuario != null) return usuario;
            }
        }

        String rol = "PM".equals(vista) ? "PROJECT_MANAGER" : "COLABORADOR";
        return usuarioRepository.findByRolNombre(rol).stream().findFirst()
                .orElseGet(() -> usuarioRepository.findAll().stream().findFirst().orElse(null));
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Integer id,
                          @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                          Model model) {
        cargarDetalle(id, vista, model);
        return "foro/detalle";
    }

    private void cargarDetalle(Integer id, String vista, Model model) {
        Foro foro = foroRepository.findById(id).orElseThrow();
        List<RespuestaForo> respuestas = respuestaForoRepository.findByForoIdForoOrderByFechaCreacionAsc(id);
        model.addAttribute("titulo", "Conversación de conocimiento");
        model.addAttribute("foro", foro);
        model.addAttribute("respuestas", respuestas);
        model.addAttribute("totalRespuestas", respuestas.size());
        model.addAttribute("soluciones", respuestas.stream().filter(r -> Boolean.TRUE.equals(r.getEsSolucion())).count());
        model.addAttribute("rolVista", "PM".equals(vista) ? "PM" : "COL");
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                        Model model) {
        Foro foro = new Foro();
        foro.setEstado(Foro.EstadoForo.ABIERTO);
        foro.setCategoria("General");
        prepararFormulario(foro, vista, "Crear nueva publicación", model);
        return "foro/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id,
                         @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                         Model model) {
        prepararFormulario(foroRepository.findById(id).orElseThrow(), vista, "Editar publicación", model);
        return "foro/form";
    }

    private void prepararFormulario(Foro foro, String vista, String titulo, Model model) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("foro", foro);
        model.addAttribute("proyectos", proyectoRepository.findAll());
        model.addAttribute("categoriasForo", CATEGORIAS_FORO);
        model.addAttribute("rolVista", "PM".equals(vista) ? "PM" : "COL");
    }

    @PostMapping("/guardar")
    public String guardar(Foro foro,
                          @RequestParam(value = "idProyecto", required = false) Integer idProyecto,
                          @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                          Model model) {
        if (foro.getIdForo() != null) {
            Foro actual = foroRepository.findById(foro.getIdForo()).orElseThrow();
            foro.setAutor(actual.getAutor());
        } else {
            foro.setAutor(autorDemo(vista));
        }

        if (foro.getEstado() == null) foro.setEstado(Foro.EstadoForo.ABIERTO);
        if (foro.getCategoria() == null || foro.getCategoria().isBlank()) foro.setCategoria("General");
        foro.setProyecto(idProyecto == null ? null : proyectoRepository.findById(idProyecto).orElse(null));

        Foro guardado = foroRepository.save(foro);
        cargarDetalle(guardado.getIdForo(), vista, model);
        model.addAttribute("mensajeExito", "Publicación guardada en el Foro de conocimiento.");
        return "foro/detalle";
    }

    @PostMapping("/{id}/responder")
    public String responder(@PathVariable("id") Integer id,
                            @RequestParam("contenido") String contenido,
                            @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                            Model model) {
        if (contenido != null && !contenido.isBlank()) {
            RespuestaForo respuesta = new RespuestaForo();
            respuesta.setForo(foroRepository.findById(id).orElseThrow());
            respuesta.setAutor(autorDemo(vista));
            respuesta.setContenido(contenido.trim());
            respuesta.setEsSolucion(false);
            respuestaForoRepository.save(respuesta);
        }
        cargarDetalle(id, vista, model);
        model.addAttribute("mensajeExito", "Respuesta publicada correctamente.");
        return "foro/detalle";
    }

    @PostMapping("/{idForo}/solucion/{idRespuesta}")
    public String marcarSolucion(@PathVariable Integer idForo,
                                 @PathVariable Integer idRespuesta,
                                 @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                                 Model model) {
        RespuestaForo respuesta = respuestaForoRepository.findById(idRespuesta).orElseThrow();
        respuesta.setEsSolucion(true);
        respuestaForoRepository.save(respuesta);

        Foro foro = foroRepository.findById(idForo).orElseThrow();
        foro.setEstado(Foro.EstadoForo.RESUELTO);
        foroRepository.save(foro);

        cargarDetalle(idForo, vista, model);
        model.addAttribute("mensajeExito", "Respuesta marcada como solución y tema resuelto.");
        return "foro/detalle";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id,
                                @RequestParam("estado") String estado,
                                @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                                Model model) {
        Foro foro = foroRepository.findById(id).orElseThrow();
        foro.setEstado(Foro.EstadoForo.valueOf(estado));
        foroRepository.save(foro);
        cargarDetalle(id, vista, model);
        model.addAttribute("mensajeExito", "Estado de la publicación actualizado.");
        return "foro/detalle";
    }

    @PostMapping("/{idForo}/respuestas/{idRespuesta}/eliminar")
    public String eliminarRespuesta(@PathVariable Integer idForo,
                                    @PathVariable Integer idRespuesta,
                                    @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                                    Model model) {
        respuestaForoRepository.deleteById(idRespuesta);
        cargarDetalle(idForo, vista, model);
        model.addAttribute("mensajeExito", "Respuesta eliminada.");
        return "foro/detalle";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarForo(@PathVariable Integer id,
                               @RequestParam(value = "vista", required = false, defaultValue = "COL") String vista,
                               Model model) {
        foroRepository.deleteById(id);
        return cargarListado(vista, model, "Publicación eliminada del Foro de conocimiento.");
    }

    private String cargarListado(String vista, Model model, String mensaje) {
        List<Foro> todos = foroRepository.findAllByOrderByFechaCreacionDesc();
        boolean esPM = "PM".equals(vista);
        int porPagina = 5;
        int totalPaginas = Math.max(1, (int) Math.ceil(todos.size() / (double) porPagina));
        List<Foro> visibles = esPM || todos.size() <= porPagina ? todos : todos.subList(0, porPagina);

        model.addAttribute("titulo", esPM ? "Foro del proyecto" : "Foro de conocimiento");
        model.addAttribute("foros", visibles);
        model.addAttribute("categoriasForo", CATEGORIAS_FORO);
        model.addAttribute("totalForos", todos.size());
        model.addAttribute("abiertos", todos.stream().filter(f -> f.getEstado() == Foro.EstadoForo.ABIERTO).count());
        model.addAttribute("resueltos", todos.stream().filter(f -> f.getEstado() == Foro.EstadoForo.RESUELTO).count());
        model.addAttribute("conteoRespuestas", visibles.stream().collect(Collectors.toMap(
                Foro::getIdForo, f -> respuestaForoRepository.countByForoIdForo(f.getIdForo()))));
        model.addAttribute("paginaActual", 1);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("mensajeExito", mensaje);
        return esPM ? "pm/foros" : "colaborador/foros";
    }
}
