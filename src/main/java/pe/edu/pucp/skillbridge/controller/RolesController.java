package pe.edu.pucp.skillbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.RolRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;
import pe.edu.pucp.skillbridge.service.PermisosRolService;
import pe.edu.pucp.skillbridge.service.PermisosRolService.Permiso;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/roles")
public class RolesController {
    public record Resumen(Rol rol, long usuarios, List<Permiso> permisos, Set<String> deshabilitados) {
        public boolean habilitado(Permiso permiso) {
            return !permiso.configurable() || !deshabilitados.contains(permiso.codigo());
        }

        public long totalHabilitados() {
            return permisos.stream().filter(this::habilitado).count();
        }
    }

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PermisosRolService permisos;

    public RolesController(RolRepository roles, UsuarioRepository usuarios, PermisosRolService permisos) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.permisos = permisos;
    }

    @GetMapping
    public String consultar(@RequestParam(value = "rol", required = false) Integer rolSolicitado,
                            @RequestParam(value = "editar", defaultValue = "false") boolean editar,
                            Model model) {
        List<Resumen> lista = roles.findAll().stream()
                .map(rol -> new Resumen(rol, usuarios.countByRol_IdRol(rol.getIdRol()),
                        permisos.catalogo(rol.getNombre()), permisos.deshabilitados(rol.getIdRol())))
                .toList();
        Resumen seleccionado = lista.stream()
                .filter(r -> r.rol().getIdRol().equals(rolSolicitado))
                .findFirst().orElse(lista.isEmpty() ? null : lista.get(0));

        model.addAttribute("titulo", "Roles y permisos");
        model.addAttribute("roles", lista);
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("editar", editar && seleccionado != null && !seleccionado.permisos().isEmpty());
        model.addAttribute("totalUsuarios", lista.stream().mapToLong(Resumen::usuarios).sum());
        model.addAttribute("totalPermisos", lista.stream().mapToLong(Resumen::totalHabilitados).sum());
        return "admin/roles";
    }

    @PostMapping("/{id}/guardar")
    public String guardar(@PathVariable("id") Integer id,
                          @RequestParam(value = "descripcion", required = false) String descripcion,
                          @RequestParam(value = "habilitados", required = false) List<String> habilitados,
                          @RequestAttribute("usuarioVerificado") Usuario administrador,
                          RedirectAttributes redirect) {
        redirect.addAttribute("rol", id);
        try {
            boolean huboCambios = permisos.guardar(id, descripcion,
                    habilitados == null ? Set.of() : new HashSet<>(habilitados), administrador);
            redirect.addFlashAttribute("mensajeRol", huboCambios
                    ? "Se guardaron los cambios del rol."
                    : "La configuración del rol está guardada.");
            redirect.addFlashAttribute("verAuditoria", huboCambios);
        } catch (IllegalArgumentException ex) {
            redirect.addAttribute("editar", true);
            redirect.addFlashAttribute("errorRol", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
