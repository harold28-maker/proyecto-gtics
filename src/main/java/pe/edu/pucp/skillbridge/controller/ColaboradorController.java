package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/colaborador")
public class ColaboradorController {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final HabilidadRepository habilidadRepository;
    private final CertificacionRepository certificacionRepository;
    private final AsignacionRepository asignacionRepository;
    private final ForoRepository foroRepository;
    private final NotificacionRepository notificacionRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final RespuestaForoRepository respuestaForoRepository;

    public ColaboradorController(UsuarioRepository usuarioRepository,
                                 ColaboradorRepository colaboradorRepository,
                                 HabilidadRepository habilidadRepository,
                                 CertificacionRepository certificacionRepository,
                                 AsignacionRepository asignacionRepository,
                                 ForoRepository foroRepository,
                                 NotificacionRepository notificacionRepository,
                                 MensajeChatRepository mensajeChatRepository,
                                 RespuestaForoRepository respuestaForoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.habilidadRepository = habilidadRepository;
        this.certificacionRepository = certificacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.foroRepository = foroRepository;
        this.notificacionRepository = notificacionRepository;
        this.mensajeChatRepository = mensajeChatRepository;
        this.respuestaForoRepository = respuestaForoRepository;
    }

    private Colaborador colaboradorDemo() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null && session.getAttribute("usuarioSesionId") instanceof Integer idUsuario) {
                var colaboradorSesion = colaboradorRepository.findByUsuarioIdUsuario(idUsuario);
                if (colaboradorSesion.isPresent()) return colaboradorSesion.get();
            }
        }

        List<Usuario> usuarios = usuarioRepository.findByRolNombre("COLABORADOR");
        for (Usuario usuario : usuarios) {
            var colaborador = colaboradorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
            if (colaborador.isPresent()) return colaborador.get();
        }
        return colaboradorRepository.findAll().stream().findFirst().orElse(null);
    }

    private List<HabilidadColaboradorDTO> habilidades(Integer idColaborador) {
        List<HabilidadColaboradorDTO> lista = new ArrayList<>();
        for (Object[] fila : colaboradorRepository.obtenerHabilidades(idColaborador)) {
            lista.add(new HabilidadColaboradorDTO(
                    ((Number) fila[0]).intValue(),
                    String.valueOf(fila[1]),
                    String.valueOf(fila[2]),
                    String.valueOf(fila[3]),
                    fila[4] == null ? BigDecimal.ZERO : new BigDecimal(fila[4].toString())
            ));
        }
        return lista;
    }

    private boolean soloLetrasHasta20(String valor) {
        return valor != null && valor.matches("^[\\p{L} ]{1,20}$");
    }

    private String validarDatosUsuario(String nombres, String apellidos, String telefono) {
        if (!soloLetrasHasta20(nombres)) return "Los nombres solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (!soloLetrasHasta20(apellidos)) return "Los apellidos solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (telefono == null || !telefono.matches("^[0-9]{9}$")) return "El teléfono debe contener exactamente 9 dígitos.";
        return null;
    }

    @RequestMapping(value = "/inicio", method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Mi espacio de trabajo");
        model.addAttribute("colaborador", c);
        if (c != null) {
            List<Asignacion> asignaciones = asignacionRepository.findByColaboradorIdColaboradorOrderByFechaInicioDesc(c.getIdColaborador());
            List<Asignacion> activas = asignaciones.stream()
                    .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA || a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA)
                    .toList();
            int cargaActual = activas.stream().mapToInt(a -> a.getPorcentajeDedicacion() == null ? 0 : a.getPorcentajeDedicacion()).sum();
            int disponibilidadReal = Math.max((c.getDisponibilidadBase() == null ? 0 : c.getDisponibilidadBase()) - cargaActual, 0);
            Asignacion principal = activas.stream().findFirst().orElse(null);

            model.addAttribute("totalAsignaciones", asignaciones.size());
            model.addAttribute("asignacionesActivas", activas.size());
            model.addAttribute("asignacionPrincipal", principal);
            model.addAttribute("cargaActual", cargaActual);
            model.addAttribute("disponibilidadReal", disponibilidadReal);
            model.addAttribute("habilidades", habilidades(c.getIdColaborador()).size());
            model.addAttribute("totalCertificaciones", certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador()).size());
        }
        return "colaborador/inicio";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Mi perfil profesional");
        model.addAttribute("colaborador", c);
        model.addAttribute("certificaciones", c == null ? List.of()
                : certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador()));
        model.addAttribute("habilidades", c == null ? List.of() : habilidades(c.getIdColaborador()));
        return "colaborador/perfil";
    }

    @GetMapping("/perfil/editar")
    public String editarPerfil(Model model) {
        model.addAttribute("titulo", "Editar perfil profesional");
        model.addAttribute("colaborador", colaboradorDemo());
        return "colaborador/perfil-form";
    }

    @PostMapping("/perfil/guardar")
    public String guardarPerfil(@Valid @ModelAttribute("colaborador") Colaborador colaborador,
                                BindingResult bindingResult,
                                @RequestParam("nombres") String nombres,
                                @RequestParam("apellidos") String apellidos,
                                @RequestParam("telefono") String telefono,
                                Model model) {
        Colaborador actual = colaboradorRepository.findById(colaborador.getIdColaborador()).orElseThrow();
        if (bindingResult.hasErrors()) {
            colaborador.setUsuario(actual.getUsuario());
            model.addAttribute("titulo", "Editar perfil profesional");
            model.addAttribute("mensajeError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "colaborador/perfil-form";
        }
        String error = validarDatosUsuario(nombres.trim(), apellidos.trim(), telefono.trim());
        if (error != null) {
            colaborador.setUsuario(actual.getUsuario());
            model.addAttribute("titulo", "Editar perfil profesional");
            model.addAttribute("colaborador", colaborador);
            model.addAttribute("mensajeError", error);
            return "colaborador/perfil-form";
        }

        Usuario usuario = actual.getUsuario();
        usuario.setNombres(nombres.trim());
        usuario.setApellidos(apellidos.trim());
        usuario.setTelefono(telefono.trim());
        actual.setCargo(colaborador.getCargo().trim());
        actual.setArea(colaborador.getArea().trim());
        actual.setSeniority(colaborador.getSeniority());
        actual.setBiografia(colaborador.getBiografia());
        actual.setInteresesProfesionales(colaborador.getInteresesProfesionales());
        actual.setDisponibilidadBase(colaborador.getDisponibilidadBase());

        usuarioRepository.save(usuario);
        colaboradorRepository.save(actual);

        perfil(model);
        model.addAttribute("mensajeExito", "Tu perfil fue actualizado correctamente.");
        return "colaborador/perfil";
    }

    @GetMapping("/certificaciones")
    public String certificaciones(Model model) {
        Colaborador c = colaboradorDemo();
        List<Certificacion> lista = c == null ? List.of()
                : certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        return "colaborador/certificaciones";
    }

    private void cargarCertificaciones(Model model, List<Certificacion> lista) {
        model.addAttribute("titulo", "Certificaciones y credenciales");
        model.addAttribute("certificaciones", lista);
        model.addAttribute("vigentes", lista.stream().filter(cert -> cert.getEstado() == Certificacion.EstadoCertificacion.VIGENTE).count());
    }

    @GetMapping("/certificaciones/nueva")
    public String nuevaCertificacion(Model model) {
        Certificacion certificacion = new Certificacion();
        certificacion.setEstado(Certificacion.EstadoCertificacion.VIGENTE);
        model.addAttribute("titulo", "Nueva certificación");
        model.addAttribute("certificacion", certificacion);
        return "colaborador/certificacion-form";
    }

    @GetMapping("/certificaciones/editar/{id}")
    public String editarCertificacion(@PathVariable Integer id, Model model) {
        model.addAttribute("titulo", "Editar certificación");
        model.addAttribute("certificacion", certificacionRepository.findById(id).orElseThrow());
        return "colaborador/certificacion-form";
    }

    @PostMapping("/certificaciones/guardar")
    public String guardarCertificacion(@Valid @ModelAttribute("certificacion") Certificacion certificacion,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", certificacion.getIdCertificacion() == null ? "Nueva certificación" : "Editar certificación");
            model.addAttribute("mensajeError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "colaborador/certificacion-form";
        }
        String nombre = certificacion.getNombre() == null ? "" : certificacion.getNombre().trim();
        String entidad = certificacion.getEntidadEmisora() == null ? "" : certificacion.getEntidadEmisora().trim();
        Colaborador c = colaboradorDemo();
        certificacion.setNombre(nombre);
        certificacion.setEntidadEmisora(entidad);
        certificacion.setColaborador(c);
        certificacionRepository.save(certificacion);
        List<Certificacion> lista = certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        model.addAttribute("mensajeExito", "Certificación guardada correctamente.");
        return "colaborador/certificaciones";
    }

    @PostMapping("/certificaciones/{id}/eliminar")
    public String eliminarCertificacion(@PathVariable Integer id, Model model) {
        certificacionRepository.deleteById(id);
        Colaborador c = colaboradorDemo();
        List<Certificacion> lista = certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        model.addAttribute("mensajeExito", "Certificación eliminada del perfil.");
        return "colaborador/certificaciones";
    }

    @GetMapping("/habilidades")
    public String habilidades(@RequestParam(value = "editar", required = false) Integer idEditar, Model model) {
        cargarHabilidades(model, idEditar);
        return "colaborador/habilidades";
    }

    private void cargarHabilidades(Model model, Integer idEditar) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Mapa de habilidades");
        model.addAttribute("colaborador", c);
        List<HabilidadColaboradorDTO> actuales = c == null ? List.of() : habilidades(c.getIdColaborador());
        model.addAttribute("habilidades", actuales);
        List<Integer> idsActuales = actuales.stream().map(HabilidadColaboradorDTO::getIdHabilidad).toList();
        model.addAttribute("habilidadesDisponibles", habilidadRepository.findByEstadoTrueOrderByNombreAsc().stream()
                .filter(h -> !idsActuales.contains(h.getIdHabilidad())).toList());
        model.addAttribute("habilidadEditar", idEditar == null ? null : actuales.stream()
                .filter(h -> h.getIdHabilidad().equals(idEditar)).findFirst().orElse(null));
    }

    private String validarExperienciaYNivel(String nivel, BigDecimal aniosExperiencia) {
        if (aniosExperiencia == null || aniosExperiencia.compareTo(BigDecimal.ZERO) < 0 || aniosExperiencia.compareTo(new BigDecimal("50")) > 0) {
            return "Los años de experiencia deben estar entre 0 y 50.";
        }
        if (!List.of("BASICO", "INTERMEDIO", "AVANZADO", "EXPERTO").contains(nivel)) {
            return "Selecciona un nivel válido.";
        }
        return null;
    }

    @PostMapping("/habilidades/guardar")
    public String guardarHabilidad(@RequestParam("idHabilidad") Integer idHabilidad,
                                   @RequestParam("nivel") String nivel,
                                   @RequestParam("aniosExperiencia") BigDecimal aniosExperiencia,
                                   Model model) {
        Colaborador c = colaboradorDemo();
        if (c == null) {
            cargarHabilidades(model, null);
            return "colaborador/habilidades";
        }
        String error = validarExperienciaYNivel(nivel, aniosExperiencia);
        if (error != null) {
            cargarHabilidades(model, null);
            model.addAttribute("mensajeError", error);
            return "colaborador/habilidades";
        }
        boolean yaExiste = habilidades(c.getIdColaborador()).stream().anyMatch(h -> h.getIdHabilidad().equals(idHabilidad));
        if (yaExiste) {
            cargarHabilidades(model, null);
            model.addAttribute("mensajeError", "La habilidad seleccionada ya forma parte de tu perfil. Usa Editar para modificarla.");
            return "colaborador/habilidades";
        }
        colaboradorRepository.guardarHabilidad(c.getIdColaborador(), idHabilidad, nivel, aniosExperiencia);
        cargarHabilidades(model, null);
        model.addAttribute("mensajeExito", "Habilidad agregada correctamente.");
        return "colaborador/habilidades";
    }

    @PostMapping("/habilidades/actualizar")
    public String actualizarHabilidad(@RequestParam("idHabilidad") Integer idHabilidad,
                                      @RequestParam("nivel") String nivel,
                                      @RequestParam("aniosExperiencia") BigDecimal aniosExperiencia,
                                      Model model) {
        Colaborador c = colaboradorDemo();
        String error = validarExperienciaYNivel(nivel, aniosExperiencia);
        if (c == null || error != null) {
            cargarHabilidades(model, idHabilidad);
            if (error != null) model.addAttribute("mensajeError", error);
            return "colaborador/habilidades";
        }
        boolean existe = habilidades(c.getIdColaborador()).stream().anyMatch(h -> h.getIdHabilidad().equals(idHabilidad));
        if (!existe) {
            cargarHabilidades(model, null);
            model.addAttribute("mensajeError", "La habilidad que deseas editar ya no está asociada a tu perfil.");
            return "colaborador/habilidades";
        }
        colaboradorRepository.guardarHabilidad(c.getIdColaborador(), idHabilidad, nivel, aniosExperiencia);
        cargarHabilidades(model, null);
        model.addAttribute("mensajeExito", "Habilidad actualizada correctamente.");
        return "colaborador/habilidades";
    }

    @PostMapping("/habilidades/{idHabilidad}/eliminar")
    public String eliminarHabilidad(@PathVariable Integer idHabilidad, Model model) {
        Colaborador c = colaboradorDemo();
        if (c != null) colaboradorRepository.eliminarHabilidad(c.getIdColaborador(), idHabilidad);
        cargarHabilidades(model, null);
        model.addAttribute("mensajeExito", "Habilidad retirada del perfil.");
        return "colaborador/habilidades";
    }

    @GetMapping("/proyectos")
    public String proyectos(Model model) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Mis proyectos");
        model.addAttribute("asignaciones", c == null ? List.of()
                : asignacionRepository.findByColaboradorIdColaboradorOrderByFechaInicioDesc(c.getIdColaborador()));
        return "colaborador/proyectos";
    }

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "estado", required = false, defaultValue = "TODOS") String estado,
                               Model model) {
        Colaborador c = colaboradorDemo();
        List<Asignacion> lista = c == null ? List.of()
                : asignacionRepository.findByColaboradorIdColaboradorOrderByFechaInicioDesc(c.getIdColaborador());
        lista = lista.stream().filter(a -> {
            boolean texto = q == null || q.isBlank()
                    || a.getProyecto().getNombre().toLowerCase().contains(q.toLowerCase())
                    || (a.getRolProyecto() != null && a.getRolProyecto().toLowerCase().contains(q.toLowerCase()));
            boolean coincideEstado = estado == null || estado.isBlank() || "TODOS".equals(estado)
                    || a.getEstado().name().equalsIgnoreCase(estado);
            return texto && coincideEstado;
        }).toList();
        model.addAttribute("titulo", "Mis asignaciones");
        model.addAttribute("asignaciones", lista);
        model.addAttribute("q", q);
        model.addAttribute("estadoSeleccionado", estado);
        return "colaborador/asignaciones";
    }

    @GetMapping("/foros")
    public String foros(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "categoria", required = false) String categoria,
                        @RequestParam(value = "estado", required = false) String estado,
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        Model model) {
        List<Foro> filtrados = foroRepository.findAllByOrderByFechaCreacionDesc().stream().filter(f -> {
            boolean texto = q == null || q.isBlank()
                    || f.getTitulo().toLowerCase().contains(q.toLowerCase())
                    || f.getContenido().toLowerCase().contains(q.toLowerCase());
            boolean cat = categoria == null || categoria.isBlank() || "TODAS".equals(categoria)
                    || categoria.equalsIgnoreCase(f.getCategoria());
            boolean est = estado == null || estado.isBlank() || "TODOS".equals(estado)
                    || estado.equalsIgnoreCase(f.getEstado().name());
            return texto && cat && est;
        }).toList();

        int porPagina = 5;
        int totalPaginas = Math.max(1, (int) Math.ceil(filtrados.size() / (double) porPagina));
        int paginaActual = Math.max(1, Math.min(page, totalPaginas));
        int desde = (paginaActual - 1) * porPagina;
        int hasta = Math.min(desde + porPagina, filtrados.size());
        List<Foro> lista = filtrados.isEmpty() ? List.of() : filtrados.subList(desde, hasta);
        List<Foro> todos = foroRepository.findAllByOrderByFechaCreacionDesc();

        model.addAttribute("titulo", "Foro de conocimiento");
        model.addAttribute("foros", lista);
        model.addAttribute("q", q);
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("categoriasForo", ForoController.CATEGORIAS_FORO);
        model.addAttribute("totalForos", todos.size());
        model.addAttribute("abiertos", todos.stream().filter(f -> f.getEstado() == Foro.EstadoForo.ABIERTO).count());
        model.addAttribute("resueltos", todos.stream().filter(f -> f.getEstado() == Foro.EstadoForo.RESUELTO).count());
        model.addAttribute("conteoRespuestas", lista.stream().collect(Collectors.toMap(
                Foro::getIdForo, f -> respuestaForoRepository.countByForoIdForo(f.getIdForo()))));
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        return "colaborador/foros";
    }

    @GetMapping("/notificaciones")
    public String notificaciones(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
        Colaborador c = colaboradorDemo();
        List<Notificacion> lista = c == null ? List.of()
                : notificacionRepository.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(c.getUsuario().getIdUsuario());
        cargarNotificaciones(model, lista, page);
        return "colaborador/notificaciones";
    }

    private void cargarNotificaciones(Model model, List<Notificacion> lista, int page) {
        int porPagina = 5;
        int totalPaginas = Math.max(1, (int) Math.ceil(lista.size() / (double) porPagina));
        int paginaActual = Math.max(1, Math.min(page, totalPaginas));
        int desde = (paginaActual - 1) * porPagina;
        int hasta = Math.min(desde + porPagina, lista.size());
        List<Notificacion> pagina = lista.isEmpty() ? List.of() : lista.subList(desde, hasta);
        long noLeidas = lista.stream().filter(n -> !Boolean.TRUE.equals(n.getLeida())).count();

        model.addAttribute("titulo", "Centro de notificaciones");
        model.addAttribute("notificaciones", pagina);
        model.addAttribute("totalNotificaciones", lista.size());
        model.addAttribute("noLeidas", noLeidas);
        model.addAttribute("leidas", lista.size() - noLeidas);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
    }

    @PostMapping("/notificaciones/{id}/leer")
    public String marcarLeida(@PathVariable Integer id,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              Model model) {
        Notificacion n = notificacionRepository.findById(id).orElseThrow();
        n.setLeida(true);
        notificacionRepository.save(n);
        Colaborador c = colaboradorDemo();
        List<Notificacion> lista = c == null ? List.of()
                : notificacionRepository.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(c.getUsuario().getIdUsuario());
        cargarNotificaciones(model, lista, page);
        return "colaborador/notificaciones";
    }

    @PostMapping("/notificaciones/leer-todas")
    public String marcarTodasLeidas(Model model) {
        Colaborador c = colaboradorDemo();
        List<Notificacion> lista = c == null ? List.of()
                : notificacionRepository.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(c.getUsuario().getIdUsuario());
        lista.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(lista);
        cargarNotificaciones(model, lista, 1);
        return "colaborador/notificaciones";
    }

    private List<Asignacion> asignacionesChat(Colaborador colaborador) {
        if (colaborador == null) return List.of();
        return asignacionRepository
                .findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador())
                .stream()
                .filter(a -> a.getProyecto() != null
                        && a.getEstado() != Asignacion.EstadoAsignacion.CANCELADA)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                a -> a.getProyecto().getIdProyecto(),
                                a -> a,
                                (primera, repetida) -> primera,
                                java.util.LinkedHashMap::new
                        ),
                        mapa -> new ArrayList<>(mapa.values())
                ));
    }

    private Proyecto proyectoChatPermitido(List<Asignacion> asignaciones, Integer idProyecto) {
        if (asignaciones.isEmpty()) return null;
        if (idProyecto == null) return asignaciones.get(0).getProyecto();
        return asignaciones.stream()
                .map(Asignacion::getProyecto)
                .filter(p -> p != null && p.getIdProyecto().equals(idProyecto))
                .findFirst()
                .orElse(null);
    }

    private void cargarChat(Model model, Colaborador colaborador, Integer idProyecto) {
        List<Asignacion> asignaciones = asignacionesChat(colaborador);
        Proyecto proyectoSeleccionado = proyectoChatPermitido(asignaciones, idProyecto);

        model.addAttribute("titulo", "Chat de proyectos");
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("asignaciones", asignaciones);
        model.addAttribute("proyectoSeleccionado", proyectoSeleccionado);
        model.addAttribute("mensajes", proyectoSeleccionado == null ? List.of()
                : mensajeChatRepository.findByProyectoIdProyectoOrderByFechaEnvioAsc(
                        proyectoSeleccionado.getIdProyecto()));
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "idProyecto", required = false) Integer idProyecto,
                       Model model) {
        cargarChat(model, colaboradorDemo(), idProyecto);
        return "colaborador/chat";
    }

    @PostMapping("/chat/enviar")
    public String enviarMensaje(@RequestParam("idProyecto") Integer idProyecto,
                                @RequestParam("contenido") String contenido,
                                Model model) {
        Colaborador colaborador = colaboradorDemo();
        List<Asignacion> asignaciones = asignacionesChat(colaborador);
        Proyecto proyecto = proyectoChatPermitido(asignaciones, idProyecto);
        String mensajeTexto = contenido == null ? "" : contenido.trim();

        if (colaborador == null) {
            cargarChat(model, null, idProyecto);
            model.addAttribute("mensajeError", "No se encontró un perfil de colaborador asociado a la sesión.");
            return "colaborador/chat";
        }

        if (proyecto == null) {
            cargarChat(model, colaborador, null);
            model.addAttribute("mensajeError", "No puedes enviar mensajes a un proyecto en el que no participas.");
            return "colaborador/chat";
        }

        if (mensajeTexto.isBlank()) {
            cargarChat(model, colaborador, idProyecto);
            model.addAttribute("mensajeError", "Escribe un mensaje antes de enviarlo.");
            return "colaborador/chat";
        }

        if (mensajeTexto.length() > 500) {
            cargarChat(model, colaborador, idProyecto);
            model.addAttribute("mensajeError", "El mensaje puede tener como máximo 500 caracteres.");
            return "colaborador/chat";
        }

        MensajeChat mensaje = new MensajeChat();
        mensaje.setProyecto(proyecto);
        mensaje.setUsuario(colaborador.getUsuario());
        mensaje.setContenido(mensajeTexto);
        mensajeChatRepository.saveAndFlush(mensaje);

        cargarChat(model, colaborador, idProyecto);
        model.addAttribute("mensajeExito", "Mensaje enviado correctamente.");
        return "colaborador/chat";
    }
}
