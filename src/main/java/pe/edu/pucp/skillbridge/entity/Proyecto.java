package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proyectos")
public class Proyecto {
    public enum Prioridad { BAJA, MEDIA, ALTA }
    public enum EstadoProyecto { PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @ManyToOne
    @JoinColumn(name = "id_project_manager", nullable = false)
    private Usuario projectManager;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "{validation.proyecto.nombre.required}")
    @Size(max = 150, message = "{validation.proyecto.nombre.size}")
    private String nombre;

    @Column(columnDefinition = "TEXT")
    @Size(max = 3000, message = "{validation.proyecto.descripcion.size}")
    private String descripcion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @PositiveOrZero(message = "{validation.proyecto.vacantes.min}")
    private Integer vacantes = 0;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.proyecto.prioridad.required}")
    private Prioridad prioridad = Prioridad.MEDIA;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.proyecto.estado.required}")
    private EstadoProyecto estado = EstadoProyecto.PLANNING;

    @NotNull(message = "{validation.proyecto.progreso.required}")
    @Min(value = 0, message = "{validation.proyecto.progreso.min}")
    @Max(value = 100, message = "{validation.proyecto.progreso.max}")
    private Integer progreso = 0;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @AssertTrue(message = "{validation.proyecto.periodo}")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }
    public Usuario getProjectManager() { return projectManager; }
    public void setProjectManager(Usuario projectManager) { this.projectManager = projectManager; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Integer getVacantes() { return vacantes; }
    public void setVacantes(Integer vacantes) { this.vacantes = vacantes; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public EstadoProyecto getEstado() { return estado; }
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }
    public Integer getProgreso() { return progreso; }
    public void setProgreso(Integer progreso) { this.progreso = progreso; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
