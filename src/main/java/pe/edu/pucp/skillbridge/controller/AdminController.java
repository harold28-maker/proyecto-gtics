package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {


    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final HabilidadRepository habilidadRepository;
    private final AuditoriaRepository auditoriaRepository;

    // BCrypt se usa únicamente para almacenar contraseñas de forma hasheada.
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();


    public AdminController(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            ColaboradorRepository colaboradorRepository,
            HabilidadRepository habilidadRepository,
            AuditoriaRepository auditoriaRepository
    ) {

        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.habilidadRepository = habilidadRepository;
        this.auditoriaRepository = auditoriaRepository;
    }


    /* =========================================================
       INICIO ADMINISTRADOR
       ========================================================= */

    @RequestMapping(
            value = "/inicio",
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
                "Centro de administración"
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
                        .limit(5)
                        .toList()
        );


        return "admin/inicio";
    }


    /* =========================================================
       USUARIOS
       ========================================================= */

    @GetMapping("/usuarios")
    public String usuarios(
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


        List<Usuario> lista =
                usuarioRepository.findAll();


        if (
                q != null
                        &&
                        !q.isBlank()
        ) {

            String texto =
                    q.toLowerCase();


            lista = lista.stream()
                    .filter(
                            u ->
                                    u.getNombreCompleto()
                                            .toLowerCase()
                                            .contains(texto)

                                            ||

                                            u.getCorreo()
                                                    .toLowerCase()
                                                    .contains(texto)

                                            ||

                                            u.getRol()
                                                    .getNombre()
                                                    .toLowerCase()
                                                    .contains(texto)
                    )
                    .toList();
        }


        cargarUsuarios(
                model,
                lista,
                q
        );


        return "admin/usuarios";
    }


    private void cargarUsuarios(
            Model model,
            List<Usuario> lista,
            String q
    ) {

        model.addAttribute(
                "titulo",
                "Gestión de usuarios"
        );


        model.addAttribute(
                "usuarios",
                lista
        );


        model.addAttribute(
                "q",
                q
        );


        model.addAttribute(
                "activos",
                lista.stream()
                        .filter(
                                u ->
                                        u.getEstado()
                                                == Usuario.EstadoUsuario.ACTIVO
                        )
                        .count()
        );


        model.addAttribute(
                "inactivos",
                lista.stream()
                        .filter(
                                u ->
                                        u.getEstado()
                                                == Usuario.EstadoUsuario.INACTIVO
                        )
                        .count()
        );


        model.addAttribute(
                "bloqueados",
                lista.stream()
                        .filter(
                                u ->
                                        u.getEstado()
                                                == Usuario.EstadoUsuario.BLOQUEADO
                        )
                        .count()
        );
    }


    /* =========================================================
       NUEVO USUARIO
       ========================================================= */

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


    /* =========================================================
       GUARDAR USUARIO
       ========================================================= */

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

            HttpSession session,
            Model model
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
                        : correo.trim().toLowerCase();

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


        /* =====================================================
           NUEVO O EDICIÓN
           ===================================================== */

        boolean nuevo =
                idUsuario == null;

        Usuario usuario;


        if (nuevo) {

            usuario =
                    new Usuario();

        } else {

            usuario =
                    usuarioRepository
                            .findById(idUsuario)
                            .orElse(null);

            if (usuario == null) {

                return volverFormularioUsuario(
                        model,
                        formulario,
                        "El usuario que intentas editar no existe."
                );
            }
        }


        /* =====================================================
           CONTRASEÑA
           ===================================================== */

        String clave =
                password == null
                        ? ""
                        : password;

        String confirmacion =
                confirmarPassword == null
                        ? ""
                        : confirmarPassword;


        if (
                nuevo
                        &&
                        clave.isBlank()
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "La contraseña es obligatoria para un usuario nuevo."
            );
        }


        if (
                !nuevo
                        &&
                        clave.isBlank()
                        &&
                        !confirmacion.isBlank()
        ) {

            return volverFormularioUsuario(
                    model,
                    formulario,
                    "Ingresa la nueva contraseña antes de confirmarla."
            );
        }


        if (!clave.isBlank()) {

            if (
                    !clave.matches(
                            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,30}$"
                    )
            ) {

                return volverFormularioUsuario(
                        model,
                        formulario,
                        "La contraseña debe tener entre 8 y 30 caracteres, una mayúscula, una minúscula y un número."
                );
            }


            if (!clave.equals(confirmacion)) {

                return volverFormularioUsuario(
                        model,
                        formulario,
                        "Las contraseñas no coinciden."
                );
            }


            usuario.setPasswordHash(
                    passwordEncoder.encode(
                            clave
                    )
            );
        }


        /* =====================================================
           COPIAR SOLO CAMPOS YA VALIDADOS
           ===================================================== */

        usuario.setNombres(
                nombresLimpios
        );

        usuario.setApellidos(
                apellidosLimpios
        );

        usuario.setCorreo(
                correoLimpio
        );

        usuario.setTelefono(
                telefonoLimpio
        );

        usuario.setEstado(
                estado
        );

        usuario.setRol(
                rol
        );


        /* =====================================================
           GUARDAR
           ===================================================== */

        Usuario guardado =
                usuarioRepository.save(
                        usuario
                );


        if (
                !"ADMINISTRADOR".equals(
                        rol.getNombre()
                )
                        &&
                        colaboradorRepository
                                .findByUsuarioIdUsuario(
                                        guardado.getIdUsuario()
                                )
                                .isEmpty()
        ) {

            Colaborador colaborador =
                    new Colaborador();

            colaborador.setUsuario(
                    guardado
            );

            colaborador.setCargo(
                    "Por definir"
            );

            colaborador.setArea(
                    "Por definir"
            );

            colaborador.setDisponibilidadBase(
                    100
            );

            colaboradorRepository.save(
                    colaborador
            );
        }


        registrarAuditoria(
                nuevo ? "Crear usuario" : "Actualizar usuario",
                "Administración",
                (nuevo
                        ? "Se registró la cuenta de "
                        : "Se actualizó la cuenta de ")
                        + guardado.getNombreCompleto()
        );


        cargarUsuarios(
                model,
                usuarioRepository.findAll(),
                null
        );


        model.addAttribute(
                "mensajeExito",
                nuevo
                        ? "Usuario registrado correctamente."
                        : "Usuario actualizado correctamente."
        );


        return "admin/usuarios";
    }


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


    private String volverFormularioUsuario(
            Model model,
            Usuario usuario,
            String error
    ) {

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

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstadoUsuario(
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


        Usuario usuario =
                encontrado.get();


        usuario.setEstado(
                usuario.getEstado()
                        == Usuario.EstadoUsuario.ACTIVO
                        ? Usuario.EstadoUsuario.INACTIVO
                        : Usuario.EstadoUsuario.ACTIVO
        );


        usuarioRepository.save(
                usuario
        );


        registrarAuditoria(
                "Cambiar estado de usuario",
                "Administración",
                usuario.getNombreCompleto()
                        + " quedó en estado "
                        + usuario.getEstado()
        );


        cargarUsuarios(
                model,
                usuarioRepository.findAll(),
                null
        );


        model.addAttribute(
                "mensajeExito",
                "Estado del usuario actualizado."
        );


        return "admin/usuarios";
    }


    /* =========================================================
       ROLES Y PERMISOS
       ========================================================= */

    @GetMapping("/roles")
    public String roles(
            HttpSession session,
            Model model
    ) {

        if (!esAdmin(session)) {
            return "redirect:/login";
        }


        List<Rol> roles =
                rolRepository.findAll();


        model.addAttribute(
                "titulo",
                "Roles y permisos"
        );


        model.addAttribute(
                "roles",
                roles
        );


        model.addAttribute(
                "permisos",
                permisoRepository.findAll()
        );


        model.addAttribute(
                "totalPermisos",
                permisoRepository.count()
        );


        model.addAttribute(
                "totalUsuarios",
                usuarioRepository.count()
        );


        return "admin/roles";
    }


    /* =========================================================
       HABILIDADES
       ========================================================= */

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

}
