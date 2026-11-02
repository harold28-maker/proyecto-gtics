package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequest(
        @NotBlank(message = "Escribe una pregunta para el asistente")
        @Size(max = 1000, message = "La pregunta puede tener como máximo 1000 caracteres")
        String mensaje
) {
}
