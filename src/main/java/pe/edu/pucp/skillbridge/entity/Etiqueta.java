package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "etiquetas")
public class Etiqueta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etiqueta")
    private Integer idEtiqueta;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    public Integer getIdEtiqueta() { return idEtiqueta; }
    public void setIdEtiqueta(Integer idEtiqueta) { this.idEtiqueta = idEtiqueta; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
