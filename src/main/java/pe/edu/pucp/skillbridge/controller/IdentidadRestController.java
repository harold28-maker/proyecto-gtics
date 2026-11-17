package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.skillbridge.dto.DniConsultaRequest;
import pe.edu.pucp.skillbridge.dto.DniConsultaResponse;
import pe.edu.pucp.skillbridge.dto.DniValidacionRequest;
import pe.edu.pucp.skillbridge.dto.DniValidacionResponse;
import pe.edu.pucp.skillbridge.service.ConsultaDniService;

import java.util.Map;

/**
 * PPT Clase 9.2: @RestController, JSON, POST y codigos HTTP.
 * El DNI se manda en el body, NO en la URL.
 */
/**
 * Expone servicios REST para consultar y validar la información de identidad.
 * La ruta base del controlador es {@code /api/identidad}.
 */
@RestController
@RequestMapping(path = "/api/identidad", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentidadRestController {

    private final ConsultaDniService consultaDniService;

    /**
     * Construye el controlador con el servicio utilizado para las operaciones de identidad.
     *
     * @param consultaDniService Servicio encargado de consultar y validar DNI.
     */
    public IdentidadRestController(ConsultaDniService consultaDniService) {
        this.consultaDniService = consultaDniService;
    }

    /**
     * Atiende solicitudes GET a la ruta {@code /api/identidad/estado}.
     *
     * @return Mapa con el nombre del servicio y su estado operativo.
     */
    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "identidad", "estado", "ok");
    }

    /**
     * Atiende solicitudes POST a la ruta {@code /api/identidad/consultar}.
     *
     * @param request Datos validados que contienen el DNI que se consulta.
     * @return Resultado de la consulta de identidad.
     */
    @PostMapping(path = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniConsultaResponse consultar(@Valid @RequestBody DniConsultaRequest request) {
        return consultaDniService.consultar(request.dni());
    }

    /**
     * Atiende solicitudes POST a la ruta {@code /api/identidad/validar}.
     *
     * @param request Datos validados utilizados para efectuar la validación de identidad.
     * @return Resultado de la validación solicitada.
     */
    @PostMapping(path = "/validar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniValidacionResponse validar(@Valid @RequestBody DniValidacionRequest request) {
        return consultaDniService.validar(request);
    }
}
