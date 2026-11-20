package pe.edu.pucp.skillbridge.dto;

import java.time.LocalDateTime;

public record ChatbotMessageResponse(
        String remitente,
        String contenido,
        LocalDateTime fecha
) {
}
