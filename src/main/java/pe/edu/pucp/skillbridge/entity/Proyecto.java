package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un proyecto gestionado por un usuario con la información de
 * planificación, prioridad, estado y progreso.
 */
@Entity
@Table(name = "proyectos")
public class Proyecto {
    /**
     * Define los niveles de prioridad disponibles para un proyecto.
     */
    public enum Prioridad { BAJA, MEDIA, ALTA }
    /**
     * Define los estados posibles de avance o ciclo de vida de un proyecto.
     */
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

    /**
     * Comprueba que la fecha de fin no sea anterior a la fecha de inicio.
     *
     * @return {@code true} si ambas fechas están ausentes o el periodo es válido
     */
    @AssertTrue(message = "{validation.proyecto.periodo}")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    /**
     * Obtiene el identificador del proyecto.
     *
     * @return el identificador del proyecto
     */
    public Integer getIdProyecto() { return idProyecto; }
    /**
     * Establece el identificador del proyecto.
     *
     * @param idProyecto identificador que se asignará
     */
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }
    /**
     * Obtiene el usuario responsable del proyecto.
     *
     * @return el gestor asociado
     */
    public Usuario getProjectManager() { return projectManager; }
    /**
     * Establece el usuario responsable del proyecto.
     *
     * @param projectManager gestor que se asignará
     */
    public void setProjectManager(Usuario projectManager) { this.projectManager = projectManager; }
    /**
     * Obtiene el nombre del proyecto.
     *
     * @return el nombre almacenado
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre del proyecto.
     *
     * @param nombre nombre que se asignará
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
    /**
     * Obtiene la descripción del proyecto.
     *
     * @return la descripción almacenada
     */
    public String getDescripcion() { return descripcion; }
    /**
     * Establece la descripción del proyecto.
     *
     * @param descripcion descripción que se asignará
     */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    /**
     * Obtiene la fecha de inicio del proyecto.
     *
     * @return la fecha de inicio
     */
    public LocalDate getFechaInicio() { return fechaInicio; }
    /**
     * Establece la fecha de inicio del proyecto.
     *
     * @param fechaInicio fecha que se asignará
     */
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    /**
     * Obtiene la fecha de fin del proyecto.
     *
     * @return la fecha de fin
     */
    public LocalDate getFechaFin() { return fechaFin; }
    /**
     * Establece la fecha de fin del proyecto.
     *
     * @param fechaFin fecha que se asignará
     */
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    /**
     * Obtiene el número de vacantes del proyecto.
     *
     * @return la cantidad de vacantes
     */
    public Integer getVacantes() { return vacantes; }
    /**
     * Establece el número de vacantes del proyecto.
     *
     * @param vacantes cantidad que se asignará
     */
    public void setVacantes(Integer vacantes) { this.vacantes = vacantes; }
    /**
     * Obtiene la prioridad del proyecto.
     *
     * @return la prioridad asignada
     */
    public Prioridad getPrioridad() { return prioridad; }
    /**
     * Establece la prioridad del proyecto.
     *
     * @param prioridad prioridad que se asignará
     */
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    /**
     * Obtiene el estado del proyecto.
     *
     * @return el estado asignado
     */
    public EstadoProyecto getEstado() { return estado; }
    /**
     * Establece el estado del proyecto.
     *
     * @param estado estado que se asignará
     */
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }
    /**
     * Obtiene el porcentaje de progreso del proyecto.
     *
     * @return el progreso almacenado
     */
    public Integer getProgreso() { return progreso; }
    /**
     * Establece el porcentaje de progreso del proyecto.
     *
     * @param progreso progreso que se asignará
     */
    public void setProgreso(Integer progreso) { this.progreso = progreso; }
    /**
     * Obtiene la fecha de creación del proyecto.
     *
     * @return la fecha de creación
     */
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    /**
     * Establece la fecha de creación del proyecto.
     *
     * @param fechaCreacion fecha que se asignará
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
