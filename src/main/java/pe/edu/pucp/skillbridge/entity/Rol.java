package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "roles")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "{validation.rol.nombre.required}")
    @Size(max = 50, message = "{validation.rol.nombre.size}")
    private String nombre;

    @Column(length = 200)
    @Size(max = 200, message = "{validation.rol.descripcion.size}")
    private String descripcion;

    @Column(nullable = false)
    @NotNull(message = "{validation.rol.estado.required}")
    private Boolean estado = true;

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
}
