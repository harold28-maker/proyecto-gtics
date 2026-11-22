package pe.edu.pucp.skillbridge.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CorreoCuentaServiceTest {

    private static final String REMITENTE = "donotreply@envios.example.test";

    @ParameterizedTest
    @ValueSource(strings = {"prueba@gmail.com", "prueba@hotmail.com", "prueba@outlook.com", "prueba@correo.empresa.example.test"})
    void usaElRemitenteAdministradoYElCorreoIndicadoComoDestinatario(String destinatario) throws Exception {
        var smtp = mock(JavaMailSender.class);
        var mensaje = new JavaMailSenderImpl().createMimeMessage();
        when(smtp.createMimeMessage()).thenReturn(mensaje);

        var correo = crearServicio(smtp, REMITENTE);
        var resultado = correo.enviarBienvenida(destinatario, "Ana Prueba", "Colaborador", "Temporal42");

        assertEquals(CorreoCuentaService.ResultadoEnvio.ACEPTADO, resultado);
        var de = (InternetAddress) mensaje.getFrom()[0];
        assertEquals(REMITENTE, de.getAddress());
        assertEquals("SkillBridge AI", de.getPersonal());
        var para = mensaje.getRecipients(Message.RecipientType.TO);
        assertEquals(1, para.length);
        assertEquals(destinatario, ((InternetAddress) para[0]).getAddress());
        assertEquals("auto-generated", mensaje.getHeader("Auto-Submitted", null));
        mensaje.saveChanges();
        String texto = textoPlano(mensaje);
        assertTrue(texto.contains("Contraseña temporal: Temporal42"));
        assertTrue(texto.contains("Este correo es automático."));
        verify(smtp).send(mensaje);
    }

    @Test
    void distingueConfiguracionPendienteDeEnvioRechazado() {
        var smtp = mock(JavaMailSender.class);
        var correo = crearServicio(smtp, "usuario-smtp-sin-direccion");
        assertEquals(CorreoCuentaService.ResultadoEnvio.SIN_CONFIGURACION,
                correo.enviarBienvenida("prueba@example.test", "Ana Prueba", "Colaborador", "Temporal42"));
        verifyNoInteractions(smtp);

        var mensaje = new JavaMailSenderImpl().createMimeMessage();
        when(smtp.createMimeMessage()).thenReturn(mensaje);
        doThrow(new MailAuthenticationException("Autenticación de prueba rechazada")).when(smtp).send(mensaje);
        var configurado = crearServicio(smtp, REMITENTE);
        assertEquals(CorreoCuentaService.ResultadoEnvio.ERROR_AUTENTICACION,
                configurado.enviarBienvenida("prueba@example.test", "Ana Prueba", "Colaborador", "Temporal42"));
    }

    private CorreoCuentaService crearServicio(JavaMailSender smtp, String remitente) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("smtp", smtp);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var plantillas = new SpringTemplateEngine();
        plantillas.setTemplateResolver(resolver);
        return new CorreoCuentaService(beans.getBeanProvider(JavaMailSender.class), plantillas, remitente,
                "http://localhost:8080");
    }

    private String textoPlano(Part parte) throws Exception {
        if (parte.isMimeType("text/plain")) {
            return (String) parte.getContent();
        }
        if (parte.isMimeType("multipart/*") && parte.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String texto = textoPlano(multipart.getBodyPart(i));
                if (!texto.isEmpty()) {
                    return texto;
                }
            }
        }
        return "";
    }
}
