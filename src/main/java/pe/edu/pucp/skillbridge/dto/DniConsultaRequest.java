package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** DNI enviado desde Postman hacia la API de SkillBridge. */
public record DniConsultaRequest(
    @NotBlank(message = "Ingresa un DNI")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener exactamente 8 digitos")
    String dni
) {}
