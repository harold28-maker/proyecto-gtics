package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_chat")
/**
 * Representa un mensaje enviado por un usuario dentro del chat de un proyecto.
 */
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

    /**
     * Obtiene el identificador del mensaje.
     *
     * @return identificador del mensaje
     */
    public Integer getIdMensaje() { return idMensaje; }
    /**
     * Establece el identificador del mensaje.
     *
     * @param idMensaje identificador asignado
     */
    public void setIdMensaje(Integer idMensaje) { this.idMensaje = idMensaje; }
    /**
     * Obtiene el proyecto al que pertenece el mensaje.
     *
     * @return proyecto asociado
     */
    public Proyecto getProyecto() { return proyecto; }
    /**
     * Establece el proyecto al que pertenece el mensaje.
     *
     * @param proyecto proyecto asociado
     */
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    /**
     * Obtiene el usuario que envió el mensaje.
     *
     * @return usuario remitente
     */
    public Usuario getUsuario() { return usuario; }
    /**
     * Establece el usuario que envió el mensaje.
     *
     * @param usuario usuario remitente
     */
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    /**
     * Obtiene el contenido del mensaje.
     *
     * @return contenido del mensaje
     */
    public String getContenido() { return contenido; }
    /**
     * Establece el contenido del mensaje.
     *
     * @param contenido contenido asignado
     */
    public void setContenido(String contenido) { this.contenido = contenido; }
    /**
     * Obtiene la fecha de envío del mensaje.
     *
     * @return fecha de envío
     */
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    /**
     * Establece la fecha de envío del mensaje.
     *
     * @param fechaEnvio fecha de envío
     */
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}
