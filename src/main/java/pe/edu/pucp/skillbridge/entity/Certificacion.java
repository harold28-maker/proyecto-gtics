package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "certificaciones")
public class Certificacion {
    public enum EstadoCertificacion { VIGENTE, VENCIDA, SIN_EXPIRACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificacion")
    private Integer idCertificacion;

    @ManyToOne
    @JoinColumn(name = "id_colaborador", nullable = false)
    private Colaborador colaborador;

    @Column(nullable = false, length = 40)
    @NotBlank(message = "{validation.certificacion.nombre.required}")
    @Size(max = 40, message = "{validation.certificacion.nombre.size}")
    private String nombre;

    @Column(name = "entidad_emisora", length = 40)
    @Size(max = 40, message = "{validation.certificacion.entidad.size}")
    private String entidadEmisora;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "url_credencial", length = 300)
    @Size(max = 300, message = "{validation.certificacion.url.size}")
    @Pattern(regexp = "^$|^https?://.+$", message = "{validation.certificacion.url.pattern}")
    private String urlCredencial;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.certificacion.estado.required}")
    private EstadoCertificacion estado = EstadoCertificacion.VIGENTE;

    @AssertTrue(message = "{validation.certificacion.periodo}")
    public boolean isPeriodoValido() {
        return fechaEmision == null || fechaExpiracion == null || !fechaExpiracion.isBefore(fechaEmision);
    }

    public Integer getIdCertificacion() { return idCertificacion; }
    public void setIdCertificacion(Integer idCertificacion) { this.idCertificacion = idCertificacion; }
    public Colaborador getColaborador() { return colaborador; }
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEntidadEmisora() { return entidadEmisora; }
    public void setEntidadEmisora(String entidadEmisora) { this.entidadEmisora = entidadEmisora; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public LocalDate getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDate fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    public String getUrlCredencial() { return urlCredencial; }
    public void setUrlCredencial(String urlCredencial) { this.urlCredencial = urlCredencial; }
    public EstadoCertificacion getEstado() { return estado; }
    public void setEstado(EstadoCertificacion estado) { this.estado = estado; }
}
