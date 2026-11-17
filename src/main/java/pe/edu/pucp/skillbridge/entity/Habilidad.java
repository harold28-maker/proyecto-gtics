package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Representa una habilidad disponible en el catálogo del sistema.
 */
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

    /**
     * Obtiene el identificador de la habilidad.
     *
     * @return identificador de la habilidad
     */
    public Integer getIdHabilidad() { return idHabilidad; }
    /**
     * Establece el identificador de la habilidad.
     *
     * @param idHabilidad identificador asignado
     */
    public void setIdHabilidad(Integer idHabilidad) { this.idHabilidad = idHabilidad; }
    /**
     * Obtiene el nombre de la habilidad.
     *
     * @return nombre de la habilidad
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre de la habilidad.
     *
     * @param nombre nombre asignado
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
    /**
     * Obtiene la categoría de la habilidad.
     *
     * @return categoría de la habilidad
     */
    public String getCategoria() { return categoria; }
    /**
     * Establece la categoría de la habilidad.
     *
     * @param categoria categoría asignada
     */
    public void setCategoria(String categoria) { this.categoria = categoria; }
    /**
     * Obtiene la descripción de la habilidad.
     *
     * @return descripción de la habilidad
     */
    public String getDescripcion() { return descripcion; }
    /**
     * Establece la descripción de la habilidad.
     *
     * @param descripcion descripción asignada
     */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    /**
     * Obtiene si la habilidad se encuentra activa.
     *
     * @return {@code true} si está activa
     */
    public Boolean getEstado() { return estado; }
    /**
     * Establece si la habilidad se encuentra activa.
     *
     * @param estado estado activo o inactivo
     */
    public void setEstado(Boolean estado) { this.estado = estado; }
}
