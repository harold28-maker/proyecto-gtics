package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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


    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();


    public HomeController(
            UsuarioRepository usuarioRepository
    ) {

        this.usuarioRepository =
                usuarioRepository;
    }


    /* =========================================================
       INICIO
       ========================================================= */

    @GetMapping("/")
    public String index() {

        return "index";
    }


    /* =========================================================
       LOGIN
       ========================================================= */

    @GetMapping("/login")
    public String login(
            Model model
    ) {

        prepararLogin(
                model,
                null,
                null
        );

        return "login";
    }


    /* =========================================================
       ACCEDER
       ========================================================= */

    @PostMapping("/acceder")
    public String entrar(

            @RequestParam("correo")
            String correo,

            @RequestParam("password")
            String password,

            HttpSession session,

            Model model
    ) {


        /* VALIDAR CAMPOS */

        if (
                correo == null
                        ||
                        correo.isBlank()
                        ||
                        password == null
                        ||
                        password.isBlank()
        ) {

            prepararLogin(
                    model,
                    correo,
                    "Ingresa el correo y la contraseña."
            );

            return "login";
        }


        String correoLimpio =
                correo.trim();


        /* BUSCAR USUARIO */

        Optional<Usuario> encontrado =
                usuarioRepository
                        .findByCorreoIgnoreCase(
                                correoLimpio
                        );


        if (encontrado.isEmpty()) {

            prepararLogin(
                    model,
                    correoLimpio,
                    "Correo o contraseña incorrectos."
            );

            return "login";
        }


        Usuario usuario =
                encontrado.get();


        /* VALIDAR ESTADO */

        if (
                usuario.getEstado()
                        != Usuario.EstadoUsuario.ACTIVO
        ) {

            prepararLogin(
                    model,
                    correoLimpio,
                    "La cuenta no se encuentra activa."
            );

            return "login";
        }


        /* VALIDAR ROL */

        if (
                usuario.getRol() == null
                        ||
                        usuario.getRol()
                                .getNombre() == null
        ) {

            prepararLogin(
                    model,
                    correoLimpio,
                    "La cuenta no tiene un perfil de acceso válido."
            );

            return "login";
        }


        /* VALIDAR CONTRASEÑA BCRYPT */

        if (
                usuario.getPasswordHash() == null
                        ||
                        !passwordEncoder.matches(
                                password,
                                usuario.getPasswordHash()
                        )
        ) {

            prepararLogin(
                    model,
                    correoLimpio,
                    "Correo o contraseña incorrectos."
            );

            return "login";
        }


        /* ÚLTIMO ACCESO */

        usuario.setUltimoAcceso(
                LocalDateTime.now()
        );


        usuarioRepository.save(
                usuario
        );


        /* SESIÓN */

        session.setAttribute(
                "usuarioSesionId",
                usuario.getIdUsuario()
        );


        String rolDb =
                usuario.getRol()
                        .getNombre();


        String rolVista =
                convertirRolVista(
                        rolDb
                );


        session.setAttribute(
                "rolVista",
                rolVista
        );


        /* REDIRECCIÓN SEGÚN ROL */

        return switch (rolDb) {

            case "ADMINISTRADOR" ->
                    "redirect:/admin/inicio";

            case "PROJECT_MANAGER" ->
                    "redirect:/pm/inicio";

            case "RESOURCE_MANAGER" ->
                    "redirect:/resource/inicio";

            case "COLABORADOR" ->
                    "redirect:/colaborador/inicio";

            default -> {

                session.invalidate();


                prepararLogin(
                        model,
                        correoLimpio,
                        "La cuenta no tiene un perfil de acceso válido."
                );


                yield "login";
            }
        };
    }


    /* =========================================================
       LOGOUT
       ========================================================= */

    @GetMapping("/logout")
    public String logout(

            HttpSession session,

            Model model
    ) {

        session.invalidate();


        prepararLogin(
                model,
                null,
                null
        );


        return "login";
    }


    /* =========================================================
       ROL PARA LAS VISTAS
       ========================================================= */

    private String convertirRolVista(
            String rolDb
    ) {

        return switch (rolDb) {

            case "ADMINISTRADOR" ->
                    "ADMIN";

            case "PROJECT_MANAGER" ->
                    "PM";

            case "RESOURCE_MANAGER" ->
                    "RM";

            default ->
                    "COL";
        };
    }


    /* =========================================================
       PREPARAR LOGIN
       ========================================================= */

    private void prepararLogin(

            Model model,

            String correo,

            String error
    ) {

        model.addAttribute(
                "titulo",
                "Iniciar sesión"
        );


        model.addAttribute(
                "correoIngresado",
                correo == null
                        ? ""
                        : correo
        );


        model.addAttribute(
                "errorLogin",
                error
        );
    }

}