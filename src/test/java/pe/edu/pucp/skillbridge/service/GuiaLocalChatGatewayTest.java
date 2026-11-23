package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiaLocalChatGatewayTest {
    private final GuiaLocalChatGateway guia = new GuiaLocalChatGateway();

    @Test void noSeHacePasarPorGemini() {
        String resultado = guia.generar("Contexto", "¿Dónde están mis proyectos?");
        assertTrue(resultado.contains("sin Gemini"));
        assertTrue(resultado.contains("Proyectos"));
    }

    @Test void ayudaSobreIdentidadIndicaElProveedorYElToken() {
        String resultado = guia.generar("Contexto", "¿Dónde consulto mi DNI?");
        assertTrue(resultado.contains("API Perú"));
        assertTrue(resultado.contains("token configurado en el servidor"));
    }
}
