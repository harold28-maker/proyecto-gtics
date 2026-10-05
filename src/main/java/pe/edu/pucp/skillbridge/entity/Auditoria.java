package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "{validation.auditoria.accion.required}")
    @Size(max = 100, message = "{validation.auditoria.accion.size}")
    private String accion;

    @Column(length = 80)
    @Size(max = 80, message = "{validation.auditoria.modulo.size}")
    private String modulo;

    @Column(length = 500)
    @Size(max = 500, message = "{validation.auditoria.detalle.size}")
    private String detalle;

    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha;

    public Integer getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(Integer idAuditoria) { this.idAuditoria = idAuditoria; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
