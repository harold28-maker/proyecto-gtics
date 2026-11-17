package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Representa una notificación dirigida a un usuario dentro de la plataforma.
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {
    /**
     * Clasifica la finalidad de una notificación.
     */
    public enum TipoNotificacion { INFO, PROYECTO, ASIGNACION, FORO, CHAT, SISTEMA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    @NotNull(message = "{validation.notificacion.usuario.required}")
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "{validation.notificacion.titulo.required}")
    @Size(max = 150, message = "{validation.notificacion.titulo.size}")
    private String titulo;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "{validation.notificacion.mensaje.required}")
    @Size(max = 500, message = "{validation.notificacion.mensaje.size}")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.notificacion.tipo.required}")
    private TipoNotificacion tipo = TipoNotificacion.INFO;

    @NotNull(message = "{validation.notificacion.leida.required}")
    private Boolean leida = false;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Obtiene el identificador de la notificación.
     *
     * @return el identificador de la notificación
     */
    public Integer getIdNotificacion() { return idNotificacion; }
    /**
     * Establece el identificador de la notificación.
     *
     * @param idNotificacion identificador que se asignará
     */
    public void setIdNotificacion(Integer idNotificacion) { this.idNotificacion = idNotificacion; }
    /**
     * Obtiene el usuario destinatario de la notificación.
     *
     * @return el usuario asociado
     */
    public Usuario getUsuario() { return usuario; }
    /**
     * Establece el usuario destinatario de la notificación.
     *
     * @param usuario usuario que se asignará
     */
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    /**
     * Obtiene el título de la notificación.
     *
     * @return el título almacenado
     */
    public String getTitulo() { return titulo; }
    /**
     * Establece el título de la notificación.
     *
     * @param titulo título que se asignará
     */
    public void setTitulo(String titulo) { this.titulo = titulo; }
    /**
     * Obtiene el mensaje de la notificación.
     *
     * @return el mensaje almacenado
     */
    public String getMensaje() { return mensaje; }
    /**
     * Establece el mensaje de la notificación.
     *
     * @param mensaje mensaje que se asignará
     */
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    /**
     * Obtiene el tipo de la notificación.
     *
     * @return el tipo asignado
     */
    public TipoNotificacion getTipo() { return tipo; }
    /**
     * Establece el tipo de la notificación.
     *
     * @param tipo tipo que se asignará
     */
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    /**
     * Indica si la notificación fue marcada como leída.
     *
     * @return {@code true} si está leída; {@code false} en caso contrario
     */
    public Boolean getLeida() { return leida; }
    /**
     * Establece si la notificación fue marcada como leída.
     *
     * @param leida estado de lectura que se asignará
     */
    public void setLeida(Boolean leida) { this.leida = leida; }
    /**
     * Obtiene la fecha de creación de la notificación.
     *
     * @return la fecha de creación
     */
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    /**
     * Establece la fecha de creación de la notificación.
     *
     * @param fechaCreacion fecha que se asignará
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
