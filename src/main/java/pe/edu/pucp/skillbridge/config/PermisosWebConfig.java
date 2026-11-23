package pe.edu.pucp.skillbridge.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;
import pe.edu.pucp.skillbridge.service.PermisosRolService;
import pe.edu.pucp.skillbridge.service.PermisosRolService.Permiso;

import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class PermisosWebConfig implements WebMvcConfigurer, HandlerInterceptor {
    private final UsuarioRepository usuarios;
    private final PermisosRolService permisos;

    public PermisosWebConfig(UsuarioRepository usuarios, PermisosRolService permisos) {
        this.usuarios = usuarios;
        this.permisos = permisos;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .addPathPatterns("/admin/**", "/pm/**", "/resource/**", "/colaborador/**", "/foros/**", "/mensajes/**", "/api/chatbot/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;

        HttpSession sesion = request.getSession(false);
        Object id = sesion == null ? null : sesion.getAttribute("usuarioSesionId");
        Usuario cuenta = id instanceof Integer idUsuario ? usuarios.findById(idUsuario).orElse(null) : null;
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (cuenta == null || cuenta.getEstado() != Usuario.EstadoUsuario.ACTIVO || cuenta.getRol() == null
                || autenticacion == null || autenticacion.getAuthorities().stream()
                .noneMatch(autoridad -> autoridad.getAuthority().equals("ROLE_" + cuenta.getRol().getNombre()))) {
            if (sesion != null) sesion.invalidate();
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String rol = cuenta.getRol().getNombre();
        Set<String> bloqueados = permisos.deshabilitados(cuenta.getRol().getIdRol());
        request.setAttribute("usuarioVerificado", cuenta);
        request.setAttribute("permisosBloqueados", bloqueados);

        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String ruta = patron == null ? request.getServletPath() : patron.toString();
        Permiso permiso = permisos.permisoDeRuta(rol, ruta);
        verificar(permiso, bloqueados, cuenta);
        if (permiso != null && permiso.codigo().equals("reportes")) {
            String tipo = ruta.endsWith("/reportes") ? request.getParameter("reporte") : request.getParameter("tipo");
            verificar(permisos.permisoDeReporte(rol, tipo), bloqueados, cuenta);
        }
        return true;
    }

    private void verificar(Permiso permiso, Set<String> bloqueados, Usuario cuenta)
            throws ModelAndViewDefiningException {
        if (permiso != null && permiso.configurable() && bloqueados.contains(permiso.codigo())) {
            String vistaRol = switch (cuenta.getRol().getNombre()) {
                case "ADMINISTRADOR" -> "ADMIN";
                case "PROJECT_MANAGER" -> "PM";
                case "RESOURCE_MANAGER" -> "RM";
                default -> "COL";
            };
            throw new ModelAndViewDefiningException(new ModelAndView("permiso-deshabilitado", Map.of(
                    "titulo", "Permiso deshabilitado",
                    "permiso", permiso.accion(),
                    "rolSesion", vistaRol,
                    "usuarioSesion", cuenta,
                    "permisosBloqueados", bloqueados
            ), HttpStatus.FORBIDDEN));
        }
    }
}
