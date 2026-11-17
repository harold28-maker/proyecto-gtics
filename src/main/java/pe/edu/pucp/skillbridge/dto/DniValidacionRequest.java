package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos declarados por la persona: se comparan con respuesta del proveedor. */
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
