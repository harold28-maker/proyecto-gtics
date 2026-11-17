package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "etiquetas")
/**
 * Representa una etiqueta identificable por su nombre.
 */
public class Etiqueta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etiqueta")
    private Integer idEtiqueta;

    @Column(nullable = false, unique = true, length = 60)
    @NotBlank(message = "{validation.etiqueta.nombre.required}")
    @Size(max = 60, message = "{validation.etiqueta.nombre.size}")
    private String nombre;

    /**
     * Obtiene el identificador de la etiqueta.
     *
     * @return identificador de la etiqueta
     */
    public Integer getIdEtiqueta() { return idEtiqueta; }
    /**
     * Establece el identificador de la etiqueta.
     *
     * @param idEtiqueta identificador asignado
     */
    public void setIdEtiqueta(Integer idEtiqueta) { this.idEtiqueta = idEtiqueta; }
    /**
     * Obtiene el nombre de la etiqueta.
     *
     * @return nombre de la etiqueta
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre de la etiqueta.
     *
     * @param nombre nombre asignado
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
}
