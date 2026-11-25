package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import org.springframework.format.annotation.DateTimeFormat;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;

import java.time.LocalDate;

public class AsignacionFormulario {
    public interface ConsultaCapacidad {}

    @Positive(groups = {Default.class, ConsultaCapacidad.class}, message = "Selecciona una asignación válida.")
    private Integer idAsignacion;

    @NotNull(groups = {Default.class, ConsultaCapacidad.class}, message = "{validation.asignacion.proyecto.required}")
    @Positive(groups = {Default.class, ConsultaCapacidad.class}, message = "Selecciona un proyecto válido.")
    private Integer idProyecto;

    @NotNull(groups = {Default.class, ConsultaCapacidad.class}, message = "{validation.asignacion.colaborador.required}")
    @Positive(groups = {Default.class, ConsultaCapacidad.class}, message = "Selecciona un colaborador válido.")
    private Integer idColaborador;

    @NotBlank(message = "{validation.asignacion.rol.required}")
    @Size(max = 100, message = "{validation.asignacion.rol.size}")
    private String rolProyecto;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(groups = {Default.class, ConsultaCapacidad.class}, message = "{validation.asignacion.inicio.required}")
    private LocalDate fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    @NotNull(message = "{validation.asignacion.dedicacion.required}")
    @Min(value = 1, message = "{validation.asignacion.dedicacion.min}")
    @Max(value = 100, message = "{validation.asignacion.dedicacion.max}")
    private Integer porcentajeDedicacion;

    @NotNull(message = "{validation.asignacion.estado.required}")
    private EstadoAsignacion estado = EstadoAsignacion.PLANIFICADA;

    @AssertTrue(groups = {Default.class, ConsultaCapacidad.class}, message = "{validation.asignacion.periodo}")
    public boolean isPeriodoValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    public static AsignacionFormulario desde(Asignacion asignacion) {
        AsignacionFormulario formulario = new AsignacionFormulario();
        formulario.setIdAsignacion(asignacion.getIdAsignacion());
        formulario.setIdProyecto(asignacion.getProyecto().getIdProyecto());
        formulario.setIdColaborador(asignacion.getColaborador().getIdColaborador());
        formulario.setRolProyecto(asignacion.getRolProyecto());
        formulario.setFechaInicio(asignacion.getFechaInicio());
        formulario.setFechaFin(asignacion.getFechaFin());
        formulario.setPorcentajeDedicacion(asignacion.getPorcentajeDedicacion());
        formulario.setEstado(asignacion.getEstado());
        return formulario;
    }

    public Integer getIdAsignacion() { return idAsignacion; }
    public void setIdAsignacion(Integer idAsignacion) { this.idAsignacion = idAsignacion; }
    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }
    public Integer getIdColaborador() { return idColaborador; }
    public void setIdColaborador(Integer idColaborador) { this.idColaborador = idColaborador; }
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
