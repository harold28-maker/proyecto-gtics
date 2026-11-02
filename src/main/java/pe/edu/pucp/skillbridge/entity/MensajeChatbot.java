package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_chatbot", indexes = {
        @Index(name = "idx_chatbot_usuario_mensaje", columnList = "id_usuario, id_mensaje_chatbot")
})
public class MensajeChatbot {

    public enum Remitente { USUARIO, ASISTENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje_chatbot")
    private Long idMensajeChatbot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @NotNull
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @NotNull
    private Remitente remitente;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    @Size(max = 4000)
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getIdMensajeChatbot() { return idMensajeChatbot; }
    public void setIdMensajeChatbot(Long idMensajeChatbot) { this.idMensajeChatbot = idMensajeChatbot; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Remitente getRemitente() { return remitente; }
    public void setRemitente(Remitente remitente) { this.remitente = remitente; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
