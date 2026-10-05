package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "habilidades")
public class Habilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habilidad")
    private Integer idHabilidad;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "{validation.habilidad.nombre.required}")
    @Size(max = 40, message = "{validation.habilidad.nombre.size}")
    private String nombre;

    @Column(length = 80)
    @NotBlank(message = "{validation.habilidad.categoria.required}")
    @Size(max = 30, message = "{validation.habilidad.categoria.size}")
    private String categoria;

    @Column(length = 250)
    @NotBlank(message = "{validation.habilidad.descripcion.required}")
    @Size(max = 150, message = "{validation.habilidad.descripcion.size}")
    private String descripcion;

    @NotNull(message = "{validation.habilidad.estado.required}")
    private Boolean estado = true;

    public Integer getIdHabilidad() { return idHabilidad; }
    public void setIdHabilidad(Integer idHabilidad) { this.idHabilidad = idHabilidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
}
