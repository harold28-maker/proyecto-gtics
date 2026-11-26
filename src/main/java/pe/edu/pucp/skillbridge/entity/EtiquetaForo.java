package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "etiquetas")
public class EtiquetaForo {
    @Id
    @Column(name = "id_etiqueta")
    private Integer idEtiqueta;

    @Column(nullable = false, length = 60)
    private String nombre;

    public Integer getIdEtiqueta() { return idEtiqueta; }
    public String getNombre() { return nombre; }
}
