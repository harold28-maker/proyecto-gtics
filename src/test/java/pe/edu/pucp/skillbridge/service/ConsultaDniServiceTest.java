package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** Pruebas sin API Key y sin llamar a la API de un tercero. */
class ConsultaDniServiceTest {

    private final ConsultaDniService servicio = new ConsultaDniService(
            new RestTemplateBuilder(), "", "https://sandbox-api.verifica.id/v2/consulta/personas");

    @Test
    void dniDeSieteDigitosDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar("1234567"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void dniConLetrasDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar("1234567X"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void tokenNoConfiguradoDa503() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar("00000000"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    void accesoDenegadoPorProveedorIndicaQueFaltaHabilitarLaConsulta() {
        String url = "https://proveedor.test/consulta";
        ConsultaDniService consulta = new ConsultaDniService(
                new RestTemplateBuilder().additionalCustomizers(cliente ->
                        MockRestServiceServer.bindTo(cliente).build()
                                .expect(requestTo(url)).andRespond(withStatus(HttpStatus.FORBIDDEN))),
                "token-de-prueba", url);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> consulta.consultar("00000000"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertEquals("VerificaID denegó el acceso. Solicita la habilitación de Consulta DNI para la cuenta del proveedor.",
                error.getReason());
    }
}
