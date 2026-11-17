package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contiene los datos declarados por una persona para compararlos con la respuesta del proveedor.
 *
 * @param dni número de DNI compuesto por ocho dígitos
 * @param nombres nombres declarados por la persona
 * @param apellidoPaterno apellido paterno declarado por la persona
 * @param apellidoMaterno apellido materno declarado por la persona
 */
public record DniValidacionRequest(
    @NotBlank(message = "Ingresa un DNI")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 digitos")
    String dni,

    @NotBlank(message = "Ingresa los nombres")
    @Size(max = 120)
    String nombres,

    @NotBlank(message = "Ingresa el apellido paterno")
    @Size(max = 80)
    String apellidoPaterno,

    @NotBlank(message = "Ingresa el apellido materno")
    @Size(max = 80)
    String apellidoMaterno
) {}
