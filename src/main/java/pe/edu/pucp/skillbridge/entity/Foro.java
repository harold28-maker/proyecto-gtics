package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "foros")
public class Foro {
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

    public Integer getIdForo() { return idForo; }
    public void setIdForo(Integer idForo) { this.idForo = idForo; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public EstadoForo getEstado() { return estado; }
    public void setEstado(EstadoForo estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
