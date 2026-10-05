package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recomendaciones_ia")
public class RecomendacionIA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recomendacion")
    private Integer idRecomendacion;

    @ManyToOne
    @JoinColumn(name = "id_proyecto", nullable = false)
    @NotNull(message = "{validation.recomendacion.proyecto.required}")
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "id_colaborador", nullable = false)
    @NotNull(message = "{validation.recomendacion.colaborador.required}")
    private Colaborador colaborador;

    @Column(name = "porcentaje_match")
    @NotNull(message = "{validation.recomendacion.porcentaje.required}")
    @DecimalMin(value = "0.0", message = "{validation.recomendacion.porcentaje.min}")
    @DecimalMax(value = "100.0", message = "{validation.recomendacion.porcentaje.max}")
    private BigDecimal porcentajeMatch;

    @Column(columnDefinition = "TEXT")
    @Size(max = 2000, message = "{validation.recomendacion.justificacion.size}")
    private String justificacion;

    @ManyToOne
    @JoinColumn(name = "recomendado_por")
    private Usuario recomendadoPor;

    @Column(name = "fecha_recomendacion", insertable = false, updatable = false)
    private LocalDateTime fechaRecomendacion;

    public Integer getIdRecomendacion() { return idRecomendacion; }
    public void setIdRecomendacion(Integer idRecomendacion) { this.idRecomendacion = idRecomendacion; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Colaborador getColaborador() { return colaborador; }
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    public BigDecimal getPorcentajeMatch() { return porcentajeMatch; }
    public void setPorcentajeMatch(BigDecimal porcentajeMatch) { this.porcentajeMatch = porcentajeMatch; }
    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    public Usuario getRecomendadoPor() { return recomendadoPor; }
    public void setRecomendadoPor(Usuario recomendadoPor) { this.recomendadoPor = recomendadoPor; }
    public LocalDateTime getFechaRecomendacion() { return fechaRecomendacion; }
    public void setFechaRecomendacion(LocalDateTime fechaRecomendacion) { this.fechaRecomendacion = fechaRecomendacion; }
}
