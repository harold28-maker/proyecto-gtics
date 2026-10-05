package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_chat")
public class MensajeChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @ManyToOne
    @JoinColumn(name = "id_proyecto", nullable = false)
    @NotNull(message = "{validation.chat.proyecto.required}")
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    @NotNull(message = "{validation.chat.usuario.required}")
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{validation.chat.contenido.required}")
    @Size(max = 2000, message = "{validation.chat.contenido.size}")
    private String contenido;

    @Column(name = "fecha_envio", insertable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    public Integer getIdMensaje() { return idMensaje; }
    public void setIdMensaje(Integer idMensaje) { this.idMensaje = idMensaje; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}
