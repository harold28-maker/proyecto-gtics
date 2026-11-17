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
@RestController
@RequestMapping(path = "/api/identidad", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentidadRestController {

    private final ConsultaDniService consultaDniService;

    public IdentidadRestController(ConsultaDniService consultaDniService) {
        this.consultaDniService = consultaDniService;
    }

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "identidad", "estado", "ok");
    }

    @PostMapping(path = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniConsultaResponse consultar(@Valid @RequestBody DniConsultaRequest request) {
        return consultaDniService.consultar(request.dni());
    }

    @PostMapping(path = "/validar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniValidacionResponse validar(@Valid @RequestBody DniValidacionRequest request) {
        return consultaDniService.validar(request);
    }
}
