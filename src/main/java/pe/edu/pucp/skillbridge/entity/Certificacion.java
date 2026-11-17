package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Representa una certificación obtenida por un colaborador.
 */
@Entity
@Table(name = "certificaciones")
public class Certificacion {
    /**
     * Estados posibles de una certificación.
     */
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
    @Pattern(regexp = "^$|^https?://.+$|^/colaborador/certificaciones/archivo/[A-Za-z0-9._-]+$", message = "{validation.certificacion.url.pattern}")
    private String urlCredencial;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.certificacion.estado.required}")
    private EstadoCertificacion estado = EstadoCertificacion.VIGENTE;

    /**
     * Comprueba que la fecha de expiración no sea anterior a la fecha de emisión.
     *
     * @return {@code true} si alguna fecha no está definida o el período es válido
     */
    @AssertTrue(message = "{validation.certificacion.periodo}")
    public boolean isPeriodoValido() {
        return fechaEmision == null || fechaExpiracion == null || !fechaExpiracion.isBefore(fechaEmision);
    }

    /**
     * Obtiene el identificador de la certificación.
     *
     * @return identificador de la certificación
     */
    public Integer getIdCertificacion() { return idCertificacion; }
    /**
     * Establece el identificador de la certificación.
     *
     * @param idCertificacion identificador asignado
     */
    public void setIdCertificacion(Integer idCertificacion) { this.idCertificacion = idCertificacion; }
    /**
     * Obtiene el colaborador asociado a la certificación.
     *
     * @return colaborador asociado
     */
    public Colaborador getColaborador() { return colaborador; }
    /**
     * Establece el colaborador asociado a la certificación.
     *
     * @param colaborador colaborador asociado
     */
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }
    /**
     * Obtiene el nombre de la certificación.
     *
     * @return nombre de la certificación
     */
    public String getNombre() { return nombre; }
    /**
     * Establece el nombre de la certificación.
     *
     * @param nombre nombre asignado
     */
    public void setNombre(String nombre) { this.nombre = nombre; }
    /**
     * Obtiene la entidad emisora de la certificación.
     *
     * @return entidad emisora
     */
    public String getEntidadEmisora() { return entidadEmisora; }
    /**
     * Establece la entidad emisora de la certificación.
     *
     * @param entidadEmisora entidad emisora asignada
     */
    public void setEntidadEmisora(String entidadEmisora) { this.entidadEmisora = entidadEmisora; }
    /**
     * Obtiene la fecha de emisión de la certificación.
     *
     * @return fecha de emisión
     */
    public LocalDate getFechaEmision() { return fechaEmision; }
    /**
     * Establece la fecha de emisión de la certificación.
     *
     * @param fechaEmision fecha de emisión
     */
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    /**
     * Obtiene la fecha de expiración de la certificación.
     *
     * @return fecha de expiración
     */
    public LocalDate getFechaExpiracion() { return fechaExpiracion; }
    /**
     * Establece la fecha de expiración de la certificación.
     *
     * @param fechaExpiracion fecha de expiración
     */
    public void setFechaExpiracion(LocalDate fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    /**
     * Obtiene la URL de la credencial de la certificación.
     *
     * @return URL de la credencial
     */
    public String getUrlCredencial() { return urlCredencial; }
    /**
     * Establece la URL de la credencial de la certificación.
     *
     * @param urlCredencial URL asignada
     */
    public void setUrlCredencial(String urlCredencial) { this.urlCredencial = urlCredencial; }
    /**
     * Obtiene el estado actual de la certificación.
     *
     * @return estado de la certificación
     */
    public EstadoCertificacion getEstado() { return estado; }
    /**
     * Establece el estado actual de la certificación.
     *
     * @param estado estado asignado
     */
    public void setEstado(EstadoCertificacion estado) { this.estado = estado; }
}
