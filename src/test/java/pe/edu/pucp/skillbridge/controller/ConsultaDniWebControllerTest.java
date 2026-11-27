package pe.edu.pucp.skillbridge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.DniConsultaResponse;
import pe.edu.pucp.skillbridge.service.ConsultaDniService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultaDniWebControllerTest {
    private static final String RUTA = "/admin/usuarios/consulta-dni";
    private static final String DNI = "01234567";
    private final ConsultaDniService servicio = mock(ConsultaDniService.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ConsultaDniWebController(servicio)).build();
    }

    @Test
    void devuelveSoloLosCamposDelFormularioYSinCache() throws Exception {
        when(servicio.consultar(DNI)).thenReturn(new DniConsultaResponse(DNI, "María", "González", "Pérez"));

        mvc.perform(post(RUTA).param("dni", DNI).param("consentimiento", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.nombres").value("María"))
                .andExpect(jsonPath("$.apellidos").value("González Pérez"))
                .andExpect(jsonPath("$.dni").doesNotExist());
    }

    @Test
    void sinConfirmacionNoConsultaAlProveedor() throws Exception {
        mvc.perform(post(RUTA).param("dni", DNI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    void comunicaElErrorControladoDelServicio() throws Exception {
        when(servicio.consultar(DNI)).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "API Perú rechazó la consulta."));

        mvc.perform(post(RUTA).param("dni", DNI).param("consentimiento", "true"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("API Perú rechazó la consulta."));
    }
}
