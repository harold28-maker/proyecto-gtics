package pe.edu.pucp.skillbridge.controller;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO.EstadoParticipacion;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadPerfilForm;
import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;
import pe.edu.pucp.skillbridge.service.FotoPerfilService;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final CapacidadColaboradorService capacidadColaboradorService;
    private final FotoPerfilService fotoPerfilService;
    private final TransactionTemplate transacciones;
    private final EntityManager entidades;

    @Value("${skillbridge.upload-dir:uploads}")
    private String uploadDir;

    private static final BigDecimal MAX_ANIOS_EXPERIENCIA = BigDecimal.valueOf(50);
    private static final String HABILIDAD_DUPLICADA = "validation.habilidadPerfil.id.duplicada";

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final int PROYECTOS_POR_PAGINA = 4;
    private static final int CERTIFICACIONES_POR_PAGINA = 4;
    private static final long MAX_CREDENCIAL_BYTES = 5L * 1024L * 1024L;

    /**
     * Crea el controlador e inicializa los repositorios utilizados por sus operaciones.
     *
     * @param usuarioRepository repositorio de usuarios
     * @param colaboradorRepository repositorio de colaboradores
     * @param habilidadRepository repositorio de habilidades
     * @param certificacionRepository repositorio de certificaciones
     * @param asignacionRepository repositorio de asignaciones
     * @param capacidadColaboradorService servicio de carga vigente
     * @param fotoPerfilService servicio compartido de fotos de usuario
     * @param transacciones transacciones utilizadas al guardar el perfil
     * @param entidades contexto de persistencia de la cuenta
     */
    public ColaboradorController(UsuarioRepository usuarioRepository,
                                 ColaboradorRepository colaboradorRepository,
                                 HabilidadRepository habilidadRepository,
                                 CertificacionRepository certificacionRepository,
                                 AsignacionRepository asignacionRepository,
                                 CapacidadColaboradorService capacidadColaboradorService,
                                 FotoPerfilService fotoPerfilService,
                                 TransactionTemplate transacciones,
                                 EntityManager entidades) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.habilidadRepository = habilidadRepository;
        this.certificacionRepository = certificacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.capacidadColaboradorService = capacidadColaboradorService;
        this.fotoPerfilService = fotoPerfilService;
        this.transacciones = transacciones;
        this.entidades = entidades;
    }

    /** Obtiene únicamente el perfil de la cuenta autenticada. */
    private Colaborador colaboradorSesion() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object usuario = attrs == null ? null : attrs.getRequest().getAttribute("usuarioVerificado");
        return usuario instanceof Usuario cuenta
                ? colaboradorRepository.findByUsuarioIdUsuario(cuenta.getIdUsuario()).orElse(null)
                : null;
    }

    /**
     * Consulta y convierte las habilidades registradas para un colaborador.
     *
     * @param idColaborador identificador del colaborador
     * @return lista de habilidades del colaborador
     */
    private List<HabilidadColaboradorDTO> habilidades(Integer idColaborador) {
        return colaboradorRepository.obtenerHabilidades(idColaborador).stream()
                .map(HabilidadColaboradorDTO::desdeFila).toList();
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

    /** Valida los datos personales y conserva el mensaje de cada campo. */
    private Map<String, String> validarDatosUsuario(String nombres, String apellidos, String telefono) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!soloLetrasHasta20(nombres)) {
            errores.put("nombres", "Ingresa solo letras y espacios, hasta 20 caracteres.");
        }
        if (!soloLetrasHasta20(apellidos)) {
            errores.put("apellidos", "Ingresa solo letras y espacios, hasta 20 caracteres.");
        }
        if (!telefono.matches("^[0-9]{9}$")) {
            errores.put("telefono", "Ingresa exactamente 9 dígitos.");
        }
        return errores;
    }

    /**
     * Prepara el resumen del espacio de trabajo del colaborador.
     *
     * @param model modelo utilizado para enviar datos a la vista
     * @return nombre de la vista de inicio
     */
    @RequestMapping(value = "/inicio", method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(Model model) {
        Colaborador colaborador = colaboradorSesion();
        model.addAttribute("titulo", "Inicio");
        model.addAttribute("colaborador", colaborador);
        if (colaborador != null) {
            var resumen = capacidadColaboradorService.consultarPersonal(colaborador);
            model.addAttribute("resumen", resumen);
            model.addAttribute("habilidades", habilidades(colaborador.getIdColaborador()).size());
            model.addAttribute("totalCertificaciones", certificacionRepository
                    .findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(colaborador.getIdColaborador()).size());
        }
        return "colaborador/inicio";
    }

    /**
     * Carga el perfil profesional y los datos que habilita el rol.
     *
     * @param bloqueados permisos deshabilitados para el rol
     * @param model modelo utilizado para enviar datos a la vista
     * @return nombre de la vista de perfil
     */
    @GetMapping("/perfil")
    public String perfil(@RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Colaborador c = colaboradorSesion();
        if (c == null) return "redirect:/colaborador/inicio";
        model.addAttribute("titulo", "Mi perfil profesional");
        model.addAttribute("colaborador", c);
        if (!bloqueados.contains("certificaciones")) {
            model.addAttribute("certificaciones",
                    certificacionRepository.findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(c.getIdColaborador()));
        }
        if (!bloqueados.contains("habilidades")) {
            model.addAttribute("habilidades", habilidades(c.getIdColaborador()));
        }
        if (!bloqueados.contains("asignaciones")) {
            model.addAttribute("capacidad", capacidadColaboradorService.consultarPersonal(c).capacidad());
        }
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
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) return "redirect:/colaborador/inicio";
        Usuario cuenta = colaborador.getUsuario();
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("nombresPerfil", cuenta.getNombres());
        model.addAttribute("apellidosPerfil", cuenta.getApellidos());
        model.addAttribute("telefonoPerfil", cuenta.getTelefono());
        model.addAttribute("erroresPerfil", Map.of());
        return vistaEditarPerfil(model);
    }

    /** Guarda el perfil de la cuenta autenticada tras validar todos los datos en el servidor. */
    @PostMapping("/perfil/guardar")
    public String guardarPerfil(@Valid @ModelAttribute("colaborador") Colaborador cambios,
                                BindingResult bindingResult,
                                @RequestParam(value = "nombres", defaultValue = "") String nombres,
                                @RequestParam(value = "apellidos", defaultValue = "") String apellidos,
                                @RequestParam(value = "telefono", defaultValue = "") String telefono,
                                @RequestParam(value = "foto", required = false) MultipartFile foto,
                                @RequestParam(value = "fotoRecortada", required = false) MultipartFile fotoRecortada,
                                Model model, RedirectAttributes redirect) {
        Colaborador actual = colaboradorSesion();
        if (actual == null) return "redirect:/colaborador/inicio";

        cambios.setIdColaborador(actual.getIdColaborador());
        cambios.setUsuario(actual.getUsuario());
        model.addAttribute("nombresPerfil", nombres);
        model.addAttribute("apellidosPerfil", apellidos);
        model.addAttribute("telefonoPerfil", telefono);
        Map<String, String> errores = validarDatosUsuario(nombres.trim(), apellidos.trim(), telefono.trim());
        model.addAttribute("erroresPerfil", errores);

        boolean cambiarFoto = foto != null && (!foto.isEmpty() || StringUtils.hasText(foto.getOriginalFilename()));
        if (fotoRecortada != null && !fotoRecortada.isEmpty() && !cambiarFoto) {
            errores.put("foto", "Selecciona primero una foto de perfil.");
        }
        if (bindingResult.hasErrors() || !errores.isEmpty()) {
            return vistaEditarPerfil(model);
        }

        String fotoNueva;
        try {
            fotoNueva = cambiarFoto ? fotoPerfilService.guardar(foto, fotoRecortada) : null;
        } catch (IllegalArgumentException ex) {
            errores.put("foto", ex.getMessage());
            return vistaEditarPerfil(model);
        } catch (IOException ex) {
            errores.put("foto", "No se pudo guardar la foto. Vuelve a intentarlo.");
            return vistaEditarPerfil(model);
        }

        Integer idUsuario = actual.getUsuario().getIdUsuario();
        PerfilGuardado guardado = null;
        try {
            guardado = transacciones.execute(estado -> actualizarPerfil(idUsuario, cambios,
                    nombres.trim(), apellidos.trim(), telefono.trim(), fotoNueva));
        } catch (DataAccessException ex) {
            model.addAttribute("mensajeError", "No se pudo guardar el perfil. Vuelve a intentarlo.");
            return vistaEditarPerfil(model);
        } finally {
            if (guardado == null) fotoPerfilService.eliminar(fotoNueva);
        }

        if (guardado == null) return "redirect:/colaborador/inicio";
        if (fotoNueva != null) fotoPerfilService.eliminar(guardado.fotoAnterior());
        redirect.addFlashAttribute("mensajeExito", "Tu perfil fue actualizado correctamente.");
        return "redirect:/colaborador/perfil";
    }

    private String vistaEditarPerfil(Model model) {
        model.addAttribute("titulo", "Editar perfil profesional");
        return "colaborador/perfil-form";
    }

    private record PerfilGuardado(String fotoAnterior) {}

    private PerfilGuardado actualizarPerfil(Integer idUsuario, Colaborador cambios, String nombres,
                                            String apellidos, String telefono, String fotoNueva) {
        Usuario cuenta = usuarioRepository.buscarParaActualizar(idUsuario).orElse(null);
        if (cuenta == null) return null;
        entidades.refresh(cuenta);
        if (cuenta.getEstado() != Usuario.EstadoUsuario.ACTIVO || cuenta.getRol() == null
                || !"COLABORADOR".equals(cuenta.getRol().getNombre())) {
            return null;
        }
        Colaborador perfil = colaboradorRepository.findByUsuarioIdUsuario(idUsuario).orElse(null);
        if (perfil == null) return null;

        String fotoAnterior = cuenta.getFotoUrl();
        cuenta.setNombres(nombres);
        cuenta.setApellidos(apellidos);
        cuenta.setTelefono(telefono);
        if (fotoNueva != null) cuenta.setFotoUrl(fotoNueva);

        perfil.setCargo(cambios.getCargo().trim());
        perfil.setArea(cambios.getArea().trim());
        perfil.setSeniority(cambios.getSeniority());
        perfil.setBiografia(cambios.getBiografia());
        perfil.setInteresesProfesionales(cambios.getInteresesProfesionales());
        perfil.setDisponibilidadBase(cambios.getDisponibilidadBase());

        usuarioRepository.save(cuenta);
        colaboradorRepository.save(perfil);
        return new PerfilGuardado(fotoAnterior);
    }

    /** Lista las certificaciones del colaborador desde la más reciente. */
    @GetMapping("/certificaciones")
    public String certificaciones(@RequestParam(defaultValue = "1") int page, Model model) {
        List<Certificacion> lista = listarCertificaciones(colaboradorSesion());
        int total = lista.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) CERTIFICACIONES_POR_PAGINA));
        int paginaActual = Math.max(1, Math.min(page, totalPaginas));
        int desde = (paginaActual - 1) * CERTIFICACIONES_POR_PAGINA;
        int hasta = Math.min(desde + CERTIFICACIONES_POR_PAGINA, total);
        long vencidas = lista.stream()
                .filter(cert -> cert.getEstadoActual() == Certificacion.EstadoCertificacion.VENCIDA)
                .count();

        model.addAttribute("titulo", "Certificaciones");
        model.addAttribute("certificaciones", lista.subList(desde, hasta));
        model.addAttribute("totalCertificaciones", total);
        model.addAttribute("vigentes", total - vencidas);
        model.addAttribute("vencidas", vencidas);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("primerResultado", desde + 1);
        model.addAttribute("ultimoResultado", hasta);
        return "colaborador/certificaciones";
    }

    private List<Certificacion> listarCertificaciones(Colaborador colaborador) {
        return colaborador == null ? List.of() : certificacionRepository
                .findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(colaborador.getIdColaborador());
    }

    @InitBinder("certificacion")
    public void prepararDatosCertificacion(WebDataBinder binder) {
        binder.setAllowedFields("idCertificacion", "nombre", "entidadEmisora",
                "fechaEmision", "fechaExpiracion", "urlCredencial");
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/certificaciones/nueva")
    public String nuevaCertificacion(@RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("certificacion", new Certificacion());
        return cargarFormularioCertificacion(model, null, false, page);
    }

    @GetMapping("/certificaciones/editar/{id}")
    public String editarCertificacion(@PathVariable Integer id, @RequestParam(defaultValue = "1") int page,
                                      Model model, RedirectAttributes redirect) {
        Certificacion actual = certificacionPropia(colaboradorSesion(), id);
        if (actual == null) {
            return volverACertificaciones(redirect, page, "La certificación no pertenece a tu perfil.");
        }

        Certificacion formulario = new Certificacion();
        formulario.setIdCertificacion(actual.getIdCertificacion());
        formulario.setNombre(actual.getNombre());
        formulario.setEntidadEmisora(actual.getEntidadEmisora());
        formulario.setFechaEmision(actual.getFechaEmision());
        formulario.setFechaExpiracion(actual.getFechaExpiracion());
        if (!esArchivoLocal(actual.getUrlCredencial())) {
            formulario.setUrlCredencial(actual.getUrlCredencial());
        }

        model.addAttribute("certificacion", formulario);
        boolean sinExpiracion = actual.getEstadoActual() == Certificacion.EstadoCertificacion.SIN_EXPIRACION;
        return cargarFormularioCertificacion(model, actual, sinExpiracion, page);
    }

    /** Valida el formulario antes de guardar la certificación y su credencial opcional. */
    @PostMapping("/certificaciones/guardar")
    public String guardarCertificacion(@Valid @ModelAttribute("certificacion") Certificacion formulario,
                                       BindingResult errores,
                                       @RequestParam(required = false) MultipartFile archivoCredencial,
                                       @RequestParam(defaultValue = "false") boolean sinExpiracion,
                                       @RequestParam(defaultValue = "1") int page,
                                       Model model, RedirectAttributes redirect) {
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) {
            return volverACertificaciones(redirect, page, "No se encontró tu perfil de colaborador.");
        }

        if (sinExpiracion && formulario.getFechaExpiracion() != null) {
            errores.rejectValue("fechaExpiracion", "validation.certificacion.expiracion.incompatible");
        }

        if (!errores.hasFieldErrors("urlCredencial") && !enlaceCredencialValido(formulario.getUrlCredencial())) {
            errores.rejectValue("urlCredencial", "validation.certificacion.url.pattern");
        }

        String errorArchivo = validarArchivoCredencial(archivoCredencial);
        boolean subirArchivo = archivoCredencial != null && !archivoCredencial.isEmpty();
        if (errores.hasErrors() || errorArchivo != null) {
            if (subirArchivo && errorArchivo == null) {
                errorArchivo = "Vuelve a seleccionar el archivo antes de guardar.";
            }
            model.addAttribute("errorArchivo", errorArchivo);
            return volverAlFormularioCertificacion(formulario, colaborador, sinExpiracion, page, model, redirect);
        }

        String archivoNuevo = null;
        CertificacionGuardada guardada = null;
        try {
            archivoNuevo = subirArchivo ? guardarArchivoCredencial(archivoCredencial) : null;
            String rutaNueva = archivoNuevo;
            guardada = transacciones.execute(estado ->
                    actualizarCertificacion(formulario, colaborador, sinExpiracion, rutaNueva));
        } catch (IOException ex) {
            model.addAttribute("errorArchivo", "No se pudo guardar el archivo. Vuelve a seleccionarlo.");
            return volverAlFormularioCertificacion(formulario, colaborador, sinExpiracion, page, model, redirect);
        } catch (DataAccessException | TransactionException ex) {
            model.addAttribute("mensajeError", "No pudimos guardar la certificación. Vuelve a intentarlo.");
            if (subirArchivo) {
                model.addAttribute("errorArchivo", "Vuelve a seleccionar el archivo antes de guardar.");
            }
            return volverAlFormularioCertificacion(formulario, colaborador, sinExpiracion, page, model, redirect);
        } finally {
            if (guardada == null) eliminarArchivoLocal(archivoNuevo);
        }

        if (guardada == null) {
            return volverACertificaciones(redirect, page, "La certificación no pertenece a tu perfil.");
        }

        eliminarArchivoLocal(guardada.archivoAnterior());
        redirect.addFlashAttribute("mensajeExito", "Certificación guardada correctamente.");
        return volverACertificaciones(redirect, buscarPaginaCertificacion(colaborador, guardada.id()), null);
    }

    private String cargarFormularioCertificacion(Model model, Certificacion actual, boolean sinExpiracion, int page) {
        model.addAttribute("titulo", actual == null ? "Nueva certificación" : "Editar certificación");
        model.addAttribute("certificacionActual", actual);
        boolean tieneCredencial = actual != null && StringUtils.hasText(actual.getUrlCredencial());
        model.addAttribute("credencialActual", tieneCredencial ? actual.getUrlCredencial() : null);
        model.addAttribute("archivoActual", tieneCredencial && esArchivoLocal(actual.getUrlCredencial()));
        model.addAttribute("sinExpiracion", sinExpiracion);
        model.addAttribute("paginaLista", Math.max(1, page));
        return "colaborador/certificacion-form";
    }

    private String volverAlFormularioCertificacion(Certificacion formulario, Colaborador colaborador, boolean sinExpiracion,
                                          int page, Model model, RedirectAttributes redirect) {
        Integer id = formulario.getIdCertificacion();
        Certificacion actual = id == null ? null : certificacionPropia(colaborador, id);
        if (id != null && actual == null) {
            return volverACertificaciones(redirect, page, "La certificación no pertenece a tu perfil.");
        }
        return cargarFormularioCertificacion(model, actual, sinExpiracion, page);
    }

    private Certificacion certificacionPropia(Colaborador colaborador, Integer id) {
        return colaborador == null ? null : certificacionRepository
                .findByIdCertificacionAndColaboradorIdColaborador(id, colaborador.getIdColaborador()).orElse(null);
    }

    private String volverACertificaciones(RedirectAttributes redirect, int page, String error) {
        if (page > 1) redirect.addAttribute("page", page);
        if (error != null) redirect.addFlashAttribute("mensajeError", error);
        return "redirect:/colaborador/certificaciones";
    }

    private int buscarPaginaCertificacion(Colaborador colaborador, Integer idCertificacion) {
        List<Certificacion> lista = listarCertificaciones(colaborador);
        for (int i = 0; i < lista.size(); i++) {
            if (idCertificacion.equals(lista.get(i).getIdCertificacion())) {
                return i / CERTIFICACIONES_POR_PAGINA + 1;
            }
        }
        return 1;
    }

    private boolean enlaceCredencialValido(String enlace) {
        if (enlace == null) return true;

        try {
            URI uri = new URI(enlace);
            return uri.getHost() != null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private record CertificacionGuardada(Integer id, String archivoAnterior) {}

    private CertificacionGuardada actualizarCertificacion(Certificacion formulario, Colaborador colaborador,
                                                           boolean sinExpiracion, String archivoNuevo) {
        Integer id = formulario.getIdCertificacion();
        Certificacion actual = id == null ? new Certificacion() : certificacionRepository
                .buscarParaActualizar(id, colaborador.getIdColaborador()).orElse(null);
        if (actual == null) return null;

        String urlAnterior = actual.getUrlCredencial();
        String url = formulario.getUrlCredencial();
        if (archivoNuevo != null) {
            url = archivoNuevo;
        } else if (url == null && esArchivoLocal(urlAnterior)) {
            url = urlAnterior;
        }

        boolean vencidaSinFecha = actual.getFechaExpiracion() == null
                && actual.getEstado() == Certificacion.EstadoCertificacion.VENCIDA;
        actual.setColaborador(colaborador);
        actual.setNombre(formulario.getNombre());
        actual.setEntidadEmisora(formulario.getEntidadEmisora());
        actual.setFechaEmision(formulario.getFechaEmision());
        actual.setFechaExpiracion(formulario.getFechaExpiracion());
        actual.setUrlCredencial(url);
        if (sinExpiracion) {
            actual.setEstado(Certificacion.EstadoCertificacion.SIN_EXPIRACION);
        } else {
            actual.setEstado(vencidaSinFecha ? Certificacion.EstadoCertificacion.VENCIDA : Certificacion.EstadoCertificacion.VIGENTE);
            actual.setEstado(actual.getEstadoActual());
        }

        Certificacion guardada = certificacionRepository.save(actual);
        String archivoAnterior = esArchivoLocal(urlAnterior) && !urlAnterior.equals(url) ? urlAnterior : null;
        return new CertificacionGuardada(guardada.getIdCertificacion(), archivoAnterior);
    }

    /**
     * Entrega una credencial local si el nombre pertenece al colaborador actual.
     *
     * @param nombreArchivo nombre del archivo almacenado
     * @return recurso de la credencial o respuesta 404 cuando no puede mostrarse
     */
    @GetMapping("/certificaciones/archivo/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verArchivoCredencial(@PathVariable String nombreArchivo) {
        Colaborador c = colaboradorSesion();

        if (c == null || nombreArchivo.contains("..") || nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
            return ResponseEntity.notFound().build();
        }

        String urlEsperada = "/colaborador/certificaciones/archivo/" + nombreArchivo;
        boolean pertenece = certificacionRepository
                .existsByColaboradorIdColaboradorAndUrlCredencial(c.getIdColaborador(), urlEsperada);

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

    /** Elimina la certificación y luego su archivo local, si lo tiene. */
    @PostMapping("/certificaciones/{id}/eliminar")
    public String eliminarCertificacion(@PathVariable Integer id, @RequestParam(defaultValue = "1") int page,
                                        RedirectAttributes redirect) {
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) {
            return volverACertificaciones(redirect, page, "No se encontró tu perfil de colaborador.");
        }

        Certificacion eliminada;
        try {
            eliminada = transacciones.execute(estado -> {
                Certificacion actual = certificacionRepository.buscarParaActualizar(id, colaborador.getIdColaborador())
                        .orElse(null);
                if (actual != null) certificacionRepository.delete(actual);
                return actual;
            });
        } catch (DataAccessException | TransactionException ex) {
            return volverACertificaciones(redirect, page, "No pudimos eliminar la certificación. Vuelve a intentarlo.");
        }

        if (eliminada == null) {
            return volverACertificaciones(redirect, page, "La certificación no pertenece a tu perfil.");
        }
        eliminarArchivoLocal(eliminada.getUrlCredencial());
        redirect.addFlashAttribute("mensajeExito", "Certificación eliminada del perfil.");
        return volverACertificaciones(redirect, page, null);
    }

    /**
     * Valida el tamaño, extensión, tipo MIME y firma de un archivo de credencial.
     *
     * @param archivo archivo recibido en el formulario
     * @return mensaje de error o {@code null} si el archivo es válido
     */
    private String validarArchivoCredencial(MultipartFile archivo) {
        if (archivo == null) return null;
        if (archivo.isEmpty()) {
            return StringUtils.hasText(archivo.getOriginalFilename()) ? "El archivo seleccionado está vacío." : null;
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
            int leidos = entrada.readNBytes(cabecera, 0, cabecera.length);

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
        Path base = directorioCredenciales();
        Files.createDirectories(base);

        Path destino = Files.createTempFile(base, "credencial-", "." + ext);
        String url = "/colaborador/certificaciones/archivo/" + destino.getFileName();
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            eliminarArchivoLocal(url);
            throw ex;
        }
        return url;
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

    /** Muestra las habilidades propias y abre el formulario o la confirmación solicitada. */
    @GetMapping("/habilidades")
    public String habilidades(@RequestParam(value = "editar", required = false) Integer idEditar,
                              @RequestParam(value = "quitar", required = false) Integer idQuitar,
                              Model model, RedirectAttributes redirect) {
        return vistaHabilidades(model, colaboradorSesion(), idEditar, idQuitar, redirect);
    }

    private String vistaHabilidades(Model model, Colaborador colaborador, Integer idEditar, Integer idQuitar,
                                    RedirectAttributes redirect) {
        boolean tienePerfil = colaborador != null;
        List<HabilidadColaboradorDTO> actuales = tienePerfil ? habilidades(colaborador.getIdColaborador()) : List.of();
        HabilidadColaboradorDTO editar = buscarHabilidadPropia(actuales, idEditar);
        HabilidadColaboradorDTO quitar = buscarHabilidadPropia(actuales, idQuitar);
        if ((idEditar != null && editar == null) || (idQuitar != null && quitar == null)) {
            return volverAHabilidades(redirect, "mensajeError", "La habilidad ya no está en tu perfil.");
        }

        Set<Integer> idsRegistrados = actuales.stream()
                .map(HabilidadColaboradorDTO::getIdHabilidad).collect(Collectors.toSet());
        List<Habilidad> disponibles = tienePerfil ? habilidadRepository.findByEstadoTrueOrderByNombreAsc().stream()
                .filter(h -> !idsRegistrados.contains(h.getIdHabilidad())).toList() : List.of();
        List<String> categorias = actuales.stream().map(HabilidadColaboradorDTO::getCategoria)
                .filter(StringUtils::hasText).map(String::strip).distinct().toList();

        model.addAttribute("titulo", "Mis habilidades");
        model.addAttribute("tienePerfil", tienePerfil);
        model.addAttribute("habilidades", actuales);
        model.addAttribute("habilidadesDisponibles", disponibles);
        model.addAttribute("totalCategorias", categorias.size());
        model.addAttribute("categoriasVisibles", String.join(" · ", categorias.stream().limit(3).toList()));
        model.addAttribute("categoriasRestantes", Math.max(0, categorias.size() - 3));
        model.addAttribute("niveles", HabilidadColaboradorDTO.Nivel.values());
        model.addAttribute("habilidadEditar", editar);
        model.addAttribute("habilidadQuitar", quitar);
        if (!model.containsAttribute("habilidadForm")) {
            HabilidadPerfilForm formulario = new HabilidadPerfilForm();
            if (editar != null) {
                formulario.setIdHabilidad(editar.getIdHabilidad());
                formulario.setNivel(editar.getNivelHabilidad());
                formulario.setAniosExperiencia(editar.getAniosExperienciaLocal());
            }
            model.addAttribute("habilidadForm", formulario);
        }
        return "colaborador/habilidades";
    }

    private HabilidadColaboradorDTO buscarHabilidadPropia(List<HabilidadColaboradorDTO> habilidades, Integer id) {
        return id == null ? null : habilidades.stream()
                .filter(habilidad -> id.equals(habilidad.getIdHabilidad())).findFirst().orElse(null);
    }

    /** Valida en el servidor el formato y el rango, conservando el texto ingresado. */
    private BigDecimal validarAniosExperiencia(String experiencia, BindingResult errores) {
        if (errores.hasFieldErrors("aniosExperiencia")) return null;

        String valor = experiencia.strip();
        if (!valor.matches("[+-]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)")) {
            errores.rejectValue("aniosExperiencia", "validation.habilidadPerfil.anios.formato");
            return null;
        }

        BigDecimal anios = new BigDecimal(valor.replace(',', '.'));
        if (anios.compareTo(BigDecimal.ZERO) < 0 || anios.compareTo(MAX_ANIOS_EXPERIENCIA) > 0) {
            errores.rejectValue("aniosExperiencia", "validation.habilidadPerfil.anios.rango");
        } else if (anios.scale() > 1) {
            errores.rejectValue("aniosExperiencia", "validation.habilidadPerfil.anios.decimales");
        }
        return anios;
    }

    @PostMapping("/habilidades/guardar")
    public String guardarHabilidad(@Valid @ModelAttribute("habilidadForm") HabilidadPerfilForm formulario,
                                   BindingResult errores, Model model, RedirectAttributes redirect) {
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) {
            return volverAHabilidades(redirect, "mensajeError", "No se encontró tu perfil de colaborador.");
        }

        BigDecimal anios = validarAniosExperiencia(formulario.getAniosExperiencia(), errores);
        if (!errores.hasErrors()) {
            Integer idColaborador = colaborador.getIdColaborador();
            Integer idHabilidad = formulario.getIdHabilidad();
            String error;
            try {
                error = transacciones.execute(estado -> agregarHabilidadAlPerfil(idColaborador, formulario, anios));
            } catch (DataIntegrityViolationException ex) {
                error = colaboradorRepository.contarHabilidad(idColaborador, idHabilidad) > 0
                        ? HABILIDAD_DUPLICADA : "validation.habilidadPerfil.id.noDisponible";
            }
            if (error == null) {
                return volverAHabilidades(redirect, "mensajeExito", "Habilidad agregada correctamente.");
            }
            errores.rejectValue("idHabilidad", error);
        }
        return vistaHabilidades(model, colaborador, null, null, redirect);
    }

    private String agregarHabilidadAlPerfil(Integer idColaborador, HabilidadPerfilForm formulario, BigDecimal anios) {
        Integer idHabilidad = formulario.getIdHabilidad();
        if (!habilidadRepository.buscarParaActualizar(idHabilidad).map(Habilidad::getEstado).orElse(false)) {
            return "validation.habilidadPerfil.id.noDisponible";
        }
        if (colaboradorRepository.contarHabilidad(idColaborador, idHabilidad) > 0) {
            return HABILIDAD_DUPLICADA;
        }
        colaboradorRepository.guardarHabilidad(idColaborador, idHabilidad, formulario.getNivel().name(), anios);
        return null;
    }

    @PostMapping("/habilidades/actualizar")
    public String actualizarHabilidad(@Valid @ModelAttribute("habilidadForm") HabilidadPerfilForm formulario,
                                      BindingResult errores, Model model, RedirectAttributes redirect) {
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) {
            return volverAHabilidades(redirect, "mensajeError", "No se encontró tu perfil de colaborador.");
        }
        if (errores.hasFieldErrors("idHabilidad")) {
            return volverAHabilidades(redirect, "mensajeError", "Selecciona una habilidad de tu perfil para editarla.");
        }

        Integer idColaborador = colaborador.getIdColaborador();
        Integer idHabilidad = formulario.getIdHabilidad();
        BigDecimal anios = validarAniosExperiencia(formulario.getAniosExperiencia(), errores);
        if (errores.hasErrors()) {
            return vistaHabilidades(model, colaborador, idHabilidad, null, redirect);
        }

        int filas = colaboradorRepository.actualizarHabilidad(idColaborador, idHabilidad, formulario.getNivel().name(), anios);
        if (filas == 0 && colaboradorRepository.contarHabilidad(idColaborador, idHabilidad) == 0) {
            return volverAHabilidades(redirect, "mensajeError", "La habilidad ya no está en tu perfil.");
        }
        return volverAHabilidades(redirect, "mensajeExito", "Habilidad actualizada correctamente.");
    }

    @PostMapping("/habilidades/{idHabilidad}/eliminar")
    public String eliminarHabilidad(@PathVariable Integer idHabilidad, RedirectAttributes redirect) {
        Colaborador colaborador = colaboradorSesion();
        if (colaborador == null) {
            return volverAHabilidades(redirect, "mensajeError", "No se encontró tu perfil de colaborador.");
        }

        int retiradas = colaboradorRepository.eliminarHabilidad(colaborador.getIdColaborador(), idHabilidad);
        if (retiradas == 0) {
            return volverAHabilidades(redirect, "mensajeError", "La habilidad ya no está en tu perfil.");
        }
        return volverAHabilidades(redirect, "mensajeExito", "Habilidad retirada del perfil.");
    }

    private String volverAHabilidades(RedirectAttributes redirect, String atributo, String mensaje) {
        redirect.addFlashAttribute(atributo, mensaje);
        return "redirect:/colaborador/habilidades";
    }

    /** Agrupa las participaciones del colaborador por proyecto y pagina las tarjetas. */
    @GetMapping("/proyectos")
    public String proyectos(@RequestParam(defaultValue = "1") int page,
                            @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        List<ProyectoColaborador> proyectos = proyectosDelColaborador(hoy);
        int total = proyectos.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) PROYECTOS_POR_PAGINA));
        int paginaActual = Math.max(1, Math.min(page, totalPaginas));
        int desde = (paginaActual - 1) * PROYECTOS_POR_PAGINA;
        int hasta = Math.min(desde + PROYECTOS_POR_PAGINA, total);

        Map<Boolean, List<ProyectoColaborador>> pagina = proyectos.subList(desde, hasta).stream()
                .collect(Collectors.partitioningBy(ProyectoColaborador::vigente));
        Map<String, List<ProyectoColaborador>> grupos = new LinkedHashMap<>();
        grupos.put("Vigentes", pagina.get(true));
        grupos.put("Próximos", pagina.get(false));

        model.addAttribute("titulo", "Mis proyectos");
        model.addAttribute("gruposProyectos", grupos);
        model.addAttribute("totalProyectos", total);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("primerProyecto", desde + 1);
        model.addAttribute("ultimoProyecto", hasta);
        model.addAttribute("puedeVerHistorial", !bloqueados.contains("historial"));
        return "colaborador/mis-proyectos";
    }

    private List<ProyectoColaborador> proyectosDelColaborador(LocalDate hoy) {
        Map<Integer, List<Asignacion>> asignacionesPorProyecto = participaciones().stream()
                .filter(a -> a.getEstado() == Asignacion.EstadoAsignacion.ACTIVA
                        || a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA)
                .filter(a -> !a.esHistorica(hoy))
                .collect(Collectors.groupingBy(a -> a.getProyecto().getIdProyecto()));

        return asignacionesPorProyecto.values().stream()
                .map(asignaciones -> prepararProyectoColaborador(asignaciones, hoy))
                .sorted(Comparator.comparing(ProyectoColaborador::vigente).reversed()
                        .thenComparing(ProyectoColaborador::primerInicio)
                        .thenComparing(p -> p.proyecto().getIdProyecto()))
                .toList();
    }

    private ProyectoColaborador prepararProyectoColaborador(List<Asignacion> asignaciones, LocalDate hoy) {
        List<ParticipacionProyecto> participaciones = asignaciones.stream()
                .map(a -> {
                    boolean vigente = a.estaVigente(hoy);
                    boolean pendiente = a.getFechaInicio().isBefore(hoy)
                            && a.getEstado() == Asignacion.EstadoAsignacion.PLANIFICADA;
                    return new ParticipacionProyecto(a, vigente, pendiente);
                })
                .sorted(Comparator.comparing(ParticipacionProyecto::vigente).reversed()
                        .thenComparing(p -> p.asignacion().getFechaInicio())
                        .thenComparing(p -> p.asignacion().getIdAsignacion()))
                .toList();
        return new ProyectoColaborador(asignaciones.get(0).getProyecto(), participaciones);
    }

    public record ProyectoColaborador(Proyecto proyecto, List<ParticipacionProyecto> participaciones) {
        public boolean vigente() {
            return participaciones.get(0).vigente();
        }

        public LocalDate primerInicio() {
            return participaciones.get(0).asignacion().getFechaInicio();
        }
    }

    public record ParticipacionProyecto(Asignacion asignacion, boolean vigente, boolean pendiente) {
        public String etiqueta() {
            if (vigente) return "Asignación vigente";
            return pendiente ? "Pendiente de iniciar" : "Asignación próxima";
        }
    }

    @GetMapping("/historial")
    public String historial(@RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        List<FilaAsignacionDTO> filas = participaciones().stream()
                .filter(a -> a.esHistorica(hoy))
                .map(a -> FilaAsignacionDTO.desde(a, hoy))
                .sorted(Comparator.comparing(FilaAsignacionDTO::fechaOrden, Comparator.reverseOrder())
                        .thenComparing(fila -> fila.asignacion().getIdAsignacion(), Comparator.reverseOrder()))
                .toList();
        model.addAttribute("titulo", "Historial de proyectos");
        model.addAttribute("filas", filas);
        model.addAttribute("puedeVerProyectos", !bloqueados.contains("proyectos"));
        return "colaborador/historial";
    }

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(value = "q", defaultValue = "") String q,
                               @RequestParam(value = "estado", defaultValue = "TODOS") String estado,
                               @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        String busqueda = q.strip();
        String filtro = estado.strip().toUpperCase(Locale.ROOT);
        if ("ACTIVA".equals(filtro)) filtro = "VIGENTE";
        boolean puedeVerHistorial = !bloqueados.contains("historial");
        List<EstadoParticipacion> estados = List.of(EstadoParticipacion.values()).stream()
                .filter(e -> puedeVerHistorial || !e.historica())
                .toList();
        EstadoParticipacion seleccionado = null;
        if (!"TODOS".equals(filtro)) {
            String codigo = filtro;
            seleccionado = estados.stream().filter(e -> e.name().equals(codigo)).findFirst().orElse(null);
            if (seleccionado == null) model.addAttribute("errorEstado", "Selecciona un estado de la lista.");
        }
        if (busqueda.length() > 150) model.addAttribute("errorBusqueda", "La búsqueda admite hasta 150 caracteres.");

        List<FilaAsignacionDTO> filas = List.of();
        if (!model.containsAttribute("errorEstado") && !model.containsAttribute("errorBusqueda")) {
            String texto = busqueda.toLowerCase(Locale.ROOT);
            EstadoParticipacion estadoElegido = seleccionado;
            LocalDate hoy = LocalDate.now(ZONA_HORARIA);
            filas = participaciones().stream()
                    .filter(a -> puedeVerHistorial || !a.esHistorica(hoy))
                    .filter(a -> a.getProyecto().getNombre().toLowerCase(Locale.ROOT).contains(texto)
                            || (a.getRolProyecto() != null && a.getRolProyecto().toLowerCase(Locale.ROOT).contains(texto)))
                    .map(a -> FilaAsignacionDTO.desde(a, hoy))
                    .filter(fila -> estadoElegido == null || fila.estado() == estadoElegido)
                    .toList();
        }
        model.addAttribute("titulo", "Mis asignaciones");
        model.addAttribute("filas", filas);
        model.addAttribute("q", busqueda);
        model.addAttribute("estados", estados);
        model.addAttribute("estadoSeleccionado", seleccionado);
        return "colaborador/asignaciones";
    }

    private List<Asignacion> participaciones() {
        Colaborador colaborador = colaboradorSesion();
        return colaborador == null ? List.of() : asignacionRepository
                .findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador());
    }



}
