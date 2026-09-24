package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;

    public HomeController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        prepararLogin(model, null, false, null, null);
        return "login";
    }

    @PostMapping("/acceder")
    public String entrar(@RequestParam("rol") String rol,
                         @RequestParam("correo") String correo,
                         @RequestParam("password") String password,
                         HttpSession session,
                         Model model) {

        String rolDb = convertirRolBd(rol);
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correo.trim());

        if (encontrado.isEmpty()) {
            prepararLogin(model, rol, true, correo, "No encontramos una cuenta con ese correo.");
            return "login";
        }

        Usuario usuario = encontrado.get();

        if (usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            prepararLogin(model, rol, true, correo, "La cuenta no se encuentra activa.");
            return "login";
        }

        if (usuario.getRol() == null || !rolDb.equals(usuario.getRol().getNombre())) {
            prepararLogin(model, rol, true, correo,
                    "La cuenta no pertenece al perfil seleccionado. Verifica el rol e inténtalo nuevamente.");
            return "login";
        }

        // Para este avance académico los datos demo mantienen texto simple en password_hash.
        // BCrypt/Spring Security queda para una etapa posterior del curso.
        if (usuario.getPasswordHash() == null || !usuario.getPasswordHash().equals(password)) {
            prepararLogin(model, rol, true, correo, "La contraseña ingresada no es correcta.");
            return "login";
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        session.setAttribute("usuarioSesionId", usuario.getIdUsuario());
        session.setAttribute("rolVista", rol);

        return switch (rol) {
            case "ADMIN" -> "forward:/admin/inicio";
            case "PM" -> "forward:/pm/inicio";
            case "RM" -> "forward:/resource/inicio";
            default -> "forward:/colaborador/inicio";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, Model model) {
        session.invalidate();
        prepararLogin(model, null, false, null, null);
        return "login";
    }

    private String convertirRolBd(String rol) {
        return switch (rol) {
            case "ADMIN" -> "ADMINISTRADOR";
            case "PM" -> "PROJECT_MANAGER";
            case "RM" -> "RESOURCE_MANAGER";
            default -> "COLABORADOR";
        };
    }

    private void prepararLogin(Model model,
                               String rol,
                               boolean mostrarCredenciales,
                               String correo,
                               String error) {
        model.addAttribute("titulo", "Iniciar sesión");
        model.addAttribute("rolSeleccionado", rol == null ? "COL" : rol);
        model.addAttribute("mostrarCredenciales", mostrarCredenciales);
        model.addAttribute("correoIngresado", correo == null ? "" : correo);
        model.addAttribute("errorLogin", error);
    }
}
