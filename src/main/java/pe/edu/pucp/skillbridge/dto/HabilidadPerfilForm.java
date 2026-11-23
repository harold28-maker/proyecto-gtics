package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Datos que el colaborador puede enviar al agregar o editar una habilidad. */
public class HabilidadPerfilForm {

    @NotNull(message = "{validation.habilidadPerfil.id.required}")
    @Positive(message = "{validation.habilidadPerfil.id.required}")
    private Integer idHabilidad;

    @NotNull(message = "{validation.habilidadPerfil.nivel.required}")
    private HabilidadColaboradorDTO.Nivel nivel;

    @NotBlank(message = "{validation.habilidadPerfil.anios.required}")
    @Size(max = 32, message = "{validation.habilidadPerfil.anios.formato}")
    private String aniosExperiencia;

    public Integer getIdHabilidad() { return idHabilidad; }
    public void setIdHabilidad(Integer idHabilidad) { this.idHabilidad = idHabilidad; }

    public HabilidadColaboradorDTO.Nivel getNivel() { return nivel; }
    public void setNivel(HabilidadColaboradorDTO.Nivel nivel) { this.nivel = nivel; }

    public String getAniosExperiencia() { return aniosExperiencia; }
    public void setAniosExperiencia(String aniosExperiencia) { this.aniosExperiencia = aniosExperiencia; }
}
