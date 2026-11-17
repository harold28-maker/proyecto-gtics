package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa la recomendación de un colaborador para un proyecto generada
 * o registrada con apoyo de inteligencia artificial.
 */
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

    /**
     * Obtiene el identificador de la recomendación.
     *
     * @return el identificador de la recomendación
     */
    public Integer getIdRecomendacion() { return idRecomendacion; }
    /**
     * Establece el identificador de la recomendación.
     *
     * @param idRecomendacion identificador que se asignará
     */
    public void setIdRecomendacion(Integer idRecomendacion) { this.idRecomendacion = idRecomendacion; }
    /**
     * Obtiene el proyecto para el que se realizó la recomendación.
     *
     * @return el proyecto asociado
     */
    public Proyecto getProyecto() { return proyecto; }
    /**
     * Establece el proyecto para el que se realizó la recomendación.
     *
     * @param proyecto proyecto que se asignará
     */
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    /**
     * Obtiene el colaborador recomendado.
     *
     * @return el colaborador asociado
     */
    public Colaborador getColaborador() { return colaborador; }
    /**
     * Establece el colaborador recomendado.
     *
     * @param colaborador colaborador que se asignará
     */
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    /**
     * Obtiene el porcentaje de coincidencia de la recomendación.
     *
     * @return el porcentaje de coincidencia
     */
    public BigDecimal getPorcentajeMatch() { return porcentajeMatch; }
    /**
     * Establece el porcentaje de coincidencia de la recomendación.
     *
     * @param porcentajeMatch porcentaje que se asignará
     */
    public void setPorcentajeMatch(BigDecimal porcentajeMatch) { this.porcentajeMatch = porcentajeMatch; }
    /**
     * Obtiene la justificación de la recomendación.
     *
     * @return la justificación almacenada
     */
    public String getJustificacion() { return justificacion; }
    /**
     * Establece la justificación de la recomendación.
     *
     * @param justificacion justificación que se asignará
     */
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    /**
     * Obtiene el usuario que realizó la recomendación.
     *
     * @return el usuario recomendador
     */
    public Usuario getRecomendadoPor() { return recomendadoPor; }
    /**
     * Establece el usuario que realizó la recomendación.
     *
     * @param recomendadoPor usuario que se asignará
     */
    public void setRecomendadoPor(Usuario recomendadoPor) { this.recomendadoPor = recomendadoPor; }
    /**
     * Obtiene la fecha en que se registró la recomendación.
     *
     * @return la fecha de recomendación
     */
    public LocalDateTime getFechaRecomendacion() { return fechaRecomendacion; }
    /**
     * Establece la fecha en que se registró la recomendación.
     *
     * @param fechaRecomendacion fecha que se asignará
     */
    public void setFechaRecomendacion(LocalDateTime fechaRecomendacion) { this.fechaRecomendacion = fechaRecomendacion; }
}
