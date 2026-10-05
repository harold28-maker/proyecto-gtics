package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "etiquetas")
public class Etiqueta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etiqueta")
    private Integer idEtiqueta;

    @Column(nullable = false, unique = true, length = 60)
    @NotBlank(message = "{validation.etiqueta.nombre.required}")
    @Size(max = 60, message = "{validation.etiqueta.nombre.size}")
    private String nombre;

    public Integer getIdEtiqueta() { return idEtiqueta; }
    public void setIdEtiqueta(Integer idEtiqueta) { this.idEtiqueta = idEtiqueta; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
