package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador web que gestiona las vistas y operaciones del perfil del colaborador.
 */
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

    @Value("${skillbridge.upload-dir:uploads}")
    private String uploadDir;

    private static final long MAX_CREDENCIAL_BYTES = 5L * 1024L * 1024L;

    /**
     * Crea el controlador e inicializa los repositorios utilizados por sus operaciones.
     *
     * @param usuarioRepository repositorio de usuarios
     * @param colaboradorRepository repositorio de colaboradores
     * @param habilidadRepository repositorio de habilidades
     * @param certificacionRepository repositorio de certificaciones
     * @param asignacionRepository repositorio de asignaciones
     * @param foroRepository repositorio de foros
     * @param notificacionRepository repositorio de notificaciones
     * @param mensajeChatRepository repositorio de mensajes de chat
     * @param respuestaForoRepository repositorio de respuestas de foro
     */
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

    /**
     * Obtiene el colaborador asociado a la sesión, o selecciona un colaborador disponible.
     *
     * @return colaborador encontrado o {@code null} si no existe ninguno
     */
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

    /**
     * Consulta y convierte las habilidades registradas para un colaborador.
     *
     * @param idColaborador identificador del colaborador
     * @return lista de habilidades del colaborador
     */
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

    /**
     * Comprueba que un texto contenga únicamente letras y espacios con máximo veinte caracteres.
     *
     * @param valor texto a validar
     * @return {@code true} si el texto cumple el formato; {@code false} en caso contrario
     */
    private boolean soloLetrasHasta20(String valor) {
        return valor != null && valor.matches("^[\\p{L} ]{1,20}$");
    }

    /**
     * Valida los datos personales que se pueden editar en el perfil.
     *
     * @param nombres nombres del usuario
     * @param apellidos apellidos del usuario
     * @param telefono teléfono del usuario
     * @return mensaje de error o {@code null} si los datos son válidos
     */
    private String validarDatosUsuario(String nombres, String apellidos, String telefono) {
        if (!soloLetrasHasta20(nombres)) return "Los nombres solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (!soloLetrasHasta20(apellidos)) return "Los apellidos solo pueden contener letras y espacios, con un máximo de 20 caracteres.";
        if (telefono == null || !telefono.matches("^[0-9]{9}$")) return "El teléfono debe contener exactamente 9 dígitos.";
        return null;
    }

    /**
     * Prepara el resumen del espacio de trabajo del colaborador.
     *
     * @param model modelo utilizado para enviar datos a la vista
     * @return nombre de la vista de inicio
     */
    @RequestMapping(value = "/inicio", method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Inicio");
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

    /**
     * Carga el perfil profesional y sus certificaciones y habilidades.
     *
     * @param model modelo utilizado para enviar datos a la vista
     * @return nombre de la vista de perfil
     */
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

    /**
     * Prepara el formulario para editar el perfil profesional.
     *
     * @param model modelo utilizado para enviar datos a la vista
     * @return nombre de la vista del formulario de perfil
     */
    @GetMapping("/perfil/editar")
    public String editarPerfil(Model model) {
        model.addAttribute("titulo", "Editar perfil profesional");
        model.addAttribute("colaborador", colaboradorDemo());
        return "colaborador/perfil-form";
    }

    /**
     * Valida y guarda los datos personales y profesionales editados del colaborador.
     *
     * @param colaborador datos profesionales enviados en el formulario
     * @param bindingResult resultado de las validaciones del formulario
     * @param nombres nombres editados del usuario
     * @param apellidos apellidos editados del usuario
     * @param telefono teléfono editado del usuario
     * @param model modelo utilizado para comunicar el resultado
     * @return vista del formulario ante un error o vista del perfil tras guardar
     */
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

    /**
     * Lista las certificaciones del colaborador en orden de emisión descendente.
     *
     * @param model modelo utilizado para enviar la información a la vista
     * @return nombre de la vista de certificaciones
     */
    @GetMapping("/certificaciones")
    public String certificaciones(Model model) {
        Colaborador c = colaboradorDemo();
        List<Certificacion> lista = c == null ? List.of()
                : certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        return "colaborador/certificaciones";
    }

    /**
     * Agrega al modelo el título, la lista y el número de certificaciones vigentes.
     *
     * @param model modelo que recibirá los atributos
     * @param lista certificaciones que se mostrarán
     */
    private void cargarCertificaciones(Model model, List<Certificacion> lista) {
        model.addAttribute("titulo", "Certificaciones y credenciales");
        model.addAttribute("certificaciones", lista);
        model.addAttribute("vigentes", lista.stream()
                .filter(cert -> cert.getEstado() == Certificacion.EstadoCertificacion.VIGENTE)
                .count());
    }

    /**
     * Crea una certificación vacía con estado vigente para el formulario de alta.
     *
     * @param model modelo utilizado para enviar la información a la vista
     * @return nombre de la vista del formulario de certificación
     */
    @GetMapping("/certificaciones/nueva")
    public String nuevaCertificacion(Model model) {
        Certificacion certificacion = new Certificacion();
        certificacion.setEstado(Certificacion.EstadoCertificacion.VIGENTE);
        model.addAttribute("titulo", "Nueva certificación");
        model.addAttribute("certificacion", certificacion);
        return "colaborador/certificacion-form";
    }

    /**
     * Muestra una certificación en el formulario solo si pertenece al colaborador actual.
     *
     * @param id identificador de la certificación
     * @param model modelo utilizado para enviar datos a la vista
     * @return vista del formulario o lista de certificaciones ante una edición no válida
     */
    @GetMapping("/certificaciones/editar/{id}")
    public String editarCertificacion(@PathVariable Integer id, Model model) {
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c == null || certificacion == null || !certificacionPerteneceA(certificacion, c)) {
            List<Certificacion> lista = c == null ? List.of()
                    : certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
            cargarCertificaciones(model, lista);
            model.addAttribute("mensajeError", "La certificación que intentas editar no pertenece a tu perfil.");
            return "colaborador/certificaciones";
        }

        model.addAttribute("titulo", "Editar certificación");
        model.addAttribute("certificacion", certificacion);
        return "colaborador/certificacion-form";
    }

    /**
     * Valida, guarda o actualiza una certificación y su archivo de credencial opcional.
     *
     * @param certificacion datos de la certificación enviados en el formulario
     * @param bindingResult resultado de las validaciones del formulario
     * @param archivoCredencial archivo opcional de la credencial
     * @param model modelo utilizado para comunicar el resultado
     * @return vista del formulario o de la lista de certificaciones
     */
    @PostMapping("/certificaciones/guardar")
    public String guardarCertificacion(@Valid @ModelAttribute("certificacion") Certificacion certificacion,
                                       BindingResult bindingResult,
                                       @RequestParam(value = "archivoCredencial", required = false) MultipartFile archivoCredencial,
                                       Model model) {
        Colaborador c = colaboradorDemo();

        if (c == null) {
            model.addAttribute("titulo", certificacion.getIdCertificacion() == null
                    ? "Nueva certificación" : "Editar certificación");
            model.addAttribute("mensajeError", "No se encontró el perfil del colaborador.");
            return "colaborador/certificacion-form";
        }

        Certificacion existente = null;
        if (certificacion.getIdCertificacion() != null) {
            existente = certificacionRepository.findById(certificacion.getIdCertificacion()).orElse(null);
            if (existente == null || !certificacionPerteneceA(existente, c)) {
                List<Certificacion> lista = certificacionRepository
                        .findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
                cargarCertificaciones(model, lista);
                model.addAttribute("mensajeError", "La certificación que intentas modificar no pertenece a tu perfil.");
                return "colaborador/certificaciones";
            }
        }

        if (bindingResult.hasErrors()) {
            if (existente != null && esArchivoLocal(existente.getUrlCredencial())
                    && (certificacion.getUrlCredencial() == null || certificacion.getUrlCredencial().isBlank())) {
                certificacion.setUrlCredencial(existente.getUrlCredencial());
            }
            model.addAttribute("titulo", certificacion.getIdCertificacion() == null
                    ? "Nueva certificación" : "Editar certificación");
            model.addAttribute("mensajeError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "colaborador/certificacion-form";
        }

        String errorArchivo = validarArchivoCredencial(archivoCredencial);
        if (errorArchivo != null) {
            if (existente != null && esArchivoLocal(existente.getUrlCredencial())
                    && (certificacion.getUrlCredencial() == null || certificacion.getUrlCredencial().isBlank())) {
                certificacion.setUrlCredencial(existente.getUrlCredencial());
            }
            model.addAttribute("titulo", certificacion.getIdCertificacion() == null
                    ? "Nueva certificación" : "Editar certificación");
            model.addAttribute("mensajeError", errorArchivo);
            return "colaborador/certificacion-form";
        }

        String nombre = certificacion.getNombre() == null ? "" : certificacion.getNombre().trim();
        String entidad = certificacion.getEntidadEmisora() == null ? "" : certificacion.getEntidadEmisora().trim();
        String urlIngresada = certificacion.getUrlCredencial() == null ? "" : certificacion.getUrlCredencial().trim();

        certificacion.setNombre(nombre);
        certificacion.setEntidadEmisora(entidad);
        certificacion.setColaborador(c);

        String urlAnterior = existente == null ? null : existente.getUrlCredencial();

        try {
            if (archivoCredencial != null && !archivoCredencial.isEmpty()) {
                String nuevaRuta = guardarArchivoCredencial(archivoCredencial);
                certificacion.setUrlCredencial(nuevaRuta);
            } else if (!urlIngresada.isBlank()) {
                certificacion.setUrlCredencial(urlIngresada);
            } else if (existente != null && esArchivoLocal(urlAnterior)) {
                certificacion.setUrlCredencial(urlAnterior);
            } else {
                certificacion.setUrlCredencial(null);
            }

            Certificacion guardada = certificacionRepository.save(certificacion);

            if (existente != null
                    && esArchivoLocal(urlAnterior)
                    && urlAnterior != null
                    && !urlAnterior.equals(guardada.getUrlCredencial())) {
                eliminarArchivoLocal(urlAnterior);
            }

        } catch (IOException e) {
            if (existente != null && esArchivoLocal(urlAnterior)) {
                certificacion.setUrlCredencial(urlAnterior);
            }
            model.addAttribute("titulo", certificacion.getIdCertificacion() == null
                    ? "Nueva certificación" : "Editar certificación");
            model.addAttribute("mensajeError", "No se pudo guardar el archivo de la credencial. Intenta nuevamente.");
            return "colaborador/certificacion-form";
        }

        List<Certificacion> lista = certificacionRepository
                .findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        model.addAttribute("mensajeExito", "Certificación guardada correctamente.");
        return "colaborador/certificaciones";
    }

    /**
     * Entrega una credencial local si el nombre pertenece al colaborador actual.
     *
     * @param nombreArchivo nombre del archivo almacenado
     * @return recurso de la credencial o respuesta 404 cuando no puede mostrarse
     */
    @GetMapping("/certificaciones/archivo/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verArchivoCredencial(@PathVariable String nombreArchivo) {
        Colaborador c = colaboradorDemo();

        if (c == null || nombreArchivo.contains("..") || nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
            return ResponseEntity.notFound().build();
        }

        String urlEsperada = "/colaborador/certificaciones/archivo/" + nombreArchivo;
        boolean pertenece = certificacionRepository
                .findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador())
                .stream()
                .anyMatch(cert -> urlEsperada.equals(cert.getUrlCredencial()));

        if (!pertenece) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path base = directorioCredenciales();
            Path archivo = base.resolve(nombreArchivo).normalize();

            if (!archivo.startsWith(base) || !Files.exists(archivo) || !Files.isRegularFile(archivo)) {
                return ResponseEntity.notFound().build();
            }

            Resource recurso = new UrlResource(archivo.toUri());
            String tipo = Files.probeContentType(archivo);

            if (tipo == null) {
                tipo = switch (extension(nombreArchivo)) {
                    case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
                    case "png" -> MediaType.IMAGE_PNG_VALUE;
                    case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
                    default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
                };
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + nombreArchivo.replace("\"", "") + "\"")
                    .contentType(MediaType.parseMediaType(tipo))
                    .body(recurso);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina una certificación del colaborador y su archivo local asociado.
     *
     * @param id identificador de la certificación
     * @param model modelo utilizado para mostrar el resultado
     * @return vista de certificaciones
     */
    @PostMapping("/certificaciones/{id}/eliminar")
    public String eliminarCertificacion(@PathVariable Integer id, Model model) {
        Colaborador c = colaboradorDemo();
        Certificacion certificacion = certificacionRepository.findById(id).orElse(null);

        if (c == null || certificacion == null || !certificacionPerteneceA(certificacion, c)) {
            List<Certificacion> lista = c == null ? List.of()
                    : certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
            cargarCertificaciones(model, lista);
            model.addAttribute("mensajeError", "La certificación que intentas eliminar no pertenece a tu perfil.");
            return "colaborador/certificaciones";
        }

        String urlArchivo = certificacion.getUrlCredencial();
        certificacionRepository.deleteById(id);

        if (esArchivoLocal(urlArchivo)) {
            eliminarArchivoLocal(urlArchivo);
        }

        List<Certificacion> lista = certificacionRepository
                .findByColaboradorIdColaboradorOrderByFechaEmisionDesc(c.getIdColaborador());
        cargarCertificaciones(model, lista);
        model.addAttribute("mensajeExito", "Certificación eliminada del perfil.");
        return "colaborador/certificaciones";
    }

    /**
     * Comprueba si una certificación está asociada al colaborador indicado.
     *
     * @param certificacion certificación que se debe verificar
     * @param colaborador colaborador propietario esperado
     * @return {@code true} si la certificación pertenece al colaborador; {@code false} en caso contrario
     */
    private boolean certificacionPerteneceA(Certificacion certificacion, Colaborador colaborador) {
        return certificacion.getColaborador() != null
                && certificacion.getColaborador().getIdColaborador() != null
                && certificacion.getColaborador().getIdColaborador().equals(colaborador.getIdColaborador());
    }

    /**
     * Valida el tamaño, extensión, tipo MIME y firma de un archivo de credencial.
     *
     * @param archivo archivo recibido en el formulario
     * @return mensaje de error o {@code null} si el archivo es válido
     */
    private String validarArchivoCredencial(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        if (archivo.getSize() > MAX_CREDENCIAL_BYTES) {
            return "El archivo de la credencial no puede superar los 5 MB.";
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String ext = extension(nombreOriginal);

        if (!List.of("pdf", "png", "jpg", "jpeg").contains(ext)) {
            return "Solo se permiten archivos PDF, PNG, JPG o JPEG.";
        }

        String tipo = archivo.getContentType() == null
                ? ""
                : archivo.getContentType().toLowerCase(Locale.ROOT);

        boolean tipoValido = switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE.equals(tipo);
            case "png" -> MediaType.IMAGE_PNG_VALUE.equals(tipo);
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE.equals(tipo);
            default -> false;
        };

        if (!tipoValido) {
            return "El tipo del archivo no coincide con su extensión.";
        }

        try {
            if (!firmaArchivoValida(archivo, ext)) {
                return "El contenido del archivo no corresponde a un PDF, PNG, JPG o JPEG válido.";
            }
        } catch (IOException e) {
            return "No se pudo validar el archivo seleccionado.";
        }

        return null;
    }

    /**
     * Verifica que la cabecera del archivo coincida con el formato declarado.
     *
     * @param archivo archivo cuya cabecera se inspeccionará
     * @param ext extensión declarada del archivo
     * @return {@code true} si la firma corresponde al formato; {@code false} en caso contrario
     * @throws IOException si no puede abrirse el flujo de lectura del archivo
     */
    private boolean firmaArchivoValida(MultipartFile archivo, String ext) throws IOException {
        byte[] cabecera = new byte[8];

        try (InputStream entrada = archivo.getInputStream()) {
            int leidos = entrada.read(cabecera);

            if (leidos < 3) {
                return false;
            }

            if ("pdf".equals(ext)) {
                return leidos >= 5
                        && cabecera[0] == '%'
                        && cabecera[1] == 'P'
                        && cabecera[2] == 'D'
                        && cabecera[3] == 'F'
                        && cabecera[4] == '-';
            }

            if ("png".equals(ext)) {
                return leidos >= 8
                        && (cabecera[0] & 0xFF) == 0x89
                        && cabecera[1] == 0x50
                        && cabecera[2] == 0x4E
                        && cabecera[3] == 0x47
                        && cabecera[4] == 0x0D
                        && cabecera[5] == 0x0A
                        && cabecera[6] == 0x1A
                        && cabecera[7] == 0x0A;
            }

            return (cabecera[0] & 0xFF) == 0xFF
                    && (cabecera[1] & 0xFF) == 0xD8
                    && (cabecera[2] & 0xFF) == 0xFF;
        }
    }

    /**
     * Guarda un archivo de credencial con un nombre aleatorio dentro del directorio configurado.
     *
     * @param archivo archivo que se copiará
     * @return URL local mediante la cual podrá consultarse la credencial
     * @throws IOException si no puede crearse el directorio o copiarse el archivo
     */
    private String guardarArchivoCredencial(MultipartFile archivo) throws IOException {
        String ext = extension(archivo.getOriginalFilename());
        String nombreSeguro = UUID.randomUUID() + "." + ext;

        Path base = directorioCredenciales();
        Files.createDirectories(base);

        Path destino = base.resolve(nombreSeguro).normalize();

        if (!destino.startsWith(base)) {
            throw new IOException("Ruta de archivo inválida.");
        }

        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/colaborador/certificaciones/archivo/" + nombreSeguro;
    }

    /**
     * Resuelve el directorio absoluto donde se almacenan las credenciales.
     *
     * @return ruta normalizada del directorio de credenciales
     */
    private Path directorioCredenciales() {
        return Path.of(uploadDir, "credenciales").toAbsolutePath().normalize();
    }

    /**
     * Obtiene la extensión de un nombre de archivo en minúsculas.
     *
     * @param nombreArchivo nombre cuyo sufijo se analizará
     * @return extensión sin el punto o cadena vacía si no existe
     */
    private String extension(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }

        int punto = nombreArchivo.lastIndexOf('.');
        if (punto < 0 || punto == nombreArchivo.length() - 1) {
            return "";
        }

        return nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Determina si una URL representa un archivo de credencial almacenado localmente.
     *
     * @param url URL que se inspeccionará
     * @return {@code true} si corresponde al directorio local de credenciales
     */
    private boolean esArchivoLocal(String url) {
        return url != null && url.startsWith("/colaborador/certificaciones/archivo/");
    }

    /**
     * Elimina el archivo local asociado a una URL válida de credencial.
     *
     * @param url URL del archivo que se desea eliminar
     */
    private void eliminarArchivoLocal(String url) {
        if (!esArchivoLocal(url)) {
            return;
        }

        String nombreArchivo = url.substring("/colaborador/certificaciones/archivo/".length());

        if (nombreArchivo.contains("..") || nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
            return;
        }

        try {
            Path base = directorioCredenciales();
            Path archivo = base.resolve(nombreArchivo).normalize();

            if (archivo.startsWith(base)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException ignored) {
            // Si el archivo físico ya no existe, no se impide eliminar/actualizar la certificación.
        }
    }

    /**
     * Muestra el mapa de habilidades del colaborador y, opcionalmente, una habilidad seleccionada para editar.
     *
     * @param idEditar identificador de la habilidad que se editará, o {@code null}
     * @param model modelo utilizado para enviar los datos a la vista
     * @return nombre de la vista de habilidades
     */
    @GetMapping("/habilidades")
    public String habilidades(@RequestParam(value = "editar", required = false) Integer idEditar, Model model) {
        cargarHabilidades(model, idEditar);
        return "colaborador/habilidades";
    }

    /**
     * Carga en el modelo las habilidades actuales y disponibles del colaborador.
     *
     * @param model modelo que recibirá los atributos
     * @param idEditar identificador de la habilidad seleccionada para editar
     */
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

    /**
     * Valida que los años de experiencia y el nivel indicados sean aceptados.
     *
     * @param nivel nivel de dominio seleccionado
     * @param aniosExperiencia años de experiencia indicados
     * @return mensaje de error o {@code null} si ambos valores son válidos
     */
    private String validarExperienciaYNivel(String nivel, BigDecimal aniosExperiencia) {
        if (aniosExperiencia == null || aniosExperiencia.compareTo(BigDecimal.ZERO) < 0 || aniosExperiencia.compareTo(new BigDecimal("50")) > 0) {
            return "Los años de experiencia deben estar entre 0 y 50.";
        }
        if (!List.of("BASICO", "INTERMEDIO", "AVANZADO", "EXPERTO").contains(nivel)) {
            return "Selecciona un nivel válido.";
        }
        return null;
    }

    /**
     * Valida y registra una nueva habilidad en el perfil del colaborador.
     *
     * @param idHabilidad identificador de la habilidad seleccionada
     * @param nivel nivel de dominio seleccionado
     * @param aniosExperiencia años de experiencia asociados a la habilidad
     * @param model modelo utilizado para comunicar el resultado
     * @return nombre de la vista de habilidades
     */
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

    /**
     * Valida y actualiza el nivel y los años de experiencia de una habilidad asociada.
     *
     * @param idHabilidad identificador de la habilidad que se actualizará
     * @param nivel nivel de dominio seleccionado
     * @param aniosExperiencia años de experiencia asociados a la habilidad
     * @param model modelo utilizado para comunicar el resultado
     * @return nombre de la vista de habilidades
     */
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

    /**
     * Retira una habilidad del perfil del colaborador actual.
     *
     * @param idHabilidad identificador de la habilidad que se retirará
     * @param model modelo utilizado para mostrar el resultado
     * @return nombre de la vista de habilidades
     */
    @PostMapping("/habilidades/{idHabilidad}/eliminar")
    public String eliminarHabilidad(@PathVariable Integer idHabilidad, Model model) {
        Colaborador c = colaboradorDemo();
        if (c != null) colaboradorRepository.eliminarHabilidad(c.getIdColaborador(), idHabilidad);
        cargarHabilidades(model, null);
        model.addAttribute("mensajeExito", "Habilidad retirada del perfil.");
        return "colaborador/habilidades";
    }

    /**
     * Lista las asignaciones del colaborador como representación de sus proyectos.
     *
     * @param model modelo utilizado para enviar las asignaciones a la vista
     * @return nombre de la vista de proyectos
     */
    @GetMapping("/proyectos")
    public String proyectos(Model model) {
        Colaborador c = colaboradorDemo();
        model.addAttribute("titulo", "Mis proyectos");
        model.addAttribute("asignaciones", c == null ? List.of()
                : asignacionRepository.findByColaboradorIdColaboradorOrderByFechaInicioDesc(c.getIdColaborador()));
        return "colaborador/proyectos";
    }

    /**
     * Filtra y muestra las asignaciones del colaborador por texto y estado.
     *
     * @param q texto de búsqueda aplicado al proyecto o rol
     * @param estado estado de asignación que se utilizará como filtro
     * @param model modelo utilizado para enviar los resultados a la vista
     * @return nombre de la vista de asignaciones
     */
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

    /**
     * Filtra, pagina y muestra los foros de conocimiento disponibles.
     *
     * @param q texto de búsqueda aplicado al título o contenido
     * @param categoria categoría de foro que se utilizará como filtro
     * @param estado estado de foro que se utilizará como filtro
     * @param page número de página solicitado
     * @param model modelo utilizado para enviar los resultados y estadísticas
     * @return nombre de la vista de foros
     */
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

    /**
     * Lista las notificaciones del usuario asociado al colaborador actual.
     *
     * @param page número de página solicitado
     * @param model modelo utilizado para enviar la información a la vista
     * @return nombre de la vista de notificaciones
     */
    @GetMapping("/notificaciones")
    public String notificaciones(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
        Colaborador c = colaboradorDemo();
        List<Notificacion> lista = c == null ? List.of()
                : notificacionRepository.findByUsuarioIdUsuarioOrderByFechaCreacionDesc(c.getUsuario().getIdUsuario());
        cargarNotificaciones(model, lista, page);
        return "colaborador/notificaciones";
    }

    /**
     * Calcula la página de notificaciones y los totales de mensajes leídos y no leídos.
     *
     * @param model modelo que recibirá los atributos
     * @param lista notificaciones disponibles
     * @param page número de página solicitado
     */
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

    /**
     * Marca una notificación como leída y recarga el listado de notificaciones.
     *
     * @param id identificador de la notificación
     * @param page número de página que se mostrará después de actualizar
     * @param model modelo utilizado para enviar el resultado
     * @return nombre de la vista de notificaciones
     */
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

    /**
     * Marca como leídas todas las notificaciones del colaborador actual.
     *
     * @param model modelo utilizado para recargar el listado
     * @return nombre de la vista de notificaciones
     */
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

    /**
     * Obtiene los proyectos únicos con asignaciones vigentes del colaborador para el chat.
     *
     * @param colaborador colaborador cuya lista de asignaciones se consultará
     * @return asignaciones correspondientes a proyectos permitidos para el chat
     */
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

    /**
     * Selecciona el proyecto permitido para el chat según el identificador indicado.
     *
     * @param asignaciones asignaciones cuyos proyectos están disponibles
     * @param idProyecto identificador del proyecto solicitado
     * @return proyecto permitido o {@code null} si no corresponde a las asignaciones
     */
    private Proyecto proyectoChatPermitido(List<Asignacion> asignaciones, Integer idProyecto) {
        if (asignaciones.isEmpty()) return null;
        if (idProyecto == null) return asignaciones.get(0).getProyecto();
        return asignaciones.stream()
                .map(Asignacion::getProyecto)
                .filter(p -> p != null && p.getIdProyecto().equals(idProyecto))
                .findFirst()
                .orElse(null);
    }

    /**
     * Carga en el modelo los proyectos disponibles y los mensajes del proyecto seleccionado.
     *
     * @param model modelo que recibirá los datos del chat
     * @param colaborador colaborador actual
     * @param idProyecto identificador del proyecto que se mostrará
     */
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

    /**
     * Muestra la conversación del proyecto seleccionado en el chat del colaborador.
     *
     * @param idProyecto identificador del proyecto solicitado
     * @param model modelo utilizado para enviar los datos a la vista
     * @return nombre de la vista de chat
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "idProyecto", required = false) Integer idProyecto,
                       Model model) {
        cargarChat(model, colaboradorDemo(), idProyecto);
        return "colaborador/chat";
    }

    /**
     * Valida y envía un mensaje al proyecto permitido del colaborador.
     *
     * @param idProyecto identificador del proyecto destinatario
     * @param contenido texto del mensaje
     * @param model modelo utilizado para mostrar el resultado
     * @return nombre de la vista de chat
     */
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
