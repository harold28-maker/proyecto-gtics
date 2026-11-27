package pe.edu.pucp.skillbridge.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.service.ConsultaDniService;

import java.util.Map;

/**
 * Consulta el DNI para completar los datos del formulario de nuevo usuario.
 */
@RestController
public class ConsultaDniWebController {
    private static final String AUTORIZACION_REQUERIDA = "Debes confirmar que tienes autorización para consultar este DNI.";
    private static final String ERROR_CONSULTA = "No se pudo completar la consulta.";
    private final ConsultaDniService service;

    public ConsultaDniWebController(ConsultaDniService service) {
        this.service = service;
    }

    @PostMapping(value = "/admin/usuarios/consulta-dni", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> consultarParaNuevoUsuario(
            @RequestParam(value = "dni", defaultValue = "") String dni,
            @RequestParam(value = "consentimiento", defaultValue = "false") boolean consentimiento) {
        if (!consentimiento) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                    .body(Map.of("error", AUTORIZACION_REQUERIDA));
        }
        try {
            var resultado = service.consultar(dni);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                    "nombres", resultado.nombres(),
                    "apellidos", resultado.apellidoPaterno() + " " + resultado.apellidoMaterno()
            ));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).cacheControl(CacheControl.noStore())
                    .body(Map.of("error", ex.getReason() == null ? ERROR_CONSULTA : ex.getReason()));
        }
    }
}
