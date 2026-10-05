package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
    public enum EstadoUsuario { ACTIVO, INACTIVO, BLOQUEADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    @NotNull(message = "{validation.usuario.rol.required}")
    private Rol rol;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "{validation.usuario.nombres.required}")
    @Size(max = 20, message = "{validation.usuario.nombres.size}")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "{validation.usuario.nombres.pattern}")
    private String nombres;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "{validation.usuario.apellidos.required}")
    @Size(max = 20, message = "{validation.usuario.apellidos.size}")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "{validation.usuario.apellidos.pattern}")
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    @NotBlank(message = "{validation.usuario.correo.required}")
    @Email(message = "{validation.usuario.correo.email}")
    @Size(max = 150, message = "{validation.usuario.correo.size}")
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "{validation.usuario.password.required}")
    @Size(max = 255, message = "{validation.usuario.password.size}")
    private String passwordHash;

    @Column(length = 9)
    @Pattern(regexp = "^$|^[0-9]{9}$", message = "{validation.usuario.telefono.pattern}")
    private String telefono;

    @Column(name = "foto_url", length = 300)
    @Size(max = 300, message = "{validation.usuario.foto.size}")
    private String fotoUrl;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.usuario.estado.required}")
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Transient
    public String getNombreCompleto() {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
    }

    @Transient
    public String getIniciales() {
        String n = (nombres == null || nombres.isBlank()) ? "S" : nombres.substring(0, 1).toUpperCase();
        String a = (apellidos == null || apellidos.isBlank()) ? "B" : apellidos.substring(0, 1).toUpperCase();
        return n + a;
    }

    @Transient
    public String getRolVisual() {
        if (rol == null || rol.getNombre() == null) return "Usuario";
        return switch (rol.getNombre()) {
            case "ADMINISTRADOR" -> "Administrador";
            case "PROJECT_MANAGER" -> "Project Manager";
            case "RESOURCE_MANAGER" -> "Resource Manager";
            case "COLABORADOR" -> "Colaborador";
            default -> rol.getNombre();
        };
    }
}
