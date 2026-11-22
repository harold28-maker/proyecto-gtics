package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Solicitud de recuperación vigente de un usuario. Solo se guarda la huella
 * del enlace, vinculada al correo y a la contraseña actuales de la cuenta.
 */
@Entity
@Table(name = "recuperaciones_contrasena")
public class RecuperacionContrasena {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "huella_cuenta", length = 64, nullable = false)
    private String huellaCuenta;

    @Column(name = "solicitado_en", nullable = false)
    private LocalDateTime solicitadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    protected RecuperacionContrasena() {
    }

    public RecuperacionContrasena(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void renovar(String tokenHash, String huellaCuenta, LocalDateTime solicitadoEn, LocalDateTime expiraEn) {
        this.tokenHash = tokenHash;
        this.huellaCuenta = huellaCuenta;
        this.solicitadoEn = solicitadoEn;
        this.expiraEn = expiraEn;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getHuellaCuenta() {
        return huellaCuenta;
    }

    public LocalDateTime getSolicitadoEn() {
        return solicitadoEn;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }
}
