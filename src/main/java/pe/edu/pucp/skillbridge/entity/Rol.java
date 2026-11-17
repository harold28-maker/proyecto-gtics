package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Representa un rol de usuario con un nombre, una descripción y un estado activo.
 */
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

    /**
     * Obtiene el identificador del rol.
     *
     * @return el identificador del rol
     */
    public Integer getIdRol() { return idRol; }
    /**
     * Establece el identificador del rol.
     *
     * @param idRol identificador que se asignará
     */
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    /**
     * Obtiene el nombre del rol.
     *
     * @return el nombre almacenado
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre del rol.
     *
     * @param nombre nombre que se asignará
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
    /**
     * Obtiene la descripción del rol.
     *
     * @return la descripción almacenada
     */
    public String getDescripcion() { return descripcion; }
    /**
     * Establece la descripción del rol.
     *
     * @param descripcion descripción que se asignará
     */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    /**
     * Indica si el rol se encuentra activo.
     *
     * @return {@code true} si está activo; {@code false} en caso contrario
     */
    public Boolean getEstado() { return estado; }
    /**
     * Establece si el rol se encuentra activo.
     *
     * @param estado estado que se asignará
     */
    public void setEstado(Boolean estado) { this.estado = estado; }
}
