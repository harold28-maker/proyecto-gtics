package pe.edu.pucp.skillbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import pe.edu.pucp.skillbridge.entity.*;
import pe.edu.pucp.skillbridge.repository.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/admin")
public class AdminController {


    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final HabilidadRepository habilidadRepository;
    private final AuditoriaRepository auditoriaRepository;


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
    public String inicio(Model model) {

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

            Model model
    ) {

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
    public String nuevoUsuario(Model model) {

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
            @PathVariable("id") Integer id,
            Model model
    ) {

        model.addAttribute(
                "titulo",
                "Editar usuario"
        );


        model.addAttribute(
                "usuario",
                usuarioRepository
                        .findById(id)
                        .orElseThrow()
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
            Usuario usuario,

            @RequestParam("idRol")
            Integer idRol,

            Model model
    ) {

        Rol rol =
                rolRepository
                        .findById(idRol)
                        .orElseThrow();


        usuario.setRol(rol);


        Usuario guardado =
                usuarioRepository.save(usuario);


        /*
         * Modelo final:
         * un Usuario puede tener como máximo
         * un perfil Colaborador.
         */

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
                "Guardar usuario",
                "Administración",
                "Se registró o actualizó la cuenta de "
                        + guardado.getNombreCompleto()
        );


        cargarUsuarios(
                model,
                usuarioRepository.findAll(),
                null
        );


        model.addAttribute(
                "mensajeExito",
                "Usuario guardado correctamente."
        );


        return "admin/usuarios";
    }


    /* =========================================================
       CAMBIAR ESTADO USUARIO
       ========================================================= */

    // Baja lógica:
    // evita borrar físicamente registros relacionados.

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstadoUsuario(
            @PathVariable Integer id,
            Model model
    ) {

        Usuario usuario =
                usuarioRepository
                        .findById(id)
                        .orElseThrow();


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
    public String roles(Model model) {

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

            Model model
    ) {

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
            Model model
    ) {

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
            @PathVariable("id")
            Integer id,

            Model model
    ) {

        model.addAttribute(
                "titulo",
                "Editar habilidad"
        );


        model.addAttribute(
                "habilidad",
                habilidadRepository
                        .findById(id)
                        .orElseThrow()
        );


        return "admin/habilidad-form";
    }


    /* =========================================================
       GUARDAR HABILIDAD
       ========================================================= */

    @PostMapping("/habilidades/guardar")
    public String guardarHabilidad(
            Habilidad habilidad,
            Model model
    ) {

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
            @PathVariable Integer id,
            Model model
    ) {

        Habilidad habilidad =
                habilidadRepository
                        .findById(id)
                        .orElseThrow();


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
            Model model
    ) {

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

            Model model
    ) {

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
            Model model
    ) {

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