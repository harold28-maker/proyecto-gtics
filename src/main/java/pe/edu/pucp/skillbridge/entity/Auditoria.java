package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Representa un registro de auditoría de una acción realizada en el sistema.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {
    public static final String ACCION_INICIAR_SESION = "Iniciar sesión";
    public static final String MODULO_AUTENTICACION = "Autenticación";

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

    /**
     * Obtiene el identificador del registro de auditoría.
     *
     * @return identificador del registro
     */
    public Integer getIdAuditoria() { return idAuditoria; }
    /**
     * Establece el identificador del registro de auditoría.
     *
     * @param idAuditoria identificador asignado
     */
    public void setIdAuditoria(Integer idAuditoria) { this.idAuditoria = idAuditoria; }
    /**
     * Obtiene el usuario asociado al registro.
     *
     * @return usuario asociado
     */
    public Usuario getUsuario() { return usuario; }
    /**
     * Establece el usuario asociado al registro.
     *
     * @param usuario usuario asociado
     */
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    /**
     * Obtiene la acción registrada.
     *
     * @return descripción de la acción
     */
    public String getAccion() { return accion; }
    /**
     * Establece la acción registrada.
     *
     * @param accion descripción de la acción
     */
    public void setAccion(String accion) { this.accion = accion; }
    /**
     * Obtiene el módulo donde se realizó la acción.
     *
     * @return nombre del módulo
     */
    public String getModulo() { return modulo; }
    /**
     * Establece el módulo donde se realizó la acción.
     *
     * @param modulo nombre del módulo
     */
    public void setModulo(String modulo) { this.modulo = modulo; }
    /**
     * Obtiene el detalle del registro de auditoría.
     *
     * @return detalle de la acción
     */
    public String getDetalle() { return detalle; }
    /**
     * Establece el detalle del registro de auditoría.
     *
     * @param detalle detalle de la acción
     */
    public void setDetalle(String detalle) { this.detalle = detalle; }
    /**
     * Obtiene la fecha del registro de auditoría.
     *
     * @return fecha del registro
     */
    public LocalDateTime getFecha() { return fecha; }
    /**
     * Establece la fecha del registro de auditoría.
     *
     * @param fecha fecha del registro
     */
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
