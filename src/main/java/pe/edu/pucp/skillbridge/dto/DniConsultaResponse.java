package pe.edu.pucp.skillbridge.dto;

/**
 * Representa los datos personales necesarios obtenidos de la consulta del DNI.
 *
 * @param dni número de DNI consultado
 * @param nombres nombres asociados al DNI
 * @param apellidoPaterno apellido paterno asociado al DNI
 * @param apellidoMaterno apellido materno asociado al DNI
 */
public record DniConsultaResponse(
    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno
) {}
