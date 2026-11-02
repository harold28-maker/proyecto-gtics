package pe.edu.pucp.skillbridge.dto;

import java.time.LocalDateTime;

public record ChatbotResponse(
        String respuesta,
        String error,
        LocalDateTime fecha
) {
    public static ChatbotResponse exitosa(String respuesta, LocalDateTime fecha) {
        return new ChatbotResponse(respuesta, null, fecha);
    }

    public static ChatbotResponse fallida(String error) {
        return new ChatbotResponse(null, error, LocalDateTime.now());
    }
}
