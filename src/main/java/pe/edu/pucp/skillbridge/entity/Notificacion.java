package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
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

    public Integer getIdNotificacion() { return idNotificacion; }
    public void setIdNotificacion(Integer idNotificacion) { this.idNotificacion = idNotificacion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public Boolean getLeida() { return leida; }
    public void setLeida(Boolean leida) { this.leida = leida; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
