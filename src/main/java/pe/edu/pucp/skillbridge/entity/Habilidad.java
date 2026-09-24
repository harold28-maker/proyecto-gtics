package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "habilidades")
public class Habilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habilidad")
    private Integer idHabilidad;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 80)
    private String categoria;

    @Column(length = 250)
    private String descripcion;

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
