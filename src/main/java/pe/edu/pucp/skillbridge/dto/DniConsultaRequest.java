package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Representa la solicitud de consulta de un DNI enviada a la API de SkillBridge.
 *
 * @param dni número de DNI compuesto por ocho dígitos
 */
public record DniConsultaRequest(
    @NotBlank(message = "Ingresa un DNI")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener exactamente 8 digitos")
    String dni
) {}
