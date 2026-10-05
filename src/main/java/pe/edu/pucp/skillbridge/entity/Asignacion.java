package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "asignaciones")
public class Asignacion {
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
    @NotNull(message = "{validation.asignacion.inicio.required}")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "porcentaje_dedicacion", nullable = false)
    @NotNull(message = "{validation.asignacion.dedicacion.required}")
    @Min(value = 1, message = "{validation.asignacion.dedicacion.min}")
    @Max(value = 100, message = "{validation.asignacion.dedicacion.max}")
    private Integer porcentajeDedicacion;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.asignacion.estado.required}")
    private EstadoAsignacion estado = EstadoAsignacion.PLANIFICADA;

    @AssertTrue(message = "{validation.asignacion.periodo}")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    public Integer getIdAsignacion() { return idAsignacion; }
    public void setIdAsignacion(Integer idAsignacion) { this.idAsignacion = idAsignacion; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Colaborador getColaborador() { return colaborador; }
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    public String getRolProyecto() { return rolProyecto; }
    public void setRolProyecto(String rolProyecto) { this.rolProyecto = rolProyecto; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Integer getPorcentajeDedicacion() { return porcentajeDedicacion; }
    public void setPorcentajeDedicacion(Integer porcentajeDedicacion) { this.porcentajeDedicacion = porcentajeDedicacion; }
    public EstadoAsignacion getEstado() { return estado; }
    public void setEstado(EstadoAsignacion estado) { this.estado = estado; }
}
