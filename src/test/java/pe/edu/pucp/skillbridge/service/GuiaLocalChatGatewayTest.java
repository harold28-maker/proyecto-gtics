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

    @Test void ayudaSobreIdentidadNoPrometeReniec() {
        String resultado = guia.generar("Contexto", "¿Dónde consulto mi DNI?");
        assertTrue(resultado.contains("VerificaID") || resultado.contains("proveedor autorizado"));
        assertTrue(resultado.contains("no es una conexión directa a RENIEC"));
    }
}
