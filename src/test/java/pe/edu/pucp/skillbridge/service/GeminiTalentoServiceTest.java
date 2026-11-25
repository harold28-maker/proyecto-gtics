package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiTalentoServiceTest {
    private MockRestServiceServer servidor;
    private final GeminiTalentoService servicio = new GeminiTalentoService(
            new RestTemplateBuilder().additionalCustomizers(
                    cliente -> servidor = MockRestServiceServer.bindTo(cliente).build()),
            "clave-de-prueba", "modelo-de-prueba");

    @Test
    void interpretaLaParteDeTextoAunqueNoTraigaElIndicadorThought() {
        prepararRespuesta("""
                {"cantidad":2,"criterios":[{"tipo":"HABILIDAD","termino":"Docker","obligatorio":true}],
                 "pideAclaracion":false}
                """);

        var resultados = servicio.interpretar("Necesito dos perfiles con Docker obligatorio", null, List.of("Docker"));

        assertEquals(2, resultados.cantidad());
        assertEquals("Docker", resultados.criterios().get(0).termino());
        assertTrue(resultados.criterios().get(0).obligatorio());
        servidor.verify();
    }

    @Test
    void rechazaUnPorcentajeFraccionarioDeLaConsulta() {
        prepararRespuesta("""
                {"cantidad":1,"criterios":[],"dedicacion":30.5,"pideAclaracion":false}
                """);

        var error = assertThrows(GeminiTalentoService.ConsultaFallida.class,
                () -> servicio.interpretar("Busca a alguien con dedicación del 30,5 %", null, List.of()));

        assertTrue(error.getMessage().contains("porcentaje entero"));
        servidor.verify();
    }

    private void prepararRespuesta(String criterios) {
        String cuerpo = JsonMapper.builder().build().writeValueAsString(Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(
                        Map.of("thought", true, "text", "Texto de razonamiento"),
                        Map.of("text", criterios)))))));
        servidor.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/modelo-de-prueba:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON));
    }
}
