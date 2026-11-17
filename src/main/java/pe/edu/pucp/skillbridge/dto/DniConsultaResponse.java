package pe.edu.pucp.skillbridge.dto;

/** DTO: recibimos JSON del proveedor y devolvemos solo campos necesarios. */
public record DniConsultaResponse(
    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno
) {}
