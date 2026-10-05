package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

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

    public Integer getIdRespuesta() { return idRespuesta; }
    public void setIdRespuesta(Integer idRespuesta) { this.idRespuesta = idRespuesta; }
    public Foro getForo() { return foro; }
    public void setForo(Foro foro) { this.foro = foro; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public Boolean getEsSolucion() { return esSolucion; }
    public void setEsSolucion(Boolean esSolucion) { this.esSolucion = esSolucion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
