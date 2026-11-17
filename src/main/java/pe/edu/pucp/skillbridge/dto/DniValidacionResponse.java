package pe.edu.pucp.skillbridge.dto;

/**
 * Representa el resultado de comparar los datos personales con la información consultada.
 *
 * @param coincide indica si los datos declarados coinciden con los datos consultados
 * @param mensaje descripción del resultado de la validación
 */
public record DniValidacionResponse(boolean coincide, String mensaje) {}
