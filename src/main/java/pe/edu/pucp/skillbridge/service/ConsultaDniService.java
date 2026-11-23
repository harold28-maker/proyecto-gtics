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
 * Consulta a API Perú como proveedor externo de identidad.
 * Solo consulta; NO guarda datos personales en tablas de SkillBridge.
 */
@Service
public class ConsultaDniService {

    private final RestTemplate restTemplate;
    private final String token;
    private final String apiUrl;

    /**
     * Construye el servicio con el cliente REST configurado y las credenciales
     * necesarias para consultar el proveedor externo de DNI.
     *
     * @param builder constructor utilizado para crear la instancia de RestTemplate
     * @param token token de autorización leído desde la configuración o variable de entorno
     * @param apiUrl URL del servicio externo utilizada para realizar las consultas
     */
    public ConsultaDniService(
            RestTemplateBuilder builder,
            @Value("${skillbridge.dni.token:${APIPERU_TOKEN:}}") String token,
            @Value("${skillbridge.dni.url:${APIPERU_URL:https://api.apiperu.pe/dni}}") String apiUrl) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(12))
                .build();
        this.token = token;
        this.apiUrl = apiUrl;
    }

    /**
     * Valida el formato de un DNI y solicita al proveedor externo los datos
     * personales asociados.
     *
     * @param dni número de DNI que debe contener exactamente ocho dígitos
     * @return respuesta con el DNI y los nombres recibidos del proveedor
     * @throws ResponseStatusException si el DNI es inválido, falta el token,
     *         el proveedor rechaza o no permite alcanzar la consulta, o la
     *         respuesta no contiene los datos esperados
     */
    public DniConsultaResponse consultar(String dni) {
        if (dni == null || !dni.matches("[0-9]{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El DNI debe tener exactamente 8 digitos");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Consulta DNI no configurada: falta APIPERU_TOKEN");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("dni", dni), headers);

        ResponseEntity<Map> respuesta;
        try {
            respuesta = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "API Perú rechazó el token. Revisa APIPERU_TOKEN en el servidor.");
            }
            if (status == 402) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "API Perú requiere pago para esta consulta. Revisa el plan de la cuenta.");
            }
            if (status == 403) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "API Perú denegó el acceso. Revisa los permisos de Consulta DNI en la cuenta.");
            }
            if (status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "API Perú no encontró el DNI o rechazó las credenciales de la consulta.");
            }
            if (status == 429) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Se alcanzó el límite de consultas de API Perú.");
            }
            // No exponer el cuerpo externo ni el token en el error.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "API Perú rechazó la consulta.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo completar la conexión o leer la respuesta de API Perú.");
        }

        Map<?, ?> cuerpo = respuesta.getBody();
        if (cuerpo == null || !Boolean.TRUE.equals(cuerpo.get("success"))
                || !(cuerpo.get("data") instanceof Map<?, ?> datos)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "API Perú no devolvió una consulta exitosa con datos de identidad.");
        }

        String dniRecibido = leer(datos, "numero");
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

    /**
     * Consulta los datos de DNI enviados y verifica si coinciden con los nombres
     * y apellidos proporcionados, aplicando comparaciones normalizadas.
     *
     * @param request datos ingresados por el usuario para realizar la validación
     * @return respuesta que indica si los datos normalizados coinciden
     * @throws ResponseStatusException si la consulta realizada por el DNI falla
     *         o el proveedor no devuelve una respuesta válida
     */
    public DniValidacionResponse validar(DniValidacionRequest request) {
        DniConsultaResponse resultado = consultar(request.dni());
        boolean coincide = normalizar(request.nombres()).equals(normalizar(resultado.nombres()))
                && normalizar(request.apellidoPaterno()).equals(normalizar(resultado.apellidoPaterno()))
                && normalizar(request.apellidoMaterno()).equals(normalizar(resultado.apellidoMaterno()));
        return new DniValidacionResponse(coincide,
                coincide ? "Los datos ingresados coinciden con la consulta"
                         : "Los datos ingresados no coinciden con la consulta");
    }

    /**
     * Obtiene un valor textual de los datos devueltos por el proveedor y elimina
     * los espacios iniciales y finales.
     *
     * @param data mapa que contiene los datos del DNI
     * @param key nombre de la propiedad que se desea leer
     * @return texto asociado a la propiedad o una cadena vacía si no existe
     */
    private static String leer(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString().trim();
    }

    /**
     * Normaliza un texto quitando tildes, unificando espacios y convirtiéndolo
     * a mayúsculas con una configuración independiente de la región.
     *
     * @param texto texto que se desea normalizar
     * @return texto sin tildes, con espacios unificados y en mayúsculas
     */
    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
