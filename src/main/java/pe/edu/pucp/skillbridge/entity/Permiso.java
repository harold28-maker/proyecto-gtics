package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Representa una autorización asociada a un rol y dirigida a un módulo.
 */
@Entity
@Table(name = "permisos")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    @NotNull(message = "{validation.permiso.rol.required}")
    private Rol rol;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "{validation.permiso.nombre.required}")
    @Size(max = 100, message = "{validation.permiso.nombre.size}")
    private String nombre;

    @Column(nullable = false, length = 60)
    @NotBlank(message = "{validation.permiso.modulo.required}")
    @Size(max = 60, message = "{validation.permiso.modulo.size}")
    private String modulo;

    @Column(length = 200)
    @Size(max = 200, message = "{validation.permiso.descripcion.size}")
    private String descripcion;

    /**
     * Obtiene el identificador del permiso.
     *
     * @return el identificador del permiso
     */
    public Integer getIdPermiso() { return idPermiso; }
    /**
     * Establece el identificador del permiso.
     *
     * @param idPermiso identificador que se asignará
     */
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }
    /**
     * Obtiene el rol al que pertenece el permiso.
     *
     * @return el rol asociado
     */
    public Rol getRol() { return rol; }
    /**
     * Establece el rol al que pertenece el permiso.
     *
     * @param rol rol que se asignará
     */
    public void setRol(Rol rol) { this.rol = rol; }
    /**
     * Obtiene el nombre del permiso.
     *
     * @return el nombre almacenado
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre del permiso.
     *
     * @param nombre nombre que se asignará
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
    /**
     * Obtiene el módulo sobre el que aplica el permiso.
     *
     * @return el módulo asociado
     */
    public String getModulo() { return modulo; }
    /**
     * Establece el módulo sobre el que aplica el permiso.
     *
     * @param modulo módulo que se asignará
     */
    public void setModulo(String modulo) { this.modulo = modulo; }
    /**
     * Obtiene la descripción del permiso.
     *
     * @return la descripción almacenada
     */
    public String getDescripcion() { return descripcion; }
    /**
     * Establece la descripción del permiso.
     *
     * @param descripcion descripción que se asignará
     */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
