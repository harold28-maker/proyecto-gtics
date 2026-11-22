package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;
import pe.edu.pucp.skillbridge.service.CorreoCuentaService;
import pe.edu.pucp.skillbridge.service.FotoPerfilService;
import pe.edu.pucp.skillbridge.service.ReporteAdministrativoService;
import pe.edu.pucp.skillbridge.util.PoliticaContrasena;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

                registrarAuditoria(nuevo ? "Crear usuario" : "Actualizar usuario", "Administración",
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
     * Obtiene el catálogo de habilidades y permite filtrarlo por nombre.
     *
     * @param q texto opcional utilizado para buscar habilidades
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega la lista de habilidades
     * @return la vista del catálogo o una redirección al inicio de sesión
     */
    @GetMapping("/habilidades")
    public String habilidades(
            @RequestParam(
                    value = "q",
                    required = false
            )
            String q,

            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        List<Habilidad> habilidades =

                (
                        q == null
                                ||
                                q.isBlank()
                )

                        ? habilidadRepository.findAll()

                        : habilidadRepository
                          .findByNombreContainingIgnoreCase(q);


        cargarHabilidades(
                model,
                habilidades,
                q
        );


        return "admin/habilidades";
    }

    /**
     * Agrega al modelo el catálogo de habilidades y sus indicadores de estado y categorías.
     *
     * @param model modelo al que se agregan los datos de la vista
     * @param habilidades habilidades que se muestran
     * @param q texto utilizado actualmente como filtro
     */
    private void cargarHabilidades(
            Model model,
            List<Habilidad> habilidades,
            String q
    ) {

        model.addAttribute(
                "titulo",
                "Catálogo de habilidades"
        );


        model.addAttribute(
                "habilidades",
                habilidades
        );


        model.addAttribute(
                "q",
                q
        );


        model.addAttribute(
                "activas",
                habilidades.stream()
                        .filter(
                                h ->
                                        Boolean.TRUE.equals(
                                                h.getEstado()
                                        )
                        )
                        .count()
        );


        model.addAttribute(
                "categorias",
                habilidades.stream()
                        .map(
                                Habilidad::getCategoria
                        )
                        .filter(
                                c ->
                                        c != null
                                                &&
                                                !c.isBlank()
                        )
                        .distinct()
                        .count()
        );
    }


    /* =========================================================
       NUEVA HABILIDAD
       ========================================================= */

    /**
     * Prepara el formulario para registrar una habilidad nueva con estado activo.
     *
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega la habilidad nueva
     * @return la vista del formulario o una redirección al inicio de sesión
     */
    @GetMapping("/habilidades/nueva")
    public String nuevaHabilidad(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        Habilidad habilidad =
                new Habilidad();


        habilidad.setEstado(
                true
        );


        model.addAttribute(
                "titulo",
                "Nueva habilidad"
        );


        model.addAttribute(
                "habilidad",
                habilidad
        );


        return "admin/habilidad-form";
    }


    /* =========================================================
       EDITAR HABILIDAD
       ========================================================= */

    /**
     * Carga los datos de una habilidad existente para modificarla.
     *
     * @param idTexto identificador textual de la habilidad recibido en la ruta
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega la habilidad encontrada
     * @return la vista del formulario o una redirección al catálogo
     */
    @GetMapping("/habilidades/editar/{id}")
    public String editarHabilidad(
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
            return "redirect:/admin/habilidades";
        }


        Optional<Habilidad> encontrada =
                habilidadRepository.findById(id);


        if (encontrada.isEmpty()) {
            return "redirect:/admin/habilidades";
        }


        model.addAttribute(
                "titulo",
                "Editar habilidad"
        );


        model.addAttribute(
                "habilidad",
                encontrada.get()
        );


        return "admin/habilidad-form";
    }


    /* =========================================================
       GUARDAR HABILIDAD
       ========================================================= */

    /**
     * Valida y guarda una habilidad nueva o actualiza una existente en el catálogo.
     *
     * @param habilidad habilidad recibida y validada desde el formulario
     * @param bindingResult resultados de las validaciones realizadas sobre la habilidad
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan los errores o la lista actualizada
     * @return la vista del formulario ante un error o la vista del catálogo al guardar
     */
    @PostMapping("/habilidades/guardar")
    public String guardarHabilidad(
            @Valid @ModelAttribute("habilidad") Habilidad habilidad,
            BindingResult bindingResult,
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", habilidad.getIdHabilidad() == null
                    ? "Nueva habilidad" : "Editar habilidad");
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "admin/habilidad-form";
        }

        String nombreLimpio = habilidad.getNombre().trim();
        boolean nombreRepetido = habilidadRepository.findAll().stream()
                .anyMatch(registrada -> registrada.getNombre() != null
                        && registrada.getNombre().equalsIgnoreCase(nombreLimpio)
                        && (habilidad.getIdHabilidad() == null
                        || !registrada.getIdHabilidad().equals(habilidad.getIdHabilidad())));

        if (nombreRepetido) {
            bindingResult.rejectValue("nombre", "validation.habilidad.nombre.unique");
            model.addAttribute("titulo", habilidad.getIdHabilidad() == null
                    ? "Nueva habilidad" : "Editar habilidad");
            model.addAttribute("error", bindingResult.getFieldError("nombre").getDefaultMessage());
            return "admin/habilidad-form";
        }

        habilidad.setNombre(nombreLimpio);
        habilidad.setCategoria(habilidad.getCategoria().trim());
        habilidad.setDescripcion(habilidad.getDescripcion().trim());


        habilidadRepository.save(
                habilidad
        );


        registrarAuditoria(
                "Guardar habilidad",
                "Habilidades",
                "Se actualizó el catálogo: "
                        + habilidad.getNombre()
        );


        cargarHabilidades(
                model,
                habilidadRepository.findAll(),
                null
        );


        model.addAttribute(
                "mensajeExito",
                "Habilidad guardada correctamente."
        );


        return "admin/habilidades";
    }


    /* =========================================================
       CAMBIAR ESTADO HABILIDAD
       ========================================================= */

    /**
     * Invierte el estado de una habilidad y actualiza el catálogo mostrado.
     *
     * @param idTexto identificador textual de la habilidad recibido en la ruta
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega la lista actualizada de habilidades
     * @return la vista del catálogo o una redirección según la validación realizada
     */
    @PostMapping("/habilidades/{id}/estado")
    public String cambiarEstadoHabilidad(
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
            return "redirect:/admin/habilidades";
        }


        Optional<Habilidad> encontrada =
                habilidadRepository.findById(id);


        if (encontrada.isEmpty()) {
            return "redirect:/admin/habilidades";
        }


        Habilidad habilidad =
                encontrada.get();


        habilidad.setEstado(
                !Boolean.TRUE.equals(
                        habilidad.getEstado()
                )
        );


        habilidadRepository.save(
                habilidad
        );


        cargarHabilidades(
                model,
                habilidadRepository.findAll(),
                null
        );


        model.addAttribute(
                "mensajeExito",
                "Estado de la habilidad actualizado."
        );


        return "admin/habilidades";
    }


    /* =========================================================
       CATEGORÍAS
       ========================================================= */

    /**
     * Agrupa las habilidades existentes por categoría y muestra los totales asociados.
     *
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agregan las categorías y sus cantidades
     * @return la vista de categorías o una redirección al inicio de sesión
     */
    @GetMapping("/categorias")
    public String categorias(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        Map<String, Long> categorias =
                new LinkedHashMap<>();


        for (
                Habilidad h :
                habilidadRepository.findAll()
        ) {

            String categoria =

                    (
                            h.getCategoria() == null
                                    ||
                                    h.getCategoria().isBlank()
                    )

                            ? "Sin categoría"

                            : h.getCategoria();


            categorias.put(
                    categoria,
                    categorias.getOrDefault(
                            categoria,
                            0L
                    ) + 1
            );
        }


        model.addAttribute(
                "titulo",
                "Categorías de habilidades"
        );


        model.addAttribute(
                "categorias",
                categorias
        );


        model.addAttribute(
                "totalCategorias",
                categorias.size()
        );


        model.addAttribute(
                "totalHabilidades",
                habilidadRepository.count()
        );


        return "admin/categorias";
    }


    /* =========================================================
       AUDITORÍA
       ========================================================= */

    /**
     * Consulta los registros de auditoría recientes y permite filtrarlos por texto o módulo.
     *
     * @param q texto opcional utilizado para buscar en los datos de auditoría
     * @param modulo módulo opcional por el que se filtran los registros
     * @param session sesión HTTP utilizada para comprobar el acceso administrativo
     * @param model modelo al que se agrega la lista y los filtros actuales
     * @return la vista de auditoría o una redirección al inicio de sesión
     */
    @GetMapping("/auditoria")
    public String auditoria(

            @RequestParam(
                    value = "q",
                    required = false
            )
            String q,

            @RequestParam(
                    value = "modulo",
                    required = false
            )
            String modulo,

            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        /*
         * Primero cargamos los últimos
         * registros de auditoría.
         */

        List<Auditoria> lista =
                auditoriaRepository
                        .findTop50ByOrderByFechaDesc();


        /* =====================================================
           FILTRO POR TEXTO
           ===================================================== */

        if (
                q != null
                        &&
                        !q.isBlank()
        ) {

            String texto =
                    normalizar(q);


            lista = lista.stream()
                    .filter(
                            a -> {


                                String usuario =
                                        "";


                                if (
                                        a.getUsuario()
                                                != null
                                ) {

                                    usuario =
                                            normalizar(
                                                    a.getUsuario()
                                                            .getNombreCompleto()
                                            );

                                }


                                String accion =
                                        normalizar(
                                                a.getAccion()
                                        );


                                String moduloAuditoria =
                                        normalizar(
                                                a.getModulo()
                                        );


                                String detalle =
                                        normalizar(
                                                a.getDetalle()
                                        );


                                return

                                        usuario.contains(texto)

                                                ||

                                                accion.contains(texto)

                                                ||

                                                moduloAuditoria.contains(texto)

                                                ||

                                                detalle.contains(texto);

                            }
                    )
                    .toList();
        }


        /* =====================================================
           FILTRO POR MÓDULO
           ===================================================== */

        if (
                modulo != null
                        &&
                        !modulo.isBlank()
        ) {

            String moduloBuscado =
                    normalizar(modulo);


            lista = lista.stream()
                    .filter(
                            a ->
                                    normalizar(
                                            a.getModulo()
                                    )
                                            .equals(
                                                    moduloBuscado
                                            )
                    )
                    .toList();
        }


        /* =====================================================
           DATOS PARA EL HTML
           ===================================================== */

        model.addAttribute(
                "titulo",
                "Auditoría"
        );


        model.addAttribute(
                "auditorias",
                lista
        );


        /*
         * Devolvemos q y modulo para que
         * el HTML conserve los filtros.
         */

        model.addAttribute(
                "q",
                q
        );


        model.addAttribute(
                "modulo",
                modulo
        );


        return "admin/auditoria";
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
     * Crea y guarda un registro de auditoría asociado al primer administrador encontrado.
     *
     * @param accion acción realizada que se registrará
     * @param modulo módulo desde el que se realizó la acción
     * @param detalle descripción adicional del evento registrado
     */
    private void registrarAuditoria(
            String accion,
            String modulo,
            String detalle
    ) {

        Usuario admin =
                usuarioRepository
                        .findFirstByRolNombreOrderByIdUsuarioAsc(
                                "ADMINISTRADOR"
                        )
                        .orElse(null);


        Auditoria auditoria =
                new Auditoria();


        auditoria.setUsuario(
                admin
        );


        auditoria.setAccion(
                accion
        );


        auditoria.setModulo(
                modulo
        );


        auditoria.setDetalle(
                detalle
        );


        auditoriaRepository.save(
                auditoria
        );
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
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Habilidad> habilidades = habilidadRepository.findAll();

        var graficoDedicacion = reporteAdministrativoService.consultarDedicacion();
        model.addAttribute("titulo", "Reportes del sistema");
        model.addAttribute("graficoDedicacion", graficoDedicacion);
        model.addAttribute("vistaPrevia", switch (reporte == null ? "" : reporte) {
            case "dedicacion" -> reporteAdministrativoService.tablaDedicacion(graficoDedicacion);
            case "usuarios" -> reporteAdministrativoService.tablaUsuarios(usuarios);
            case "habilidades" -> reporteAdministrativoService.tablaHabilidades(habilidades);
            case "auditoria" -> reporteAdministrativoService.tablaAuditoria(auditoriaRepository.findTop50ByOrderByFechaDesc());
            default -> null;
        });

        model.addAttribute("totalUsuarios", usuarios.size());
        model.addAttribute("usuariosActivos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.ACTIVO).count());
        model.addAttribute("usuariosInactivos", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.INACTIVO).count());
        model.addAttribute("usuariosBloqueados", usuarios.stream()
                .filter(u -> u.getEstado() == Usuario.EstadoUsuario.BLOQUEADO).count());
        model.addAttribute("totalHabilidades", habilidades.size());
        model.addAttribute("habilidadesActivas", habilidades.stream()
                .filter(h -> Boolean.TRUE.equals(h.getEstado())).count());

        return "admin/reportes";
    }


    @GetMapping("/reportes/excel")
    public ResponseEntity<byte[]> descargarExcel(@RequestParam("tipo") String tipo, HttpSession session)
            throws IOException {
        if (!esAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        String clave = tipo.toLowerCase(Locale.ROOT);
        var tabla = switch (clave) {
            case "dedicacion" -> reporteAdministrativoService.tablaDedicacion(reporteAdministrativoService.consultarDedicacion());
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
