package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Define las opciones de navegación que comparten el menú lateral y el encabezado.
 */
@Component
public class MenuLateral {

    private static final Map<String, List<Opcion>> MENUS = Map.of(
            "ADMIN", List.of(
                    enlace("/admin/inicio", "Inicio", null, "/admin/dashboard"),
                    enlace("/admin/usuarios", "Usuarios", "usuarios"),
                    enlace("/admin/roles", "Roles y permisos", null),
                    enlace("/admin/habilidades", "Habilidades", "habilidades"),
                    enlace("/admin/categorias", "Categorías", "categorias"),
                    enlace("/admin/auditoria", "Auditoría", "auditoria"),
                    enlace("/admin/monitoreo", "Monitoreo", "monitoreo"),
                    enlace("/admin/reportes", "Reportes", "reportes")
            ),
            "PM", List.of(
                    enlace("/pm/inicio", "Inicio", null, "/pm/dashboard"),
                    enlace("/pm/proyectos", "Proyectos", "proyectos"),
                    enlace("/pm/asignaciones", "Asignaciones", "asignaciones"),
                    enlace("/pm/colaboradores", "Colaboradores", "colaboradores"),
                    enlace("/pm/disponibilidad", "Disponibilidad", "disponibilidad"),
                    enlace("/pm/recomendaciones", "Recomendaciones", "recomendaciones"),
                    enlace("/pm/foros", "Foro", "foros", "/foros"),
                    enlace("/pm/reportes", "Reportes", "reportes"),
                    enlace("/pm/historial", "Historial", "historial")
            ),
            "RM", List.of(
                    enlace("/resource/inicio", "Inicio", null, "/resource/dashboard"),
                    enlace("/resource/colaboradores", "Colaboradores", "colaboradores"),
                    enlace("/resource/asignaciones", "Asignaciones", "asignaciones"),
                    enlace("/resource/disponibilidad", "Disponibilidad", "capacidad"),
                    enlace("/resource/carga", "Carga de trabajo", "capacidad"),
                    enlace("/resource/talent", "Asignación de talento", "talento"),
                    enlace("/resource/reportes", "Reportes", "reportes"),
                    enlace("/resource/historial", "Proyectos anteriores", "historial")
            ),
            "COL", List.of(
                    enlace("/colaborador/inicio", "Inicio", null),
                    enlace("/colaborador/perfil", "Mi perfil", "perfil"),
                    enlace("/colaborador/certificaciones", "Certificaciones", "certificaciones"),
                    enlace("/colaborador/habilidades", "Mis habilidades", "habilidades"),
                    enlace("/colaborador/proyectos", "Mis proyectos", "proyectos"),
                    enlace("/colaborador/historial", "Historial", "historial"),
                    enlace("/colaborador/asignaciones", "Mis asignaciones", "asignaciones"),
                    enlace("/colaborador/foros", "Foro", "foros", "/foros"),
                    enlace("/colaborador/chat", "Chat", "chat"),
                    enlace("/colaborador/asistente", "Asistente de IA", "asistente"),
                    enlace("/colaborador/notificaciones", "Notificaciones", "notificaciones")
            )
    );

    private final HttpServletRequest request;

    public MenuLateral(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Devuelve las secciones que se muestran en el menú lateral del rol.
     */
    public List<Opcion> opciones(String rol) {
        return MENUS.getOrDefault(rol, MENUS.get("COL"));
    }

    /**
     * Identifica la sección a la que pertenece la página solicitada.
     */
    public Opcion actual(String rol) {
        String ruta = request.getServletPath();
        return opciones(rol).stream().filter(opcion -> opcion.coincideCon(ruta)).findFirst().orElse(null);
    }

    private static Opcion enlace(String ruta, String nombre, String permiso, String... alternativas) {
        return new Opcion(ruta, nombre, permiso, List.of(alternativas));
    }

    public record Opcion(String ruta, String nombre, String permiso, List<String> alternativas) {
        private boolean coincideCon(String actual) {
            return pertenece(actual, ruta) || alternativas.stream().anyMatch(alternativa -> pertenece(actual, alternativa));
        }

        private boolean pertenece(String actual, String base) {
            return actual.equals(base) || actual.startsWith(base + "/");
        }
    }
}
