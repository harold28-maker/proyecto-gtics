package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Representa la asignación de un colaborador a un proyecto y sus datos de dedicación.
 */
@Entity
@Table(name = "asignaciones")
public class Asignacion {
    /**
     * Estados posibles de una asignación dentro de un proyecto.
     */
    public enum EstadoAsignacion { PLANIFICADA, ACTIVA, FINALIZADA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Integer idAsignacion;

    @ManyToOne
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "id_colaborador", nullable = false)
    private Colaborador colaborador;

    @Column(name = "rol_proyecto", length = 100)
    @NotBlank(message = "{validation.asignacion.rol.required}")
    @Size(max = 100, message = "{validation.asignacion.rol.size}")
    private String rolProyecto;

    @Column(name = "fecha_inicio", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "{validation.asignacion.inicio.required}")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    @Column(name = "porcentaje_dedicacion", nullable = false)
    @NotNull(message = "{validation.asignacion.dedicacion.required}")
    @Min(value = 1, message = "{validation.asignacion.dedicacion.min}")
    @Max(value = 100, message = "{validation.asignacion.dedicacion.max}")
    private Integer porcentajeDedicacion;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.asignacion.estado.required}")
    private EstadoAsignacion estado = EstadoAsignacion.PLANIFICADA;

    /**
     * Comprueba que la fecha final no sea anterior a la fecha inicial.
     *
     * @return {@code true} si alguna fecha no está definida o el período es válido
     */
    @AssertTrue(message = "{validation.asignacion.periodo}")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    /**
     * Obtiene el identificador de la asignación.
     *
     * @return identificador de la asignación
     */
    public Integer getIdAsignacion() { return idAsignacion; }
    /**
     * Establece el identificador de la asignación.
     *
     * @param idAsignacion identificador asignado
     */
    public void setIdAsignacion(Integer idAsignacion) { this.idAsignacion = idAsignacion; }
    /**
     * Obtiene el proyecto asociado a la asignación.
     *
     * @return proyecto asociado
     */
    public Proyecto getProyecto() { return proyecto; }
    /**
     * Establece el proyecto asociado a la asignación.
     *
     * @param proyecto proyecto asignado
     */
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    /**
     * Obtiene el colaborador asignado al proyecto.
     *
     * @return colaborador asignado
     */
    public Colaborador getColaborador() { return colaborador; }
    /**
     * Establece el colaborador asignado al proyecto.
     *
     * @param colaborador colaborador asignado
     */
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    /**
     * Obtiene el rol desempeñado por el colaborador en el proyecto.
     *
     * @return rol del colaborador
     */
    public String getRolProyecto() { return rolProyecto; }
    /**
     * Establece el rol desempeñado por el colaborador en el proyecto.
     *
     * @param rolProyecto rol asignado
     */
    public void setRolProyecto(String rolProyecto) { this.rolProyecto = rolProyecto; }
    /**
     * Obtiene la fecha de inicio de la asignación.
     *
     * @return fecha de inicio
     */
    public LocalDate getFechaInicio() { return fechaInicio; }
    /**
     * Establece la fecha de inicio de la asignación.
     *
     * @param fechaInicio fecha de inicio
     */
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    /**
     * Obtiene la fecha de finalización de la asignación.
     *
     * @return fecha de finalización
     */
    public LocalDate getFechaFin() { return fechaFin; }
    /**
     * Establece la fecha de finalización de la asignación.
     *
     * @param fechaFin fecha de finalización
     */
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    /**
     * Obtiene el porcentaje de dedicación del colaborador.
     *
     * @return porcentaje de dedicación
     */
    public Integer getPorcentajeDedicacion() { return porcentajeDedicacion; }
    /**
     * Establece el porcentaje de dedicación del colaborador.
     *
     * @param porcentajeDedicacion porcentaje de dedicación
     */
    public void setPorcentajeDedicacion(Integer porcentajeDedicacion) { this.porcentajeDedicacion = porcentajeDedicacion; }
    /**
     * Obtiene el estado actual de la asignación.
     *
     * @return estado de la asignación
     */
    public EstadoAsignacion getEstado() { return estado; }
    /**
     * Establece el estado actual de la asignación.
     *
     * @param estado estado asignado
     */
    public void setEstado(EstadoAsignacion estado) { this.estado = estado; }
}
