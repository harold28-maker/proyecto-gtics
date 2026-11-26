package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ConsultaTalentoFormulario {
    @NotNull(message = "Selecciona un proyecto.")
    @Positive(message = "Selecciona un proyecto válido.")
    private Integer idProyecto;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;

    @Min(value = 1, message = "La dedicación debe estar entre 1 % y 100 %.")
    @Max(value = 100, message = "La dedicación debe estar entre 1 % y 100 %.")
    private Integer dedicacion;

    @NotBlank(message = "Escribe una consulta para el asistente.")
    @Size(max = 1000, message = "La consulta admite hasta 1000 caracteres.")
    private String mensaje;

    @AssertTrue(message = "Indica ambas fechas en orden, con años entre 1000 y 9999.")
    public boolean isPeriodoValido() {
        if (desde == null && hasta == null) return true;
        return desde != null && hasta != null && !hasta.isBefore(desde)
                && desde.getYear() >= 1000 && hasta.getYear() <= 9999;
    }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public Integer getDedicacion() { return dedicacion; }
    public void setDedicacion(Integer dedicacion) { this.dedicacion = dedicacion; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
