package pe.edu.pucp.skillbridge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.DniConsultaResponse;
import pe.edu.pucp.skillbridge.dto.DniValidacionRequest;
import pe.edu.pucp.skillbridge.dto.DniValidacionResponse;

import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PPT Clases 10.1: consumir Web Service REST con RestTemplateBuilder y DTO.
 * NOTA: VerificaID es un proveedor externo, NO una API oficial de RENIEC.
 * Solo consulta; NO guarda datos personales en tablas de SkillBridge.
 */
@Service
public class ConsultaDniService {

    private final RestTemplate restTemplate;
    private final String token;
    private final String apiUrl;

    public ConsultaDniService(
            RestTemplateBuilder builder,
            @Value("${skillbridge.dni.token:${VERIFICAID_TOKEN:}}") String token,
            @Value("${skillbridge.dni.url:${VERIFICAID_URL:https://sandbox-api.verifica.id/v2/consulta/personas}}") String apiUrl) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(12))
                .build();
        this.token = token;
        this.apiUrl = apiUrl;
    }

    public DniConsultaResponse consultar(String dni) {
        if (dni == null || !dni.matches("[0-9]{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El DNI debe tener exactamente 8 digitos");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Consulta DNI no configurada: falta VERIFICAID_TOKEN");
        }

        // La documentacion del proveedor requiere POST multipart/form-data,
        // con el campo dni y Authorization: Bearer <token>.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("dni", dni);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> respuesta;
        try {
            respuesta = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El proveedor no encontro datos para el DNI");
            }
            if (status == 429) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Limite de consultas del proveedor alcanzado");
            }
            // No exponer el cuerpo externo ni el token en el error.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La API externa rechazo la consulta; revisar token o acceso");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con la API externa de identidad");
        }

        Map<?, ?> cuerpo = respuesta.getBody();
        if (cuerpo == null || !(cuerpo.get("data") instanceof Map<?, ?> datos)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La API externa devolvio un JSON inesperado");
        }
        Object status = cuerpo.get("status");
        if (status != null && !"200".equals(status.toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La API externa indico que la consulta no fue exitosa");
        }

        String dniRecibido = leer(datos, "dni");
        String nombres = leer(datos, "nombres");
        String paterno = leer(datos, "apellido_paterno");
        String materno = leer(datos, "apellido_materno");
        if (!dni.equals(dniRecibido) || nombres.isBlank()
                || paterno.isBlank() || materno.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Datos incompletos o DNI diferente en la respuesta externa");
        }

        return new DniConsultaResponse(dniRecibido, nombres, paterno, materno);
    }

    public DniValidacionResponse validar(DniValidacionRequest request) {
        DniConsultaResponse resultado = consultar(request.dni());
        boolean coincide = normalizar(request.nombres()).equals(normalizar(resultado.nombres()))
                && normalizar(request.apellidoPaterno()).equals(normalizar(resultado.apellidoPaterno()))
                && normalizar(request.apellidoMaterno()).equals(normalizar(resultado.apellidoMaterno()));
        return new DniValidacionResponse(coincide,
                coincide ? "Los datos ingresados coinciden con la consulta"
                         : "Los datos ingresados no coinciden con la consulta");
    }

    private static String leer(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
