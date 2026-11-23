package pe.edu.pucp.skillbridge.controller;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
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
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
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
import java.util.ArrayList;
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
    private final ForoRepository foroRepository;
    private final NotificacionRepository notificacionRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final RespuestaForoRepository respuestaForoRepository;
    private final CapacidadColaboradorService capacidadColaboradorService;
    private final FotoPerfilService fotoPerfilService;
    private final TransactionTemplate transacciones;
    private final EntityManager entidades;

    @Value("${skillbridge.upload-dir:uploads}")
    private String uploadDir;

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
     * @param foroRepository repositorio de foros
     * @param notificacionRepository repositorio de notificaciones
     * @param mensajeChatRepository repositorio de mensajes de chat
     * @param respuestaForoRepository repositorio de respuestas de foro
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
                                 ForoRepository foroRepository,
                                 NotificacionRepository notificacionRepository,
                                 MensajeChatRepository mensajeChatRepository,
                                 RespuestaForoRepository respuestaForoRepository,
                                 CapacidadColaboradorService capacidadColaboradorService,
                                 FotoPerfilService fotoPerfilService,
                                 TransactionTemplate transacciones,
                                 EntityManager entidades) {
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.habilidadRepository = habilidadRepository;
        this.certificacionRepository = certificacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.foroRepository = foroRepository;
        this.notificacionRepository = notificacionRepository;
        this.mensajeChatRepository = mensajeChatRepository;
        this.respuestaForoRepository = respuestaForoRepository;
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
            model.addAttribute("mensajeError", "Revisa los campos marcados.");
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
        Colaborador c = colaboradorSesion();
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
    @Transactional
    public String guardarHabilidad(@RequestParam("idHabilidad") Integer idHabilidad,
                                   @RequestParam("nivel") String nivel,
                                   @RequestParam("aniosExperiencia") BigDecimal aniosExperiencia,
                                   Model model) {
        Colaborador c = colaboradorSesion();
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
        if (!habilidadRepository.buscarParaActualizar(idHabilidad).map(Habilidad::getEstado).orElse(false)) {
            cargarHabilidades(model, null);
            model.addAttribute("mensajeError", "La habilidad seleccionada ya no está disponible. Si necesitas agregarla, contacta al administrador.");
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
        Colaborador c = colaboradorSesion();
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
        colaboradorRepository.actualizarHabilidad(c.getIdColaborador(), idHabilidad, nivel, aniosExperiencia);
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
        Colaborador c = colaboradorSesion();
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
    public String proyectos(@RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        model.addAttribute("titulo", "Mis proyectos");
        model.addAttribute("asignaciones", participaciones().stream()
                .filter(a -> !bloqueados.contains("historial") || !esHistorica(a)).toList());
        return "colaborador/proyectos";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("titulo", "Historial de proyectos");
        model.addAttribute("esHistorial", true);
        model.addAttribute("asignaciones", participaciones().stream().filter(this::esHistorica).toList());
        return "colaborador/proyectos";
    }

    private List<Asignacion> participaciones() {
        Colaborador colaborador = colaboradorSesion();
        return colaborador == null ? List.of() : asignacionRepository
                .findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador());
    }

    private boolean esHistorica(Asignacion asignacion) {
        Proyecto.EstadoProyecto estado = asignacion.getProyecto().getEstado();
        return estado == Proyecto.EstadoProyecto.COMPLETED || estado == Proyecto.EstadoProyecto.CANCELLED
                || asignacion.getEstado() == Asignacion.EstadoAsignacion.FINALIZADA
                || asignacion.getEstado() == Asignacion.EstadoAsignacion.CANCELADA;
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
                               @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        List<Asignacion> lista = participaciones().stream()
                .filter(a -> !bloqueados.contains("historial") || !esHistorica(a))
                .filter(a -> {
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
        Colaborador c = colaboradorSesion();
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
        Colaborador c = colaboradorSesion();
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
        Colaborador c = colaboradorSesion();
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
        cargarChat(model, colaboradorSesion(), idProyecto);
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
        Colaborador colaborador = colaboradorSesion();
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
