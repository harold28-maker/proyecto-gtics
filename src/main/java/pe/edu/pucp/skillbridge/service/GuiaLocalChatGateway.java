package pe.edu.pucp.skillbridge.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.Locale;

/**
 * Modo de demostración SIN IA ni consumo externo.
 * Se activa por defecto para permitir ejecutar la aplicación sin GEMINI_API_KEY.
 * Las respuestas solo orientan a navegar y nunca simulan consulta a Gemini.
 */
@Service
@ConditionalOnProperty(name = "skillbridge.chatbot.provider", havingValue = "local", matchIfMissing = true)
public class GuiaLocalChatGateway implements AiChatGateway {
    @Override
    public String generar(String contexto, String pregunta) {
        String texto = pregunta.toLowerCase(Locale.ROOT);
        String inicio = "Estás usando la guía local (sin Gemini). ";
        if (texto.contains("mensaje") || texto.contains("chat")) {
            return inicio + "En el menú de Colaborador, abre Chat para comunicarte con tu equipo.";
        }
        if (texto.contains("proyecto") || texto.contains("asignaci")) {
            return inicio + "Revisa los módulos Proyectos y Asignaciones en el menú de tu rol. "
                   + "Para consultar y resumir automáticamente tus datos, configura Gemini en el servidor.";
        }
        if (texto.contains("habilidad") || texto.contains("certific")) {
            return inicio + "Encontrarás tus habilidades y certificaciones en el menú de Colaborador. "
                   + "Un administrador puede gestionar el catálogo general de habilidades.";
        }
        if (texto.contains("reporte") || texto.contains("dashboard") || texto.contains("gráfic")) {
            return inicio + "Abre Reportes desde tu rol para consultar indicadores y exportar los datos disponibles.";
        }
        if (texto.contains("dni") || texto.contains("reniec") || texto.contains("identidad")) {
            return inicio + "En Administrador, abre Usuarios y selecciona Nuevo usuario para consultar el DNI. "
                   + "La consulta usa API Perú y requiere un token configurado en el servidor.";
        }
        return inicio + "Puedo orientarte sobre Proyectos, Asignaciones, Habilidades, Reportes, "
               + "Chat de Colaborador y registro de usuarios. Para respuestas personalizadas habilita Gemini.";
    }
}
