package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "certificaciones")
public class Certificacion {

    public enum EstadoCertificacion {
        VIGENTE,
        VENCIDA,
        SIN_EXPIRACION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificacion")
    private Integer idCertificacion;

    @ManyToOne
    @JoinColumn(name = "id_colaborador", nullable = false)
    private Colaborador colaborador;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(name = "entidad_emisora", length = 40)
    private String entidadEmisora;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "url_credencial", length = 300)
    private String urlCredencial;

    @Enumerated(EnumType.STRING)
    private EstadoCertificacion estado = EstadoCertificacion.VIGENTE;

    public Integer getIdCertificacion() {
        return idCertificacion;
    }

    public void setIdCertificacion(Integer idCertificacion) {
        this.idCertificacion = idCertificacion;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEntidadEmisora() {
        return entidadEmisora;
    }

    public void setEntidadEmisora(String entidadEmisora) {
        this.entidadEmisora = entidadEmisora;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDate fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getUrlCredencial() {
        return urlCredencial;
    }

    public void setUrlCredencial(String urlCredencial) {
        this.urlCredencial = urlCredencial;
    }

    public EstadoCertificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoCertificacion estado) {
        this.estado = estado;
    }
}
