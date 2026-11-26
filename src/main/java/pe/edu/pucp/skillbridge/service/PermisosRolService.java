package pe.edu.pucp.skillbridge.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.entity.PermisoDeshabilitado;
import pe.edu.pucp.skillbridge.entity.Rol;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository;
import pe.edu.pucp.skillbridge.repository.PermisoDeshabilitadoRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermisosRolService {
    public record Permiso(String codigo, String modulo, String accion, String alcance,
                          boolean configurable, List<String> rutas) {
        boolean corresponde(String ruta) {
            return rutas.stream().anyMatch(base -> ruta.equals(base) || ruta.startsWith(base + "/"));
        }
    }

    private static final Map<String, List<Permiso>> CATALOGO = Map.of(
            "ADMINISTRADOR", List.of(
                    new Permiso("usuarios", "Usuarios", "Gestionar usuarios", "Cuentas de SkillBridge AI", true,
                            List.of("/admin/usuarios")),
                    new Permiso("roles", "Administración", "Configurar roles y permisos", "Configuración de los roles del sistema", false,
                            List.of("/admin/roles")),
                    new Permiso("habilidades", "Catálogo", "Gestionar habilidades", "Catálogo de habilidades", true,
                            List.of("/admin/habilidades")),
                    new Permiso("categorias", "Catálogo", "Gestionar categorías", "Categorías del catálogo", true,
                            List.of("/admin/categorias")),
                    new Permiso("auditoria", "Auditoría", "Consultar auditoría", "Registro de acciones del sistema", true,
                            List.of("/admin/auditoria")),
                    new Permiso("monitoreo", "Monitoreo", "Consultar monitoreo", "Accesos y actividad reciente", true,
                            List.of("/admin/monitoreo")),
                    new Permiso("reportes", "Reportes", "Consultar y descargar reportes", "Reportes administrativos habilitados", true,
                            List.of("/admin/reportes"))
            ),
            "PROJECT_MANAGER", List.of(
                    new Permiso("proyectos", "Proyectos", "Gestionar proyectos", "Proyectos a su cargo", true,
                            List.of("/pm/proyectos")),
                    new Permiso("asignaciones", "Asignaciones", "Gestionar asignaciones", "Proyectos a su cargo", true,
                            List.of("/pm/asignaciones")),
                    new Permiso("colaboradores", "Talento", "Consultar colaboradores", "Colaboradores de la organización", true,
                            List.of("/pm/colaboradores")),
                    new Permiso("disponibilidad", "Talento", "Consultar disponibilidad", "Panel de disponibilidad del equipo", true,
                            List.of("/pm/disponibilidad")),
                    new Permiso("recomendaciones", "Talento", "Consultar recomendaciones", "Proyectos a su cargo", true,
                            List.of("/pm/recomendaciones")),
                    new Permiso("foros", "Colaboración", "Gestionar foros de proyectos", "Proyectos a su cargo", true,
                            List.of("/pm/foros", "/foros")),
                    new Permiso("reportes", "Reportes", "Consultar y descargar reportes", "Reportes de proyectos habilitados", true,
                            List.of("/pm/reportes")),
                    new Permiso("historial", "Proyectos", "Consultar historial de proyectos", "Proyectos a su cargo", true,
                            List.of("/pm/historial"))
            ),
            "RESOURCE_MANAGER", List.of(
                    new Permiso("colaboradores", "Talento", "Consultar colaboradores", "Colaboradores de la organización", true,
                            List.of("/resource/colaboradores")),
                    new Permiso("asignaciones", "Asignaciones", "Gestionar asignaciones", "Asignaciones de los proyectos", true,
                            List.of("/resource/asignaciones")),
                    new Permiso("capacidad", "Capacidad", "Consultar carga y disponibilidad", "Paneles de carga y disponibilidad", true,
                            List.of("/resource/disponibilidad", "/resource/carga")),
                    new Permiso("talento", "Talento", "Consultar Talent Matching", "Candidatos y recomendaciones", true,
                            List.of("/resource/talent")),
                    new Permiso("reportes", "Reportes", "Consultar y descargar reportes", "Reportes de recursos habilitados", true,
                            List.of("/resource/reportes")),
                    new Permiso("historial", "Proyectos", "Consultar historial de proyectos", "Historial de proyectos de la organización", true,
                            List.of("/resource/historial"))
            ),
            "COLABORADOR", List.of(
                    new Permiso("perfil", "Perfil", "Consultar y editar perfil propio", "Su propia cuenta", true,
                            List.of("/colaborador/perfil")),
                    new Permiso("certificaciones", "Perfil", "Gestionar certificaciones propias", "Su propio perfil", true,
                            List.of("/colaborador/certificaciones")),
                    new Permiso("habilidades", "Habilidades", "Gestionar habilidades propias", "Su propio perfil", true,
                            List.of("/colaborador/habilidades")),
                    new Permiso("proyectos", "Proyectos", "Consultar proyectos asignados", "Proyectos en los que participa", true,
                            List.of("/colaborador/proyectos")),
                    new Permiso("historial", "Proyectos", "Consultar historial de proyectos", "Su historial", true,
                            List.of("/colaborador/historial")),
                    new Permiso("asignaciones", "Asignaciones", "Consultar asignaciones propias", "Sus propias asignaciones", true,
                            List.of("/colaborador/asignaciones")),
                    new Permiso("foros", "Colaboración", "Participar en foros", "Proyectos en los que participa", true,
                            List.of("/colaborador/foros", "/foros")),
                    new Permiso("chat", "Colaboración", "Participar en el chat", "Proyectos en los que participa", true,
                            List.of("/colaborador/chat")),
                    new Permiso("asistente", "Colaboración", "Consultar el asistente de IA", "Publicaciones autorizadas", true,
                            List.of("/colaborador/asistente")),
                    new Permiso("notificaciones", "Notificaciones", "Consultar notificaciones", "Sus propias notificaciones", true,
                            List.of("/colaborador/notificaciones"))
            )
    );

    private static final Map<String, Map<String, String>> PERMISOS_REPORTE = Map.of(
            "ADMINISTRADOR", Map.of("usuarios", "usuarios", "habilidades", "habilidades",
                    "auditoria", "auditoria", "dedicacion", "usuarios"),
            "PROJECT_MANAGER", Map.of("proyectos", "proyectos", "asignaciones", "asignaciones",
                    "carga", "disponibilidad", "historial", "historial"),
            "RESOURCE_MANAGER", Map.of("capacidad", "capacidad", "asignaciones", "asignaciones",
                    "talento", "talento", "proyectos", "historial")
    );

    private final PermisoDeshabilitadoRepository accesos;
    private final AuditoriaRepository auditoria;
    private final EntityManager entidades;

    public PermisosRolService(PermisoDeshabilitadoRepository accesos, AuditoriaRepository auditoria,
                              EntityManager entidades) {
        this.accesos = accesos;
        this.auditoria = auditoria;
        this.entidades = entidades;
    }

    public List<Permiso> catalogo(String rol) {
        return rol == null ? List.of() : CATALOGO.getOrDefault(rol, List.of());
    }

    public Set<String> deshabilitados(Integer idRol) {
        return accesos.codigosDeshabilitados(idRol);
    }

    public Permiso permisoDeRuta(String rol, String ruta) {
        return catalogo(rol).stream().filter(permiso -> permiso.corresponde(ruta)).findFirst().orElse(null);
    }

    public Permiso permisoDeReporte(String rol, String tipo) {
        if (tipo == null) return null;
        String codigo = PERMISOS_REPORTE.getOrDefault(rol, Map.of()).get(tipo.toLowerCase(Locale.ROOT));
        return catalogo(rol).stream().filter(permiso -> permiso.codigo().equals(codigo)).findFirst().orElse(null);
    }

    /**
     * Serializa los cambios de cada rol. Un permiso del catálogo está habilitado
     * hasta que el administrador lo deshabilita expresamente.
     */
    @Transactional
    public boolean guardar(Integer idRol, String descripcion, Set<String> habilitados, Usuario administrador) {
        Rol rol = entidades.find(Rol.class, idRol, LockModeType.PESSIMISTIC_WRITE);
        if (rol == null || catalogo(rol.getNombre()).isEmpty()) {
            throw new IllegalArgumentException("El rol seleccionado no tiene una configuración disponible.");
        }

        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (descripcionLimpia != null && descripcionLimpia.length() > 200) {
            throw new IllegalArgumentException("La descripción debe tener como máximo 200 caracteres.");
        }

        List<Permiso> configurables = catalogo(rol.getNombre()).stream().filter(Permiso::configurable).toList();
        Set<String> codigos = configurables.stream().map(Permiso::codigo).collect(Collectors.toSet());
        if (!codigos.containsAll(habilitados)) {
            throw new IllegalArgumentException("Selecciona permisos disponibles para este rol.");
        }

        Map<String, PermisoDeshabilitado> bloqueos = accesos.findByRol_IdRol(idRol).stream()
                .collect(Collectors.toMap(PermisoDeshabilitado::getCodigo, permiso -> permiso));
        boolean huboCambios = false;
        for (Permiso permiso : configurables) {
            PermisoDeshabilitado bloqueo = bloqueos.get(permiso.codigo());
            boolean habilitar = habilitados.contains(permiso.codigo());
            if (habilitar && bloqueo != null) {
                accesos.delete(bloqueo);
                registrar(administrador, rol, "Habilitar permiso", permiso.accion());
                huboCambios = true;
            } else if (!habilitar && bloqueo == null) {
                accesos.save(new PermisoDeshabilitado(rol, permiso.codigo()));
                registrar(administrador, rol, "Deshabilitar permiso", permiso.accion());
                huboCambios = true;
            }
        }

        if (!Objects.equals(rol.getDescripcion(), descripcionLimpia)) {
            rol.setDescripcion(descripcionLimpia);
            registrar(administrador, rol, "Actualizar descripción del rol", "Se actualizó la descripción.");
            huboCambios = true;
        }
        return huboCambios;
    }

    private void registrar(Usuario administrador, Rol rol, String accion, String detalle) {
        Auditoria registro = new Auditoria();
        registro.setUsuario(administrador);
        registro.setAccion(accion);
        registro.setModulo("Roles y permisos");
        registro.setDetalle(rol.getNombreVisual() + " · " + detalle);
        auditoria.save(registro);
    }
}
