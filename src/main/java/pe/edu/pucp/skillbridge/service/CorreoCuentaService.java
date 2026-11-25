package pe.edu.pucp.skillbridge.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
public class CorreoCuentaService {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoCuentaService.class);
    private static final String LOGO_ID = "skillbridge-logo";
    private static final ClassPathResource LOGO = new ClassPathResource("static/css/img/SkillBridge_AI_Logo.png");
    private static final String NOMBRE_REMITENTE = "SkillBridge AI";
    private static final String NOTA_BIENVENIDA =
            "Este correo es automático. Si necesitas ayuda, comunícate con el administrador.";

    public enum ResultadoEnvio { ACEPTADO, SIN_CONFIGURACION, ERROR_AUTENTICACION, ERROR_ENVIO }

    private final ObjectProvider<JavaMailSender> remitidores;
    private final SpringTemplateEngine plantillas;
    private final String remitente;
    private final String urlBase;

    public CorreoCuentaService(ObjectProvider<JavaMailSender> remitidores, SpringTemplateEngine plantillas,
                               @Value("${skillbridge.correo.remitente}") String remitente,
                               @Value("${skillbridge.public-url}") String urlPublica) {
        this.remitidores = remitidores;
        this.plantillas = plantillas;
        this.remitente = direccionRemitente(remitente);
        this.urlBase = baseConfiable(urlPublica);
    }

    public boolean disponible() {
        return urlBase != null && remitente != null && remitidores.getIfAvailable() != null;
    }

    public ResultadoEnvio enviarBienvenida(String correo, String nombre, String rol, String claveTemporal) {
        if (!disponible()) {
            return ResultadoEnvio.SIN_CONFIGURACION;
        }

        String enlace = urlBase + "/login";
        String seguridad = "Por seguridad, cambia esta contraseña con la opción «¿Olvidaste tu contraseña?» del login.";
        var contenido = Map.<String, Object>of(
                "titulo", "Tu cuenta ya está creada",
                "nombre", nombre,
                "mensaje", "El administrador creó tu cuenta en SkillBridge AI. Estos son tus datos de acceso.",
                "correoCuenta", correo,
                "rol", rol,
                "claveTemporal", claveTemporal,
                "seguridad", seguridad,
                "urlAccion", enlace,
                "accion", "Iniciar sesión",
                "nota", NOTA_BIENVENIDA);
        String texto = """
                Hola, %s.

                El administrador creó tu cuenta en SkillBridge AI.
                Correo institucional: %s
                Rol: %s
                Contraseña temporal: %s

                %s

                Iniciar sesión: %s

                %s
                """.formatted(nombre, correo, rol, claveTemporal, seguridad, enlace, NOTA_BIENVENIDA);
        return enviar(correo, "Tu acceso a SkillBridge AI", contenido, texto);
    }

    public boolean enviarRecuperacion(String correo, String nombre, String token) {
        if (!disponible()) {
            return false;
        }

        String enlace = urlBase + "/restablecer-contrasena?token=" + token;
        String mensaje = "Recibimos una solicitud para cambiar la contraseña de tu cuenta de SkillBridge AI.";
        String nota = "Si no solicitaste el cambio, puedes ignorar este correo.";
        var contenido = Map.<String, Object>of(
                "titulo", "Restablece tu contraseña",
                "nombre", nombre,
                "mensaje", mensaje,
                "urlAccion", enlace,
                "accion", "Crear nueva contraseña",
                "nota", nota);
        String texto = """
                Hola, %s.

                %s

                Crear nueva contraseña: %s

                %s
                """.formatted(nombre, mensaje, enlace, nota);
        return enviar(correo, "Restablece tu contraseña de SkillBridge AI", contenido, texto) == ResultadoEnvio.ACEPTADO;
    }

    private ResultadoEnvio enviar(String destinatario, String asunto, Map<String, Object> contenido, String texto) {
        JavaMailSender mail = remitidores.getObject();
        try {
            var mensaje = mail.createMimeMessage();
            var helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, NOMBRE_REMITENTE);
            mensaje.setHeader("Auto-Submitted", "auto-generated");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            String html = plantillas.process("correos/cuenta", new Context(Locale.forLanguageTag("es-PE"), contenido));
            helper.setText(texto, html);
            helper.addInline(LOGO_ID, LOGO, "image/png");
            mail.send(mensaje);
            return ResultadoEnvio.ACEPTADO;
        } catch (MailAuthenticationException ex) {
            LOG.warn("El servidor SMTP rechazó la autenticación del remitente de SkillBridge AI.");
            return ResultadoEnvio.ERROR_AUTENTICACION;
        } catch (MailException | MessagingException | TemplateEngineException | UnsupportedEncodingException ex) {
            LOG.warn("No se pudo enviar el correo de cuenta ({}).", ex.getClass().getSimpleName());
            return ResultadoEnvio.ERROR_ENVIO;
        }
    }

    private static String direccionRemitente(String valor) {
        try {
            var direccion = new InternetAddress(valor.trim(), true);
            direccion.validate();
            return direccion.isGroup() ? null : direccion.getAddress();
        } catch (AddressException ex) {
            return null;
        }
    }

    private static String baseConfiable(String url) {
        try {
            URI uri = URI.create(url.trim());
            String esquema = uri.getScheme();
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                return null;
            }
            return "https".equalsIgnoreCase(esquema) || "http".equalsIgnoreCase(esquema)
                    ? uri.toASCIIString().replaceAll("/+$", "") : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
