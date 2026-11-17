package pe.edu.pucp.skillbridge.dto;

/** Resultado de comparar nombres/apellidos con los datos consultados. */
public record DniValidacionResponse(boolean coincide, String mensaje) {}
