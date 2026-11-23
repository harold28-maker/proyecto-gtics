package pe.edu.pucp.skillbridge.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.DniConsultaResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** Pruebas sin API Key y sin llamar a la API de un tercero. */
class ConsultaDniServiceTest {

    private static final String URL = "https://api.apiperu.pe/dni";

    private final ConsultaDniService servicio = new ConsultaDniService(
            new RestTemplateBuilder(), "", URL);

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
    void consultaEnviaJsonConBearerYTraduceNumeroAlDniDelProyecto() {
        ConsultaDniService consulta = new ConsultaDniService(
                new RestTemplateBuilder().additionalCustomizers(cliente ->
                        MockRestServiceServer.bindTo(cliente).build()
                                .expect(requestTo(URL))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-de-prueba"))
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(content().json("""
                                        {"dni":"00000001"}
                                        """))
                                .andRespond(withSuccess("""
                                        {
                                          "success": true,
                                          "data": {
                                            "numero": "00000001",
                                            "nombres": "NOMBRE DE PRUEBA",
                                            "apellido_paterno": "PATERNO",
                                            "apellido_materno": "MATERNO",
                                            "direccion": "DIRECCIÓN DE PRUEBA"
                                          }
                                        }
                                        """, MediaType.APPLICATION_JSON))),
                "token-de-prueba", URL);

        assertEquals(new DniConsultaResponse("00000001", "NOMBRE DE PRUEBA", "PATERNO", "MATERNO"),
                consulta.consultar("00000001"));
    }

    @Test
    void respuestaConDniDistintoEsRechazada() {
        ConsultaDniService consulta = new ConsultaDniService(
                new RestTemplateBuilder().additionalCustomizers(cliente ->
                        MockRestServiceServer.bindTo(cliente).build()
                                .expect(requestTo(URL)).andRespond(withSuccess("""
                                        {
                                          "success": true,
                                          "data": {
                                            "numero": "00000002",
                                            "nombres": "NOMBRE DE PRUEBA",
                                            "apellido_paterno": "PATERNO",
                                            "apellido_materno": "MATERNO"
                                          }
                                        }
                                        """, MediaType.APPLICATION_JSON))),
                "token-de-prueba", URL);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> consulta.consultar("00000001"));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
    }

    @Test
    void accesoDenegadoPorProveedorIndicaQueFaltaHabilitarLaConsulta() {
        ConsultaDniService consulta = new ConsultaDniService(
                new RestTemplateBuilder().additionalCustomizers(cliente ->
                        MockRestServiceServer.bindTo(cliente).build()
                                .expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN))),
                "token-de-prueba", URL);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> consulta.consultar("00000000"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertEquals("API Perú denegó el acceso. Revisa los permisos de Consulta DNI en la cuenta.",
                error.getReason());
    }
}
