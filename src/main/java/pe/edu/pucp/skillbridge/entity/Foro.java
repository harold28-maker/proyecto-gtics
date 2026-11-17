package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Representa una publicación dentro del foro asociada opcionalmente a un proyecto.
 */
@Entity
@Table(name = "foros")
public class Foro {
    /**
     * Estados posibles de una publicación del foro.
     */
    public enum EstadoForo { ABIERTO, RESUELTO, CERRADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foro")
    private Integer idForo;

    @ManyToOne
    @JoinColumn(name = "id_proyecto")
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 180)
    @NotBlank(message = "{validation.foro.titulo.required}")
    @Size(max = 180, message = "{validation.foro.titulo.size}")
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{validation.foro.contenido.required}")
    @Size(max = 5000, message = "{validation.foro.contenido.size}")
    private String contenido;

    @Column(length = 80)
    @Size(max = 80, message = "{validation.foro.categoria.size}")
    private String categoria;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.foro.estado.required}")
    private EstadoForo estado = EstadoForo.ABIERTO;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Obtiene el identificador del foro.
     *
     * @return identificador del foro
     */
    public Integer getIdForo() { return idForo; }
    /**
     * Establece el identificador del foro.
     *
     * @param idForo identificador asignado
     */
    public void setIdForo(Integer idForo) { this.idForo = idForo; }
    /**
     * Obtiene el proyecto asociado al foro.
     *
     * @return proyecto asociado
     */
    public Proyecto getProyecto() { return proyecto; }
    /**
     * Establece el proyecto asociado al foro.
     *
     * @param proyecto proyecto asociado
     */
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    /**
     * Obtiene el usuario autor del foro.
     *
     * @return usuario autor
     */
    public Usuario getAutor() { return autor; }
    /**
     * Establece el usuario autor del foro.
     *
     * @param autor usuario autor
     */
    public void setAutor(Usuario autor) { this.autor = autor; }
    /**
     * Obtiene el título del foro.
     *
     * @return título del foro
     */
    public String getTitulo() { return titulo; }
    /**
     * Establece el título del foro.
     *
     * @param titulo título asignado
     */
    public void setTitulo(String titulo) { this.titulo = titulo; }
    /**
     * Obtiene el contenido del foro.
     *
     * @return contenido del foro
     */
    public String getContenido() { return contenido; }
    /**
     * Establece el contenido del foro.
     *
     * @param contenido contenido asignado
     */
    public void setContenido(String contenido) { this.contenido = contenido; }
    /**
     * Obtiene la categoría del foro.
     *
     * @return categoría del foro
     */
    public String getCategoria() { return categoria; }
    /**
     * Establece la categoría del foro.
     *
     * @param categoria categoría asignada
     */
    public void setCategoria(String categoria) { this.categoria = categoria; }
    /**
     * Obtiene el estado actual del foro.
     *
     * @return estado del foro
     */
    public EstadoForo getEstado() { return estado; }
    /**
     * Establece el estado actual del foro.
     *
     * @param estado estado asignado
     */
    public void setEstado(EstadoForo estado) { this.estado = estado; }
    /**
     * Obtiene la fecha de creación del foro.
     *
     * @return fecha de creación
     */
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    /**
     * Establece la fecha de creación del foro.
     *
     * @param fechaCreacion fecha de creación
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
