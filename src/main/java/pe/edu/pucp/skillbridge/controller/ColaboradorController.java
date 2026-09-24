package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private String validarPerfil(String nombres, String apellidos, String telefono, Colaborador colaborador) {
        if (!soloLetrasHasta20(nombres)) return "Los nombres solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (!soloLetrasHasta20(apellidos)) return "Los apellidos solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (telefono == null || !telefono.matches("^[0-9]{9}$")) return "El teléfono debe contener exactamente 9 dígitos.";
        if (!soloLetrasHasta20(colaborador.getCargo())) return "El cargo solo puede contener letras y espacios, con un máximo de 20 caracteres.";
        if (!soloLetrasHasta20(colaborador.getArea())) return "El área solo puede contener letras y espacios, con un máximo de 20 caracteres.";
        if (colaborador.getDisponibilidadBase() == null || colaborador.getDisponibilidadBase() < 0 || colaborador.getDisponibilidadBase() > 100) {
            return "La disponibilidad debe estar entre 0 y 100%.";
        }
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
    public String guardarPerfil(Colaborador colaborador,
                                @RequestParam("nombres") String nombres,
                                @RequestParam("apellidos") String apellidos,
                                @RequestParam("telefono") String telefono,
                                Model model) {
        Colaborador actual = colaboradorRepository.findById(colaborador.getIdColaborador()).orElseThrow();
        String error = validarPerfil(nombres.trim(), apellidos.trim(), telefono.trim(), colaborador);
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
        usuarioRepository.save(usuario);

        actual.setCargo(colaborador.getCargo().trim());
        actual.setArea(colaborador.getArea().trim());
        actual.setSeniority(colaborador.getSeniority());
        actual.setBiografia(colaborador.getBiografia());
        actual.setInteresesProfesionales(colaborador.getInteresesProfesionales());
        actual.setDisponibilidadBase(colaborador.getDisponibilidadBase());
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

        Map<Integer, String> archivosCertificacion = new HashMap<>();
        for (Certificacion certificacion : lista) {
            Path ruta = buscarArchivoCertificacion(certificacion.getIdCertificacion());
            if (ruta != null) {
                archivosCertificacion.put(
                        certificacion.getIdCertificacion(),
                        obtenerNombreVisibleArchivo(ruta)
                );
            }
        }

        cargarCertificaciones(model, lista);
        model.addAttribute("archivosCertificacion", archivosCertificacion);
        return "colaborador/certificaciones";
    }

    private void cargarCertificaciones(Model model, List<Certificacion> lista) {
        model.addAttribute("titulo", "Certificaciones");
        model.addAttribute("certificaciones", lista);
        model.addAttribute("vigentes", lista.stream()
                .filter(cert -> cert.getEstado() == Certificacion.EstadoCertificacion.VIGENTE)
                .count());
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
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c == null || certificacion == null || certificacion.getColaborador() == null
                || !c.getIdColaborador().equals(certificacion.getColaborador().getIdColaborador())) {
            return "redirect:/colaborador/certificaciones";
        }

        model.addAttribute("titulo", "Editar certificación");
        model.addAttribute("certificacion", certificacion);
        agregarArchivoActual(model, certificacion.getIdCertificacion());
        return "colaborador/certificacion-form";
    }

    @PostMapping("/certificaciones/guardar")
    public String guardarCertificacion(Certificacion certificacion,
                                       @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        Colaborador c = colaboradorDemo();
        Certificacion existente = null;

        if (c == null) {
            prepararErrorCertificacion(certificacion, model, "No se encontró el perfil del colaborador.");
            return "colaborador/certificacion-form";
        }

        if (certificacion.getIdCertificacion() != null) {
            existente = certificacionRepository.findById(certificacion.getIdCertificacion()).orElse(null);

            if (existente == null || existente.getColaborador() == null
                    || !c.getIdColaborador().equals(existente.getColaborador().getIdColaborador())) {
                return "redirect:/colaborador/certificaciones";
            }
        }

        String nombre = certificacion.getNombre() == null ? "" : certificacion.getNombre().trim();
        String entidad = certificacion.getEntidadEmisora() == null ? "" : certificacion.getEntidadEmisora().trim();

        if (nombre.isBlank() || nombre.length() > 40 || entidad.length() > 40) {
            prepararErrorCertificacion(certificacion, model,
                    "El nombre es obligatorio y el nombre/entidad admiten como máximo 40 caracteres.");
            return "colaborador/certificacion-form";
        }

        if (certificacion.getFechaEmision() != null && certificacion.getFechaExpiracion() != null
                && certificacion.getFechaExpiracion().isBefore(certificacion.getFechaEmision())) {
            prepararErrorCertificacion(certificacion, model,
                    "La fecha de expiración no puede ser anterior a la fecha de emisión.");
            return "colaborador/certificacion-form";
        }

        if (archivo != null && !archivo.isEmpty()) {
            String errorArchivo = validarArchivoCertificacion(archivo);
            if (errorArchivo != null) {
                prepararErrorCertificacion(certificacion, model, errorArchivo);
                return "colaborador/certificacion-form";
            }
        }

        certificacion.setNombre(nombre);
        certificacion.setEntidadEmisora(entidad);
        certificacion.setColaborador(c);

        Certificacion guardada = certificacionRepository.save(certificacion);

        if (archivo != null && !archivo.isEmpty()) {
            try {
                guardarArchivoCertificacion(guardada.getIdCertificacion(), archivo);
            } catch (IOException e) {
                prepararErrorCertificacion(guardada, model,
                        "Los datos se guardaron, pero no se pudo almacenar el archivo. Intenta nuevamente.");
                return "colaborador/certificacion-form";
            }
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Certificación guardada correctamente.");
        return "redirect:/colaborador/certificaciones";
    }

    @GetMapping("/certificaciones/{id}/archivo")
    public void verArchivoCertificacion(@PathVariable Integer id,
                                        HttpServletResponse response) throws IOException {
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c == null || certificacion == null || certificacion.getColaborador() == null
                || !c.getIdColaborador().equals(certificacion.getColaborador().getIdColaborador())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path ruta = buscarArchivoCertificacion(id);
        if (ruta == null || !Files.exists(ruta) || !Files.isRegularFile(ruta)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String tipo = Files.probeContentType(ruta);
        response.setContentType(tipo == null ? "application/octet-stream" : tipo);

        String nombreVisible = obtenerNombreVisibleArchivo(ruta).replaceAll("[\r\n\"]", "");
        response.setHeader("Content-Disposition", "inline; filename=\"" + nombreVisible + "\"");
        response.setContentLengthLong(Files.size(ruta));
        Files.copy(ruta, response.getOutputStream());
    }

    @PostMapping("/certificaciones/{id}/archivo/eliminar")
    public String eliminarArchivoCertificacionGuardado(@PathVariable Integer id,
                                                        RedirectAttributes redirectAttributes) {
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c == null || certificacion == null || certificacion.getColaborador() == null
                || !c.getIdColaborador().equals(certificacion.getColaborador().getIdColaborador())) {
            return "redirect:/colaborador/certificaciones";
        }

        Path ruta = buscarArchivoCertificacion(id);
        if (ruta == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "La certificación no tiene un archivo adjunto.");
        } else {
            eliminarArchivoCertificacion(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Archivo eliminado. La certificación se conserva.");
        }

        return "redirect:/colaborador/certificaciones/editar/" + id;
    }

    @PostMapping("/certificaciones/{id}/eliminar")
    public String eliminarCertificacion(@PathVariable Integer id,
                                        RedirectAttributes redirectAttributes) {
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c != null && certificacion != null && certificacion.getColaborador() != null
                && c.getIdColaborador().equals(certificacion.getColaborador().getIdColaborador())) {
            eliminarArchivoCertificacion(id);
            certificacionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Certificación eliminada del perfil.");
        }

        return "redirect:/colaborador/certificaciones";
    }

    private void prepararErrorCertificacion(Certificacion certificacion, Model model, String mensaje) {
        model.addAttribute("titulo",
                certificacion.getIdCertificacion() == null ? "Nueva certificación" : "Editar certificación");
        model.addAttribute("certificacion", certificacion);
        model.addAttribute("mensajeError", mensaje);
        agregarArchivoActual(model, certificacion.getIdCertificacion());
    }

    private String validarArchivoCertificacion(MultipartFile archivo) {
        if (archivo.getSize() > 5 * 1024 * 1024) {
            return "El archivo no puede superar los 5 MB.";
        }

        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank() || !nombre.contains(".")) {
            return "Selecciona un archivo válido.";
        }

        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
        List<String> permitidas = List.of("pdf", "jpg", "jpeg", "png");

        if (!permitidas.contains(extension)) {
            return "Formato no permitido. Usa PDF, JPG o PNG.";
        }

        return null;
    }

    /*
     * El documento se guarda en una carpeta usando el ID de la certificación.
     * De esta forma no necesitamos agregar columnas nuevas a la tabla certificaciones.
     */
    private Path carpetaCertificaciones() {
        return Path.of("uploads", "certificaciones").toAbsolutePath().normalize();
    }

    private Path buscarArchivoCertificacion(Integer idCertificacion) {
        if (idCertificacion == null) return null;

        Path carpeta = carpetaCertificaciones();
        if (!Files.exists(carpeta)) return null;

        String prefijo = idCertificacion + "__";

        try (var archivos = Files.list(carpeta)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefijo))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private String obtenerNombreVisibleArchivo(Path ruta) {
        if (ruta == null) return "archivo";

        String nombreInterno = ruta.getFileName().toString();
        int separador = nombreInterno.indexOf("__");

        return separador >= 0
                ? nombreInterno.substring(separador + 2)
                : nombreInterno;
    }

    private void agregarArchivoActual(Model model, Integer idCertificacion) {
        Path ruta = buscarArchivoCertificacion(idCertificacion);
        if (ruta != null) {
            model.addAttribute("archivoActual", obtenerNombreVisibleArchivo(ruta));
        }
    }

    private void guardarArchivoCertificacion(Integer idCertificacion,
                                              MultipartFile archivo) throws IOException {
        if (idCertificacion == null) {
            throw new IOException("La certificación debe estar guardada antes de adjuntar un archivo.");
        }

        Path carpeta = carpetaCertificaciones();
        Files.createDirectories(carpeta);

        String original = archivo.getOriginalFilename() == null
                ? "certificacion.pdf"
                : archivo.getOriginalFilename();

        String limpio = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (limpio.isBlank()) limpio = "certificacion.pdf";

        eliminarArchivoCertificacion(idCertificacion);

        String nombreInterno = idCertificacion + "__" + limpio;
        Path destino = carpeta.resolve(nombreInterno).normalize();

        if (!destino.startsWith(carpeta)) {
            throw new IOException("Ruta de archivo inválida.");
        }

        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private void eliminarArchivoCertificacion(Integer idCertificacion) {
        if (idCertificacion == null) return;

        Path carpeta = carpetaCertificaciones();
        if (!Files.exists(carpeta)) return;

        String prefijo = idCertificacion + "__";

        try (var archivos = Files.list(carpeta)) {
            List<Path> encontrados = archivos
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefijo))
                    .toList();

            for (Path archivo : encontrados) {
                try {
                    Files.deleteIfExists(archivo);
                } catch (IOException ignored) {
                    // Si el archivo físico no puede eliminarse, no bloqueamos el CRUD.
                }
            }
        } catch (IOException ignored) {
            // La certificación puede eliminarse aunque la carpeta no pueda leerse.
        }
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

    @PostMapping("/notificaciones/{id}/no-leer")
    public String marcarNoLeida(@PathVariable Integer id,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                RedirectAttributes redirectAttributes) {
        Colaborador c = colaboradorDemo();
        Notificacion n = notificacionRepository.findById(id).orElse(null);

        if (c != null && n != null && n.getUsuario() != null
                && c.getUsuario().getIdUsuario().equals(n.getUsuario().getIdUsuario())) {
            n.setLeida(false);
            notificacionRepository.save(n);
            redirectAttributes.addFlashAttribute("mensajeExito", "Notificación marcada como no leída.");
        }

        return "redirect:/colaborador/notificaciones?page=" + page;
    }

    @PostMapping("/notificaciones/{id}/eliminar")
    public String eliminarNotificacion(@PathVariable Integer id,
                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                       RedirectAttributes redirectAttributes) {
        Colaborador c = colaboradorDemo();
        Notificacion n = notificacionRepository.findById(id).orElse(null);

        if (c != null && n != null && n.getUsuario() != null
                && c.getUsuario().getIdUsuario().equals(n.getUsuario().getIdUsuario())) {
            notificacionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Notificación eliminada.");
        }

        return "redirect:/colaborador/notificaciones?page=" + page;
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

    @PostMapping("/chat/mensajes/{id}/eliminar")
    public String eliminarMensajeChat(@PathVariable Integer id,
                                      @RequestParam("idProyecto") Integer idProyecto,
                                      RedirectAttributes redirectAttributes) {
        Colaborador colaborador = colaboradorDemo();
        MensajeChat mensaje = mensajeChatRepository.findById(id).orElse(null);

        if (colaborador != null && mensaje != null && mensaje.getUsuario() != null
                && mensaje.getProyecto() != null
                && colaborador.getUsuario().getIdUsuario().equals(mensaje.getUsuario().getIdUsuario())
                && idProyecto.equals(mensaje.getProyecto().getIdProyecto())
                && proyectoChatPermitido(asignacionesChat(colaborador), idProyecto) != null) {
            mensajeChatRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Mensaje eliminado.");
        }

        return "redirect:/colaborador/chat?idProyecto=" + idProyecto;
    }
}
