package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Representa una respuesta publicada por un usuario en una discusión del foro.
 */
@Entity
@Table(name = "respuestas_foro")
public class RespuestaForo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    @ManyToOne
    @JoinColumn(name = "id_foro", nullable = false)
    @NotNull(message = "{validation.respuesta.foro.required}")
    private Foro foro;

    @ManyToOne
    @JoinColumn(name = "id_autor", nullable = false)
    @NotNull(message = "{validation.respuesta.autor.required}")
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{validation.respuesta.contenido.required}")
    @Size(max = 5000, message = "{validation.respuesta.contenido.size}")
    private String contenido;

    @Column(name = "es_solucion")
    @NotNull(message = "{validation.respuesta.solucion.required}")
    private Boolean esSolucion = false;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Obtiene el identificador de la respuesta.
     *
     * @return el identificador de la respuesta
     */
    public Integer getIdRespuesta() { return idRespuesta; }
    /**
     * Establece el identificador de la respuesta.
     *
     * @param idRespuesta identificador que se asignará
     */
    public void setIdRespuesta(Integer idRespuesta) { this.idRespuesta = idRespuesta; }
    /**
     * Obtiene la discusión del foro a la que pertenece la respuesta.
     *
     * @return el foro asociado
     */
    public Foro getForo() { return foro; }
    /**
     * Establece la discusión del foro a la que pertenece la respuesta.
     *
     * @param foro foro que se asignará
     */
    public void setForo(Foro foro) { this.foro = foro; }
    /**
     * Obtiene el usuario que creó la respuesta.
     *
     * @return el autor asociado
     */
    public Usuario getAutor() { return autor; }
    /**
     * Establece el usuario que creó la respuesta.
     *
     * @param autor autor que se asignará
     */
    public void setAutor(Usuario autor) { this.autor = autor; }
    /**
     * Obtiene el contenido de la respuesta.
     *
     * @return el contenido almacenado
     */
    public String getContenido() { return contenido; }
    /**
     * Establece el contenido de la respuesta.
     *
     * @param contenido contenido que se asignará
     */
    public void setContenido(String contenido) { this.contenido = contenido; }
    /**
     * Indica si la respuesta fue marcada como solución.
     *
     * @return {@code true} si es una solución; {@code false} en caso contrario
     */
    public Boolean getEsSolucion() { return esSolucion; }
    /**
     * Establece si la respuesta fue marcada como solución.
     *
     * @param esSolucion estado de solución que se asignará
     */
    public void setEsSolucion(Boolean esSolucion) { this.esSolucion = esSolucion; }
    /**
     * Obtiene la fecha de creación de la respuesta.
     *
     * @return la fecha de creación
     */
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    /**
     * Establece la fecha de creación de la respuesta.
     *
     * @param fechaCreacion fecha que se asignará
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
