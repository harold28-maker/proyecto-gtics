package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.repository.CategoriaHabilidadRepository.ResumenCategoria;
import pe.edu.pucp.skillbridge.repository.HabilidadRepository.UsoHabilidad;
import pe.edu.pucp.skillbridge.service.CorreoCuentaService;
import pe.edu.pucp.skillbridge.service.FotoPerfilService;
import pe.edu.pucp.skillbridge.service.ReporteAdministrativoService;
import pe.edu.pucp.skillbridge.util.PoliticaContrasena;
import pe.edu.pucp.skillbridge.util.ReportePdf;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * Controlador encargado de gestionar las páginas y operaciones administrativas
 * relacionadas con usuarios, roles y permisos.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {


    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final HabilidadRepository habilidadRepository;
    private final CategoriaHabilidadRepository categoriaHabilidadRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ReporteAdministrativoService reporteAdministrativoService;

    private final CorreoCuentaService correoCuentaService;
    private final FotoPerfilService fotoPerfilService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transacciones;

    private record CuentaGuardada(Usuario usuario, String fotoAnterior) {}


    /**
     * Construye el controlador con los repositorios utilizados para consultar
     * y modificar los datos administrados.
     *
     * @param usuarioRepository repositorio de usuarios
     * @param rolRepository repositorio de roles
     * @param colaboradorRepository repositorio de colaboradores
     * @param habilidadRepository repositorio de habilidades
     * @param categoriaHabilidadRepository repositorio del catálogo de categorías
     * @param auditoriaRepository repositorio de registros de auditoría
     * @param reporteAdministrativoService servicio de reportes y exportación a Excel
     * @param correoCuentaService servicio de envío del correo de bienvenida
     * @param fotoPerfilService servicio de fotos de usuario
     * @param passwordEncoder codificador de contraseñas utilizado por la aplicación
     * @param transacciones ejecutor de las transacciones de guardado
     */
    public AdminController(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            ColaboradorRepository colaboradorRepository,
            HabilidadRepository habilidadRepository,
            CategoriaHabilidadRepository categoriaHabilidadRepository,
            AuditoriaRepository auditoriaRepository,
            ReporteAdministrativoService reporteAdministrativoService,
            CorreoCuentaService correoCuentaService,
            FotoPerfilService fotoPerfilService,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transacciones
    ) {

        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.habilidadRepository = habilidadRepository;
        this.categoriaHabilidadRepository = categoriaHabilidadRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.reporteAdministrativoService = reporteAdministrativoService;
        this.correoCuentaService = correoCuentaService;
        this.fotoPerfilService = fotoPerfilService;
        this.passwordEncoder = passwordEncoder;
        this.transacciones = transacciones;
    }


    /* =========================================================
       INICIO ADMINISTRADOR
       ========================================================= */

    /**
     * Muestra el panel administrativo con indicadores generales y los registros
     * recientes de usuarios y auditorías.
     *
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan los datos mostrados en el panel
     * @return la vista del panel o una redirección al inicio de sesión
     */
    @RequestMapping(
            value = {"/inicio", "/dashboard"},
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST
            }
    )
    public String inicio(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        List<Usuario> usuarios =
                usuarioRepository.findAll();


        model.addAttribute(
                "titulo",
                "Inicio"
        );


        model.addAttribute(
                "totalUsuarios",
                usuarios.size()
        );


        model.addAttribute(
                "usuariosActivos",
                usuarios.stream()
                        .filter(
                                u ->
                                        u.getEstado()
                                                == Usuario.EstadoUsuario.ACTIVO
                        )
                        .count()
        );


        model.addAttribute(
                "totalRoles",
                rolRepository.count()
        );


        model.addAttribute(
                "totalHabilidades",
                habilidadRepository.count()
        );


        model.addAttribute(
                "auditorias",
                auditoriaRepository
                        .findTop50ByOrderByFechaDesc()
                        .stream()
                        .limit(6)
                        .toList()
        );


        model.addAttribute(
                "ultimosUsuarios",
                usuarios.stream()
                        .sorted(Comparator
                                .comparing(Usuario::getFechaRegistro, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(Usuario::getIdUsuario, Comparator.reverseOrder()))
                        .limit(5)
                        .toList()
        );


        return "admin/inicio";
    }


    /* =========================================================
       USUARIOS
       ========================================================= */

    /**
     * Muestra los usuarios y los indicadores de los resultados de búsqueda.
     */
    @GetMapping("/usuarios")
    public String usuarios(
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "rol", required = false) Integer rol,
            @RequestParam(value = "estado", required = false) Usuario.EstadoUsuario estado,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        String texto = normalizar(q);
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
                .filter(u -> rol == null || (u.getRol() != null && rol.equals(u.getRol().getIdRol())))
                .filter(u -> estado == null || u.getEstado() == estado)
                .filter(u -> texto.isEmpty()
                        || normalizar(u.getNombreCompleto()).contains(texto)
                        || normalizar(u.getCorreo()).contains(texto)
                        || normalizar(u.getRolVisual()).contains(texto)
                        || (u.getRol() != null && normalizar(u.getRol().getNombre()).contains(texto)))
                .toList();

        model.addAttribute("titulo", "Usuarios");
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("rolesFiltro", rolRepository.findAll());
        model.addAttribute("q", q);
        model.addAttribute("rolFiltro", rol);
        model.addAttribute("estadoFiltro", estado == null ? null : estado.name());
        model.addAttribute("hayFiltros", !texto.isEmpty() || rol != null || estado != null);
        model.addAttribute("activos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.ACTIVO).count());
        model.addAttribute("inactivos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.INACTIVO).count());
        model.addAttribute("bloqueados", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.BLOQUEADO).count());
        return "admin/usuarios";
    }


    /* =========================================================
       NUEVO USUARIO
       ========================================================= */

    /**
     * Prepara el formulario para registrar un usuario nuevo con estado activo.
     *
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega el usuario nuevo y los roles disponibles
     * @return la vista del formulario o una redirección al inicio de sesión
     */
    @GetMapping("/usuarios/nuevo")
    public String nuevoUsuario(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        Usuario usuario =
                new Usuario();


        usuario.setEstado(
                Usuario.EstadoUsuario.ACTIVO
        );


        model.addAttribute(
                "titulo",
                "Nuevo usuario"
        );


        model.addAttribute(
                "usuario",
                usuario
        );


        model.addAttribute(
                "roles",
                rolRepository.findAll()
        );


        return "admin/usuario-form";
    }


    /* =========================================================
       EDITAR USUARIO
       ========================================================= */

    /**
     * Carga los datos de un usuario existente para su modificación.
     *
     * @param idTexto identificador textual del usuario recibido en la ruta
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan el usuario encontrado y los roles disponibles
     * @return la vista del formulario o una redirección a la lista de usuarios
     */
    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuario(
            @PathVariable("id") String idTexto,
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        Integer id;

        try {
            id = Integer.valueOf(idTexto);
        } catch (Exception e) {
            return "redirect:/admin/usuarios";
        }


        Optional<Usuario> encontrado =
                usuarioRepository.findById(id);


        if (encontrado.isEmpty()) {
            return "redirect:/admin/usuarios";
        }


        model.addAttribute(
                "titulo",
                "Editar usuario"
        );


        model.addAttribute(
                "usuario",
                encontrado.get()
        );


        model.addAttribute(
                "roles",
                rolRepository.findAll()
        );


        return "admin/usuario-form";
    }


    @GetMapping("/usuarios/fotos/{nombre}")
    public ResponseEntity<Resource> fotoUsuario(@PathVariable("nombre") String nombre, HttpSession session) {
        if (!esAdmin(session)) {
            return ResponseEntity.status(403).build();
        }
        Resource foto = fotoPerfilService.buscar(nombre);
        if (foto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .body(foto);
    }


    /* =========================================================
       GUARDAR USUARIO
       ========================================================= */

    /**
     * Valida y registra un usuario nuevo o actualiza los datos de un usuario existente.
     *
     * @param idUsuarioTexto identificador textual del usuario a editar, si corresponde
     * @param nombres nombres ingresados en el formulario
     * @param apellidos apellidos ingresados en el formulario
     * @param correo correo electrónico ingresado en el formulario
     * @param telefono número telefónico ingresado en el formulario
     * @param idRolTexto identificador textual del rol seleccionado
     * @param estadoTexto estado textual seleccionado para el usuario
     * @param password contraseña nueva ingresada en el formulario
     * @param confirmarPassword confirmación de la contraseña ingresada
     * @param foto imagen original seleccionada al editar
     * @param fotoRecortada recorte enviado desde la vista previa
     * @param quitarFoto indica si se debe retirar la foto actual
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param administrador cuenta autenticada que realiza la operación
     * @param model modelo con los datos de validación
     * @param redirect atributos de los avisos del listado
     * @return el formulario en caso de error o una redirección al listado
     */
    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(

            @RequestParam(
                    value = "idUsuario",
                    required = false
            )
            String idUsuarioTexto,

            @RequestParam(
                    value = "nombres",
                    required = false
            )
            String nombres,

            @RequestParam(
                    value = "apellidos",
                    required = false
            )
            String apellidos,

            @RequestParam(
                    value = "correo",
                    required = false
            )
            String correo,

            @RequestParam(
                    value = "telefono",
                    required = false
            )
            String telefono,

            @RequestParam(
                    value = "idRol",
                    required = false
            )
            String idRolTexto,

            @RequestParam(
                    value = "estado",
                    required = false
            )
            String estadoTexto,

            @RequestParam(
                    value = "password",
                    required = false
            )
            String password,

            @RequestParam(
                    value = "confirmarPassword",
                    required = false
            )
            String confirmarPassword,

            @RequestParam(value = "foto", required = false) MultipartFile foto,
            @RequestParam(value = "fotoRecortada", required = false) MultipartFile fotoRecortada,
            @RequestParam(value = "quitarFoto", defaultValue = "false") boolean quitarFoto,

            HttpSession session,
            @RequestAttribute("usuarioVerificado") Usuario administrador,
            Model model,
            RedirectAttributes redirect
    ) {

        /* =====================================================
           SEGURIDAD DE LA RUTA
           ===================================================== */

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        /* =====================================================
           ID DEL USUARIO
           ===================================================== */

        Integer idUsuario = null;

        if (
                idUsuarioTexto != null
                        &&
                        !idUsuarioTexto.isBlank()
        ) {

            try {

                idUsuario =
                        Integer.valueOf(
                                idUsuarioTexto
                        );

            } catch (NumberFormatException e) {

                Usuario formulario =
                        construirUsuarioFormulario(
                                null,
                                nombres,
                                apellidos,
                                correo,
                                telefono,
                                null,
                                Usuario.EstadoUsuario.ACTIVO
                        );

                return volverFormularioUsuario(
                        model,
                        formulario,
                        "El identificador del usuario no es válido."
                );
            }
        }


        /* =====================================================
           ROL
           ===================================================== */

        Integer idRol;

        try {

            idRol =
                    Integer.valueOf(
                            idRolTexto
                    );

        } catch (Exception e) {

            Usuario formulario =
                    construirUsuarioFormulario(
                            idUsuario,
                            nombres,
                            apellidos,
                            correo,
                            telefono,
                            null,
                            Usuario.EstadoUsuario.ACTIVO
                    );

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "El rol seleccionado no es válido."
            );
        }


        Rol rol =
                rolRepository
                        .findById(idRol)
                        .orElse(null);


        if (rol == null) {

            Usuario formulario =
                    construirUsuarioFormulario(
                            idUsuario,
                            nombres,
                            apellidos,
                            correo,
                            telefono,
                            null,
                            Usuario.EstadoUsuario.ACTIVO
                    );

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "El rol seleccionado no existe."
            );
        }


        /* =====================================================
           ESTADO
           ===================================================== */

        Usuario.EstadoUsuario estado;

        try {

            estado =
                    Usuario.EstadoUsuario.valueOf(
                            estadoTexto
                    );

        } catch (Exception e) {

            Usuario formulario =
                    construirUsuarioFormulario(
                            idUsuario,
                            nombres,
                            apellidos,
                            correo,
                            telefono,
                            rol,
                            Usuario.EstadoUsuario.ACTIVO
                    );

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "El estado seleccionado no es válido."
            );
        }


        Usuario formulario =
                construirUsuarioFormulario(
                        idUsuario,
                        nombres,
                        apellidos,
                        correo,
                        telefono,
                        rol,
                        estado
                );


        /* =====================================================
           LIMPIEZA DE DATOS
           ===================================================== */

        String nombresLimpios =
                nombres == null
                        ? ""
                        : nombres.trim();

        String apellidosLimpios =
                apellidos == null
                        ? ""
                        : apellidos.trim();

        String correoLimpio =
                correo == null
                        ? ""
                        : correo.trim().toLowerCase(Locale.ROOT);

        String telefonoLimpio =
                telefono == null
                        ? ""
                        : telefono.trim();


        /* =====================================================
           VALIDACIONES BACKEND
           No se confía en maxlength, required o pattern del HTML.
           ===================================================== */

        if (
                !nombresLimpios.matches(
                        "[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]{1,20}"
                )
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "Los nombres son obligatorios, solo pueden contener letras y deben tener máximo 20 caracteres."
            );
        }


        if (
                !apellidosLimpios.matches(
                        "[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]{1,20}"
                )
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "Los apellidos son obligatorios, solo pueden contener letras y deben tener máximo 20 caracteres."
            );
        }


        if (
                correoLimpio.isBlank()
                        ||
                        correoLimpio.length() > 150
                        ||
                        !correoLimpio.matches(
                                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                        )
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "Ingresa un correo válido."
            );
        }


        if (
                telefonoLimpio.isBlank()
                        ||
                        !telefonoLimpio.matches(
                                "[0-9]{9}"
                        )
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "El teléfono es obligatorio y debe tener exactamente 9 números."
            );
        }


        /* =====================================================
           CORREO ÚNICO
           ===================================================== */

        Optional<Usuario> usuarioConCorreo =
                usuarioRepository
                        .findByCorreoIgnoreCase(
                                correoLimpio
                        );


        if (
                usuarioConCorreo.isPresent()
                        &&
                        (
                                idUsuario == null
                                        ||
                                        !usuarioConCorreo
                                                .get()
                                                .getIdUsuario()
                                                .equals(idUsuario)
                        )
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "El correo ya se encuentra registrado."
            );
        }


        boolean nuevo = idUsuario == null;
        String clave = password == null ? "" : password;
        String confirmacion = confirmarPassword == null ? "" : confirmarPassword;

        if (clave.isBlank()) {
            if (nuevo) {
                return volverFormularioUsuario(model, formulario, "La contraseña es obligatoria para un usuario nuevo.");
            }
            if (!confirmacion.isBlank()) {
                model.addAttribute("abrirContrasena", true);
                return volverFormularioUsuario(model, formulario, "Ingresa la nueva contraseña antes de confirmarla.");
            }
        } else {
            String errorClave = PoliticaContrasena.validar(clave, confirmacion);
            if (errorClave != null) {
                model.addAttribute("abrirContrasena", true);
                return volverFormularioUsuario(model, formulario, errorClave);
            }
        }

        boolean cambiarFoto = foto != null && (!foto.isEmpty() || StringUtils.hasText(foto.getOriginalFilename()));
        boolean tieneRecorte = fotoRecortada != null && !fotoRecortada.isEmpty();
        if (nuevo && (cambiarFoto || quitarFoto || tieneRecorte)) {
            return volverFormularioUsuario(model, formulario, "Puedes agregar una foto al editar la cuenta.");
        }
        if ((cambiarFoto && quitarFoto) || (tieneRecorte && !cambiarFoto)) {
            return volverFormularioUsuario(model, formulario, "Selecciona una foto para cambiarla o elige quitar la actual.");
        }

        String claveHash = clave.isBlank() ? null : passwordEncoder.encode(clave);
        String fotoNueva;
        try {
            fotoNueva = cambiarFoto ? fotoPerfilService.guardar(foto, fotoRecortada) : null;
        } catch (IllegalArgumentException ex) {
            return volverFormularioUsuario(model, formulario, ex.getMessage());
        } catch (IOException ex) {
            return volverFormularioUsuario(model, formulario, "No se pudo guardar la foto. Vuelve a intentarlo.");
        }

        Integer usuarioId = idUsuario;
        CuentaGuardada resultado = null;
        try {
            resultado = transacciones.execute(transaccion -> {
                Usuario usuario = nuevo ? new Usuario() : usuarioRepository.buscarParaActualizar(usuarioId).orElse(null);
                if (usuario == null) {
                    return null;
                }
                usuario.setNombres(nombresLimpios);
                usuario.setApellidos(apellidosLimpios);
                usuario.setCorreo(correoLimpio);
                usuario.setTelefono(telefonoLimpio);
                usuario.setEstado(estado);
                usuario.setRol(rol);
                if (claveHash != null) {
                    usuario.setPasswordHash(claveHash);
                }

                String fotoAnterior = usuario.getFotoUrl();
                if (cambiarFoto || quitarFoto) {
                    usuario.setFotoUrl(fotoNueva);
                }

                Usuario cuenta = usuarioRepository.save(usuario);
                if (!"ADMINISTRADOR".equals(rol.getNombre())
                        && colaboradorRepository.findByUsuarioIdUsuario(cuenta.getIdUsuario()).isEmpty()) {
                    Colaborador colaborador = new Colaborador();
                    colaborador.setUsuario(cuenta);
                    colaborador.setCargo("Por definir");
                    colaborador.setArea("Por definir");
                    colaborador.setDisponibilidadBase(100);
                    colaboradorRepository.save(colaborador);
                }

                registrarAuditoria(administrador, nuevo ? "Crear usuario" : "Actualizar usuario", "Administración",
                        (nuevo ? "Se registró la cuenta de " : "Se actualizó la cuenta de ") + cuenta.getNombreCompleto());
                return new CuentaGuardada(cuenta, fotoAnterior);
            });
        } catch (DataIntegrityViolationException ex) {
            return volverFormularioUsuario(model, formulario, "No se pudo guardar el usuario. Verifica el correo y los datos ingresados.");
        } finally {
            if (resultado == null) {
                fotoPerfilService.eliminar(fotoNueva);
            }
        }

        if (resultado == null) {
            return volverFormularioUsuario(model, formulario, "El usuario que intentas editar no existe.");
        }
        if (cambiarFoto || quitarFoto) {
            fotoPerfilService.eliminar(resultado.fotoAnterior());
        }
        Usuario guardado = resultado.usuario();

        String mensaje = nuevo ? "Usuario registrado correctamente." : "Usuario actualizado correctamente.";
        if (nuevo) {
            var envio = correoCuentaService.enviarBienvenida(
                    guardado.getCorreo(), guardado.getNombreCompleto(), guardado.getRolVisual(), clave);
            switch (envio) {
                case ACEPTADO -> mensaje = "Usuario registrado y correo de bienvenida enviado al proveedor.";
                case SIN_CONFIGURACION -> redirect.addFlashAttribute("avisoCorreo",
                        "Falta configurar el servidor SMTP y el remitente de SkillBridge AI para enviar la bienvenida.");
                case ERROR_AUTENTICACION -> redirect.addFlashAttribute("avisoCorreo",
                        "El proveedor de correo rechazó la autenticación. Revisa las credenciales SMTP.");
                case ERROR_ENVIO -> redirect.addFlashAttribute("avisoCorreo",
                        "No se pudo enviar el correo de bienvenida. Cuando el envío esté disponible, el usuario puede solicitar un enlace desde el login.");
            }
        }
        redirect.addFlashAttribute("mensajeExito", mensaje);

        return "redirect:/admin/usuarios";
    }


    /**
     * Construye un objeto de usuario con los datos ingresados en el formulario
     * para conservarlos mientras se muestra un mensaje de validación.
     *
     * @param idUsuario identificador del usuario al editar
     * @param nombres nombres ingresados
     * @param apellidos apellidos ingresados
     * @param correo correo electrónico ingresado
     * @param telefono número telefónico ingresado
     * @param rol rol seleccionado
     * @param estado estado seleccionado
     * @return usuario construido con los datos del formulario
     */
    private Usuario construirUsuarioFormulario(
            Integer idUsuario,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            Rol rol,
            Usuario.EstadoUsuario estado
    ) {

        Usuario usuario =
                new Usuario();


        usuario.setIdUsuario(
                idUsuario
        );


        usuario.setNombres(
                nombres
        );


        usuario.setApellidos(
                apellidos
        );


        usuario.setCorreo(
                correo
        );


        usuario.setTelefono(
                telefono
        );


        usuario.setRol(
                rol
        );


        usuario.setEstado(
                estado
        );


        return usuario;
    }


    /**
     * Restablece la vista del formulario de usuario con los datos conservados
     * y el mensaje de error indicado.
     *
     * @param model modelo al que se agregan el título, usuario, roles y error
     * @param usuario datos que se muestran nuevamente en el formulario
     * @param error descripción del problema detectado
     * @return la vista del formulario de usuario
     */
    private String volverFormularioUsuario(
            Model model,
            Usuario usuario,
            String error
    ) {
        if (usuario.getIdUsuario() != null) {
            usuarioRepository.findById(usuario.getIdUsuario())
                    .ifPresent(cuenta -> usuario.setFotoUrl(cuenta.getFotoUrl()));
        }

        model.addAttribute(
                "titulo",
                usuario.getIdUsuario() == null
                        ? "Nuevo usuario"
                        : "Editar usuario"
        );


        model.addAttribute(
                "usuario",
                usuario
        );


        model.addAttribute(
                "roles",
                rolRepository.findAll()
        );


        model.addAttribute(
                "error",
                error
        );


        return "admin/usuario-form";
    }



    /* =========================================================
       CAMBIAR ESTADO USUARIO
       ========================================================= */

    // Baja lógica:
    // evita borrar físicamente registros relacionados.

    /**
     * Actualiza el estado elegido y vuelve al listado con los filtros activos.
     */
    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstadoUsuario(
            @PathVariable("id") String idTexto,
            @RequestParam("activar") boolean activar,
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "rol", required = false) Integer rol,
            @RequestParam(value = "estado", required = false) Usuario.EstadoUsuario estado,
            HttpSession session,
            @RequestAttribute("usuarioVerificado") Usuario administrador,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        if (!q.isBlank()) {
            redirect.addAttribute("q", q);
        }
        if (rol != null) {
            redirect.addAttribute("rol", rol);
        }
        if (estado != null) {
            redirect.addAttribute("estado", estado.name());
        }

        Integer id;
        try {
            id = Integer.valueOf(idTexto);
        } catch (NumberFormatException e) {
            return "redirect:/admin/usuarios";
        }

        Usuario.EstadoUsuario nuevoEstado = activar
                ? Usuario.EstadoUsuario.ACTIVO
                : Usuario.EstadoUsuario.INACTIVO;
        Boolean encontrado = transacciones.execute(transaccion -> {
            Usuario usuario = usuarioRepository.buscarParaActualizar(id).orElse(null);
            if (usuario == null) {
                return false;
            }
            if (usuario.getEstado() != nuevoEstado) {
                usuario.setEstado(nuevoEstado);
                registrarAuditoria(
                        administrador,
                        "Cambiar estado de usuario",
                        "Administración",
                        usuario.getNombreCompleto() + " quedó en estado " + nuevoEstado
                );
            }
            return true;
        });

        if (Boolean.TRUE.equals(encontrado)) {
            redirect.addFlashAttribute("mensajeExito", "Estado del usuario actualizado.");
        }
        return "redirect:/admin/usuarios";
    }


    /* =========================================================
       HABILIDADES
       ========================================================= */

    /**
     * Filtros que se conservan al volver al catálogo después de una operación.
     *
     * @param q texto buscado en el nombre
     * @param categoriaFiltro categoría seleccionada
     * @param estadoFiltro estado seleccionado, o {@code null} para mostrar todos
     */
    public record FiltrosHabilidades(String q, String categoriaFiltro, Boolean estadoFiltro) {
        public FiltrosHabilidades {
            q = q == null ? "" : q.strip();
            categoriaFiltro = categoriaFiltro == null ? "" : categoriaFiltro;
        }
    }

    /**
     * Muestra el catálogo, el uso de las habilidades y la confirmación de desactivación cuando se solicita.
     */
    @GetMapping("/habilidades")
    public String habilidades(
            @ModelAttribute("filtros") FiltrosHabilidades filtros,
            @RequestParam(value = "desactivar", required = false) Integer desactivar,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Habilidad> catalogo = habilidadRepository.findAll();
        List<CategoriaHabilidad> categorias = categoriaHabilidadRepository.findAllByOrderByNombreAsc();
        List<Habilidad> buscadas = filtros.q().isEmpty()
                ? catalogo : habilidadRepository.findByNombreContainingIgnoreCase(filtros.q());
        List<Habilidad> habilidades = buscadas.stream()
                .filter(h -> filtros.categoriaFiltro().isEmpty()
                        || filtros.categoriaFiltro().equals(h.getCategoria()))
                .filter(h -> filtros.estadoFiltro() == null
                        || filtros.estadoFiltro().equals(Boolean.TRUE.equals(h.getEstado())))
                .toList();
        Map<Integer, UsoHabilidad> usos = habilidades.isEmpty()
                ? Map.of()
                : habilidadRepository.obtenerUsos(habilidades.stream().map(Habilidad::getIdHabilidad).toList())
                        .stream().collect(Collectors.toMap(UsoHabilidad::getIdHabilidad, Function.identity()));

        model.addAttribute("titulo", "Catálogo de habilidades");
        model.addAttribute("habilidades", habilidades);
        model.addAttribute("usos", usos);
        model.addAttribute("totalHabilidades", catalogo.size());
        model.addAttribute("activas", catalogo.stream().filter(h -> Boolean.TRUE.equals(h.getEstado())).count());
        model.addAttribute("categorias", categorias);

        if (desactivar != null) {
            habilidades.stream()
                    .filter(h -> desactivar.equals(h.getIdHabilidad()) && Boolean.TRUE.equals(h.getEstado()))
                    .findFirst().ifPresent(h -> model.addAttribute("habilidadConfirmar", h));
        }
        return "admin/habilidades";
    }

    /**
     * Prepara el formulario para registrar una habilidad nueva con estado activo.
     */
    @GetMapping("/habilidades/nueva")
    public String nuevaHabilidad(
            @ModelAttribute("filtros") FiltrosHabilidades filtros,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }
        Habilidad habilidad = new Habilidad();
        habilidad.setCategoria(filtros.categoriaFiltro());
        return formularioHabilidad(habilidad, model);
    }

    /**
     * Carga los datos de una habilidad existente para modificarla.
     */
    @GetMapping("/habilidades/editar/{id}")
    public String editarHabilidad(
            @PathVariable("id") Integer id,
            @ModelAttribute("filtros") FiltrosHabilidades filtros,
            HttpSession session,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        Habilidad habilidad = habilidadRepository.findById(id).orElse(null);
        if (habilidad == null) {
            redirect.addFlashAttribute("mensajeError", "La habilidad ya no está en el catálogo.");
            return redirigirCatalogoHabilidades(filtros, redirect);
        }
        model.addAttribute("estadoOriginal", habilidad.getEstado());
        return formularioHabilidad(habilidad, model);
    }

    /**
     * Valida y guarda la habilidad. Solicita confirmación si una edición la desactiva.
     */
    @PostMapping("/habilidades/guardar")
    @Transactional
    public String guardarHabilidad(
            @Valid @ModelAttribute("habilidad") Habilidad habilidad,
            BindingResult bindingResult,
            @ModelAttribute("filtros") FiltrosHabilidades filtros,
            @RequestParam(value = "estadoOriginal", required = false) Boolean estadoOriginal,
            @RequestParam(value = "confirmarDesactivacion", defaultValue = "false") boolean confirmarDesactivacion,
            HttpSession session,
            @RequestAttribute("usuarioVerificado") Usuario administrador,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("estadoOriginal", estadoOriginal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return formularioHabilidad(habilidad, model);
        }

        CategoriaHabilidad categoria = categoriaHabilidadRepository.buscarParaAsignar(habilidad.getCategoria()).orElse(null);
        if (categoria == null) {
            bindingResult.rejectValue("categoria", "validation.habilidad.categoria.catalogo");
            return formularioHabilidad(habilidad, model);
        }

        String nombreLimpio = habilidad.getNombre().trim();
        Integer id = habilidad.getIdHabilidad();
        Habilidad registrada = id == null ? null : habilidadRepository.buscarParaActualizar(id).orElse(null);
        if (id != null && registrada == null) {
            redirect.addFlashAttribute("mensajeError", "La habilidad ya no está en el catálogo.");
            return redirigirCatalogoHabilidades(filtros, redirect);
        }

        boolean nombreRepetido = habilidadRepository.findAll().stream()
                .anyMatch(h -> h.getNombre() != null
                        && h.getNombre().equalsIgnoreCase(nombreLimpio)
                        && !h.getIdHabilidad().equals(id));
        if (nombreRepetido) {
            bindingResult.rejectValue("nombre", "validation.habilidad.nombre.unique");
            return formularioHabilidad(habilidad, model);
        }

        if (registrada != null) {
            boolean estadoActual = Boolean.TRUE.equals(registrada.getEstado());
            if (habilidad.getEstado().equals(estadoOriginal)) {
                habilidad.setEstado(estadoActual);
            }
            if (estadoActual && !habilidad.getEstado() && !confirmarDesactivacion) {
                model.addAttribute("estadoOriginal", true);
                model.addAttribute("usoConfirmar", habilidadRepository.obtenerUsos(List.of(id)).get(0));
                return formularioHabilidad(habilidad, model);
            }
        }

        habilidad.setNombre(nombreLimpio);
        habilidad.setCategoria(categoria.getNombre());
        habilidad.setDescripcion(habilidad.getDescripcion().trim());
        habilidadRepository.save(habilidad);
        registrarAuditoria(administrador, "Guardar habilidad", "Habilidades",
                "Se actualizó el catálogo: " + habilidad.getNombre());

        redirect.addFlashAttribute("mensajeExito", "Habilidad guardada correctamente.");
        return redirigirCatalogoHabilidades(filtros, redirect);
    }

    /**
     * Aplica el estado solicitado, conservando las asociaciones ya registradas.
     */
    @PostMapping("/habilidades/{id}/estado")
    @Transactional
    public String cambiarEstadoHabilidad(
            @PathVariable("id") Integer id,
            @RequestParam("estado") boolean estado,
            @ModelAttribute("filtros") FiltrosHabilidades filtros,
            HttpSession session,
            @RequestAttribute("usuarioVerificado") Usuario administrador,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        Habilidad habilidad = habilidadRepository.buscarParaActualizar(id).orElse(null);
        if (habilidad == null) {
            redirect.addFlashAttribute("mensajeError", "La habilidad ya no está en el catálogo.");
            return redirigirCatalogoHabilidades(filtros, redirect);
        }

        if (estado != Boolean.TRUE.equals(habilidad.getEstado())) {
            habilidad.setEstado(estado);
            habilidadRepository.save(habilidad);
            registrarAuditoria(administrador, estado ? "Activar habilidad" : "Desactivar habilidad", "Habilidades", habilidad.getNombre());
        }
        redirect.addFlashAttribute("mensajeExito", estado ? "Habilidad activada." : "Habilidad desactivada.");
        return redirigirCatalogoHabilidades(filtros, redirect);
    }

    private String formularioHabilidad(Habilidad habilidad, Model model) {
        model.addAttribute("titulo", habilidad.getIdHabilidad() == null ? "Nueva habilidad" : "Editar habilidad");
        model.addAttribute("habilidad", habilidad);
        model.addAttribute("categorias", categoriaHabilidadRepository.findAllByOrderByNombreAsc());
        return "admin/habilidad-form";
    }

    private String redirigirCatalogoHabilidades(FiltrosHabilidades filtros, RedirectAttributes redirect) {
        if (!filtros.q().isEmpty()) {
            redirect.addAttribute("q", filtros.q());
        }
        if (!filtros.categoriaFiltro().isEmpty()) {
            redirect.addAttribute("categoriaFiltro", filtros.categoriaFiltro());
        }
        if (filtros.estadoFiltro() != null) {
            redirect.addAttribute("estadoFiltro", filtros.estadoFiltro());
        }
        return "redirect:/admin/habilidades";
    }


    /* =========================================================
       CATEGORÍAS
       ========================================================= */

    /**
     * Muestra las categorías y el estado de las habilidades vinculadas a cada una.
     */
    @GetMapping("/categorias")
    public String categorias(
            @RequestParam(value = "q", defaultValue = "") String q,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        String texto = normalizar(q);
        List<ResumenCategoria> catalogo = categoriaHabilidadRepository.resumirCatalogo();
        List<ResumenCategoria> categorias = catalogo.stream()
                .filter(c -> normalizar(c.getNombre()).contains(texto))
                .toList();

        model.addAttribute("titulo", "Categorías de habilidades");
        model.addAttribute("q", q);
        model.addAttribute("categorias", categorias);
        model.addAttribute("totalCategorias", catalogo.size());
        model.addAttribute("totalHabilidades", habilidadRepository.count());
        return "admin/categorias";
    }

    /**
     * Prepara el formulario para registrar una categoría.
     */
    @GetMapping("/categorias/nueva")
    public String nuevaCategoria(
            @RequestParam(value = "q", defaultValue = "") String q,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }
        return formularioCategoria(null, "", q, model);
    }

    /**
     * Carga la categoría que el administrador quiere renombrar.
     */
    @GetMapping("/categorias/editar/{id}")
    public String editarCategoria(
            @PathVariable("id") Integer id,
            @RequestParam(value = "q", defaultValue = "") String q,
            HttpSession session,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }
        CategoriaHabilidad categoria = categoriaHabilidadRepository.findById(id).orElse(null);
        if (categoria == null) {
            redirect.addAttribute("q", q);
            redirect.addFlashAttribute("mensajeError", "La categoría ya no está en el catálogo.");
            return "redirect:/admin/categorias";
        }
        return formularioCategoria(id, categoria.getNombre(), q, model);
    }

    /**
     * Registra una categoría o actualiza su nombre y sus habilidades en la misma transacción.
     */
    @PostMapping("/categorias/guardar")
    public String guardarCategoria(
            @RequestParam(value = "idCategoria", required = false) Integer id,
            @RequestParam(value = "nombre", defaultValue = "") String nombre,
            @RequestParam(value = "q", defaultValue = "") String q,
            HttpSession session,
            @RequestAttribute("usuarioVerificado") Usuario administrador,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        String nombreLimpio = nombre.replaceAll("(?U)\\s+", " ").trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 30) {
            model.addAttribute("errorNombre", nombreLimpio.isEmpty()
                    ? "Ingresa un nombre para la categoría."
                    : "El nombre no puede superar 30 caracteres.");
            return formularioCategoria(id, nombre, q, model);
        }

        boolean guardada;
        try {
            guardada = Boolean.TRUE.equals(transacciones.execute(status -> {
                CategoriaHabilidad categoria = id == null
                        ? new CategoriaHabilidad() : categoriaHabilidadRepository.buscarParaRenombrar(id).orElse(null);
                if (categoria == null) {
                    return false;
                }

                String anterior = categoria.getNombre();
                categoria.setNombre(nombreLimpio);
                categoriaHabilidadRepository.saveAndFlush(categoria);
                if (id != null && !nombreLimpio.equals(anterior)) {
                    habilidadRepository.renombrarCategoria(anterior, nombreLimpio);
                }
                registrarAuditoria(administrador, id == null ? "Crear categoría" : "Renombrar categoría", "Categorías",
                        id == null ? nombreLimpio : anterior + " → " + nombreLimpio);
                return true;
            }));
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorNombre", "Ya existe una categoría con ese nombre.");
            return formularioCategoria(id, nombre, q, model);
        }

        redirect.addAttribute("q", q);
        if (guardada) {
            redirect.addFlashAttribute("mensajeExito", id == null ? "Categoría registrada." : "Nombre actualizado.");
        } else {
            redirect.addFlashAttribute("mensajeError", "La categoría ya no está en el catálogo.");
        }
        return "redirect:/admin/categorias";
    }

    private String formularioCategoria(Integer id, String nombre, String q, Model model) {
        model.addAttribute("titulo", id == null ? "Nueva categoría" : "Renombrar categoría");
        model.addAttribute("idCategoria", id);
        model.addAttribute("nombreCategoria", nombre);
        model.addAttribute("q", q);
        return "admin/categoria-form";
    }


    /* =========================================================
       AUDITORÍA
       ========================================================= */

    /**
     * Consulta todo el historial de auditoría con filtros y páginas de diez eventos.
     */
    @GetMapping("/auditoria")
    public String auditoria(
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "modulo", defaultValue = "") String modulo,
            @RequestParam(value = "desde", defaultValue = "") String desde,
            @RequestParam(value = "hasta", defaultValue = "") String hasta,
            @RequestParam(value = "pagina", defaultValue = "1") int numeroPagina,
            HttpSession session,
            Model model
    ) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        q = q.strip();
        modulo = modulo.strip();
        desde = desde.strip();
        hasta = hasta.strip();
        model.addAttribute("titulo", "Auditoría");
        model.addAttribute("q", q);
        model.addAttribute("modulo", modulo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("modulos", auditoriaRepository.listarModulos());

        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = leerFechaAuditoria(desde);
            fin = leerFechaAuditoria(hasta);
        } catch (DateTimeException e) {
            model.addAttribute("mensajeError", "Ingresa fechas válidas para consultar el registro.");
            return "admin/auditoria";
        }

        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            model.addAttribute("mensajeError", "La fecha final debe ser igual o posterior a la inicial.");
            return "admin/auditoria";
        }

        String texto = normalizar(q);
        String patron = texto.isEmpty() ? ""
                : "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        LocalDateTime desdeFecha = inicio == null ? null : inicio.atStartOfDay();
        LocalDateTime hastaFecha = fin == null || fin.equals(LocalDate.of(9999, 12, 31))
                ? null : fin.plusDays(1).atStartOfDay();

        int porPagina = 10;
        int indice = Math.min(Math.max(1, numeroPagina) - 1, Integer.MAX_VALUE / porPagina);
        Page<Auditoria> resultados = auditoriaRepository.buscar(patron, modulo, desdeFecha, hastaFecha,
                PageRequest.of(indice, porPagina));
        if (resultados.getTotalPages() > 0 && indice >= resultados.getTotalPages()) {
            resultados = auditoriaRepository.buscar(patron, modulo, desdeFecha, hastaFecha,
                    PageRequest.of(resultados.getTotalPages() - 1, porPagina));
        }

        int paginaActual = resultados.getNumber() + 1;
        long inicioPagina = resultados.getPageable().getOffset();
        model.addAttribute("resultados", resultados);
        model.addAttribute("primerRegistro", inicioPagina + 1);
        model.addAttribute("ultimoRegistro", inicioPagina + resultados.getNumberOfElements());
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("primeraPaginaVisible", Math.max(1, paginaActual - 2));
        model.addAttribute("ultimaPaginaVisible", Math.min(resultados.getTotalPages(), paginaActual + 2));
        return "admin/auditoria";
    }

    private LocalDate leerFechaAuditoria(String valor) {
        if (valor.isEmpty()) {
            return null;
        }
        LocalDate fecha = LocalDate.parse(valor);
        if (fecha.getYear() < 1 || fecha.getYear() > 9999) {
            throw new DateTimeException("Año fuera del rango permitido.");
        }
        return fecha;
    }


    /* =========================================================
       MONITOREO
       ========================================================= */

    /**
     * Muestra indicadores generales y la actividad reciente del sistema.
     *
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan los conteos y eventos recientes
     * @return la vista de monitoreo o una redirección al inicio de sesión
     */
    @GetMapping("/monitoreo")
    public String monitoreo(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        List<Auditoria> eventos =
                auditoriaRepository
                        .findTop50ByOrderByFechaDesc();


        model.addAttribute(
                "titulo",
                "Monitoreo del sistema"
        );


        model.addAttribute(
                "usuarios",
                usuarioRepository.count()
        );


        model.addAttribute(
                "colaboradores",
                colaboradorRepository.count()
        );


        model.addAttribute(
                "eventos",
                eventos.size()
        );


        model.addAttribute(
                "habilidades",
                habilidadRepository.count()
        );


        model.addAttribute(
                "actividad",
                eventos.stream()
                        .limit(8)
                        .toList()
        );


        return "admin/monitoreo";
    }


    /* =========================================================
       VALIDAR SESIÓN ADMINISTRADOR
       ========================================================= */

    /**
     * Comprueba que la sesión corresponda a un usuario activo con rol administrador.
     *
     * @param session sesión HTTP que contiene el identificador del usuario
     * @return {@code true} si la sesión pertenece a un administrador activo; {@code false} en caso contrario
     */
    private boolean esAdmin(
            HttpSession session
    ) {

        Object idSesion =
                session.getAttribute(
                        "usuarioSesionId"
                );


        if (!(idSesion instanceof Integer)) {
            return false;
        }


        Integer idUsuario =
                (Integer) idSesion;


        Optional<Usuario> encontrado =
                usuarioRepository.findById(
                        idUsuario
                );


        if (encontrado.isEmpty()) {
            return false;
        }


        Usuario usuario =
                encontrado.get();


        if (
                usuario.getEstado()
                        != Usuario.EstadoUsuario.ACTIVO
        ) {
            return false;
        }


        if (
                usuario.getRol() == null
                        ||
                        usuario.getRol().getNombre() == null
        ) {
            return false;
        }


        return "ADMINISTRADOR".equals(
                usuario.getRol().getNombre()
        );
    }


    /* =========================================================
       NORMALIZAR TEXTO
       ========================================================= */

    /*
     * Permite que:
     *
     * Administración
     * administracion
     * ADMINISTRACIÓN
     *
     * sean tratados de la misma forma
     * durante las búsquedas.
     */

    /**
     * Elimina acentos y caracteres diacríticos del texto y lo convierte a minúsculas sin espacios externos.
     *
     * @param texto texto que se desea normalizar
     * @return el texto normalizado o una cadena vacía si el valor recibido es nulo
     */
    private String normalizar(
            String texto
    ) {

        if (
                texto == null
        ) {

            return "";

        }


        return java.text.Normalizer
                .normalize(
                        texto,
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .toLowerCase()
                .trim();
    }


    /* =========================================================
       REGISTRAR AUDITORÍA
       ========================================================= */

    /**
     * Guarda un evento asociado a la cuenta que realizó la acción.
     *
     * @param administrador cuenta autenticada que realizó la acción
     * @param accion acción registrada
     * @param modulo módulo donde ocurrió la acción
     * @param detalle descripción del evento
     */
    private void registrarAuditoria(Usuario administrador, String accion, String modulo, String detalle) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(administrador);
        auditoria.setAccion(accion);
        auditoria.setModulo(modulo);
        auditoria.setDetalle(detalle);
        auditoriaRepository.save(auditoria);
    }


    /* =========================================================
       REPORTES ADMINISTRADOR
       ========================================================= */

    /**
     * Prepara los datos consolidados para la pantalla de reportes administrativos.
     *
     * @param reporte nombre opcional del reporte seleccionado
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan los indicadores, el gráfico y la vista previa
     * @return la vista de reportes o una redirección al inicio de sesión
     */
    @GetMapping("/reportes")
    public String reportes(
            @RequestParam(value = "reporte", required = false) String reporte,
            @RequestParam(value = "vista", defaultValue = "barras") String vista,
            @RequestParam(value = "periodo", defaultValue = "planificacion") String periodo,
            @RequestParam(value = "proyecto", required = false) Integer proyecto,
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Habilidad> habilidades = habilidadRepository.findAll();

        boolean vigenteHoy = "hoy".equals(periodo);
        var graficoDedicacion = reporteAdministrativoService.consultarDedicacion(vigenteHoy);
        var visualizacion = reporteAdministrativoService.visualizarDedicacion(graficoDedicacion, proyecto);
        model.addAttribute("titulo", "Reportes del sistema");
        model.addAttribute("graficoDedicacion", graficoDedicacion);
        model.addAttribute("periodoDedicacion", vigenteHoy ? "hoy" : "planificacion");
        model.addAttribute("visualizacionDedicacion", visualizacion);
        model.addAttribute("modoDedicacion", "matriz".equals(vista) ? "matriz" : "barras");
        model.addAttribute("proyectoDedicacion", visualizacion.reparto().proyecto() == null
                ? null : visualizacion.reparto().proyecto().id());
        var vistaPrevia = switch (reporte == null ? "" : reporte) {
            case "dedicacion" -> reporteAdministrativoService.tablaDedicacion(graficoDedicacion);
            case "usuarios" -> reporteAdministrativoService.tablaUsuarios(usuarios);
            case "habilidades" -> reporteAdministrativoService.tablaHabilidades(habilidades);
            case "auditoria" -> reporteAdministrativoService.tablaAuditoria(auditoriaRepository.findTop50ByOrderByFechaDesc());
            default -> null;
        };
        model.addAttribute("vistaPrevia", vistaPrevia);
        model.addAttribute("reporteActivo", vistaPrevia == null ? null : reporte);

        model.addAttribute("totalUsuarios", usuarios.size());
        model.addAttribute("usuariosActivos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.ACTIVO).count());
        model.addAttribute("usuariosInactivos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.INACTIVO).count());
        model.addAttribute("usuariosBloqueados", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.BLOQUEADO).count());
        model.addAttribute("totalHabilidades", habilidades.size());

        return "admin/reportes";
    }


    @GetMapping("/reportes/excel")
    public ResponseEntity<byte[]> descargarExcel(@RequestParam("tipo") String tipo,
                                                 @RequestParam(value = "periodo", defaultValue = "planificacion") String periodo,
                                                 HttpSession session) throws IOException {
        if (!esAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        String clave = tipo.toLowerCase(Locale.ROOT);
        var tabla = switch (clave) {
            case "dedicacion" -> reporteAdministrativoService.tablaDedicacion(
                    reporteAdministrativoService.consultarDedicacion("hoy".equals(periodo)));
            case "usuarios" -> reporteAdministrativoService.tablaUsuarios(usuarioRepository.findAll());
            case "habilidades" -> reporteAdministrativoService.tablaHabilidades(habilidadRepository.findAll());
            case "auditoria" -> reporteAdministrativoService.tablaAuditoria(auditoriaRepository.findTop50ByOrderByFechaDesc());
            default -> null;
        };
        if (tabla == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + clave + ".xlsx\"")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reporteAdministrativoService.generarExcel(tabla));
    }

    /**
     * Descarga la misma tabla administrativa que Excel, ahora en formato PDF.
     * El permiso de cada tipo también es validado por PermisosWebConfig.
     */
    @GetMapping(value = "/reportes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@RequestParam("tipo") String tipo,
                                               @RequestParam(value = "periodo", defaultValue = "planificacion") String periodo,
                                               HttpSession session) {
        if (!esAdmin(session)) return ResponseEntity.status(403).build();
        String clave = tipo.toLowerCase(Locale.ROOT);
        var tabla = switch (clave) {
            case "dedicacion" -> reporteAdministrativoService.tablaDedicacion(
                    reporteAdministrativoService.consultarDedicacion("hoy".equals(periodo)));
            case "usuarios" -> reporteAdministrativoService.tablaUsuarios(usuarioRepository.findAll());
            case "habilidades" -> reporteAdministrativoService.tablaHabilidades(habilidadRepository.findAll());
            case "auditoria" -> reporteAdministrativoService.tablaAuditoria(auditoriaRepository.findTop50ByOrderByFechaDesc());
            default -> null;
        };
        if (tabla == null) return ResponseEntity.badRequest().build();
        byte[] pdf = ReportePdf.generar(tabla.nombre(), tabla.columnas(), tabla.filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + clave + ".pdf\"")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Genera y devuelve un archivo CSV con los datos del tipo de reporte solicitado.
     *
     * @param tipo tipo de reporte que se exportará
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @return una respuesta con el CSV generado o un mensaje de error de acceso o tipo
     */
    @GetMapping(value = "/reportes/exportar", produces = "text/csv")
    public ResponseEntity<String> exportarReporte(
            @RequestParam("tipo") String tipo,
            HttpSession session
    ) {

        if (!esAdmin(session)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }

        StringBuilder csv = new StringBuilder("\uFEFF");
        String nombreArchivo;

        switch (tipo == null ? "" : tipo.toLowerCase()) {
            case "usuarios" -> {
                nombreArchivo = "reporte_usuarios.csv";
                csv.append("ID,Nombre,Correo,Rol,Estado,Telefono\n");
                for (Usuario u : usuarioRepository.findAll()) {
                    csv.append(csv(u.getIdUsuario())).append(',')
                            .append(csv(u.getNombreCompleto())).append(',')
                            .append(csv(u.getCorreo())).append(',')
                            .append(csv(u.getRol() == null ? "" : u.getRol().getNombre())).append(',')
                            .append(csv(u.getEstado())).append(',')
                            .append(csv(u.getTelefono())).append('\n');
                }
            }
            case "habilidades" -> {
                nombreArchivo = "reporte_habilidades.csv";
                csv.append("ID,Habilidad,Categoria,Estado,Descripcion\n");
                for (Habilidad h : habilidadRepository.findAll()) {
                    csv.append(csv(h.getIdHabilidad())).append(',')
                            .append(csv(h.getNombre())).append(',')
                            .append(csv(h.getCategoria())).append(',')
                            .append(csv(Boolean.TRUE.equals(h.getEstado()) ? "ACTIVA" : "INACTIVA")).append(',')
                            .append(csv(h.getDescripcion())).append('\n');
                }
            }
            case "auditoria" -> {
                nombreArchivo = "reporte_auditoria.csv";
                csv.append("Fecha,Usuario,Accion,Modulo,Detalle\n");
                for (Auditoria a : auditoriaRepository.findTop50ByOrderByFechaDesc()) {
                    csv.append(csv(a.getFecha())).append(',')
                            .append(csv(a.getUsuario() == null ? "Sistema" : a.getUsuario().getNombreCompleto())).append(',')
                            .append(csv(a.getAccion())).append(',')
                            .append(csv(a.getModulo())).append(',')
                            .append(csv(a.getDetalle())).append('\n');
                }
            }
            default -> {
                return ResponseEntity.badRequest().body("Tipo de reporte no válido");
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString());
    }


    /**
     * Convierte un valor a texto CSV y aplica el escape necesario para conservar
     * comillas y evitar fórmulas en el archivo exportado.
     *
     * @param valor valor que se incluirá en una celda del CSV
     * @return el valor convertido y protegido para su uso como celda CSV
     */
    private String csv(Object valor) {
        String texto = valor == null ? "" : String.valueOf(valor);
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

}
