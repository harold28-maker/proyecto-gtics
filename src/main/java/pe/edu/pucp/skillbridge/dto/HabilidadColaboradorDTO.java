package pe.edu.pucp.skillbridge.dto;

import java.math.BigDecimal;

public class HabilidadColaboradorDTO {
    private Integer idHabilidad;
    private String nombre;
    private String categoria;
    private String nivel;
    private BigDecimal aniosExperiencia;

    public HabilidadColaboradorDTO(Integer idHabilidad, String nombre, String categoria,
                                   String nivel, BigDecimal aniosExperiencia) {
        this.idHabilidad = idHabilidad;
        this.nombre = nombre;
        this.categoria = categoria;
        this.nivel = nivel;
        this.aniosExperiencia = aniosExperiencia;
    }

    public Integer getIdHabilidad() { return idHabilidad; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getNivel() { return nivel; }
    public BigDecimal getAniosExperiencia() { return aniosExperiencia; }
}
