package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Representa un usuario registrado en SkillBridge y almacena sus datos
 * personales, su rol y el estado de su cuenta.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {
    /**
     * Define los estados posibles de una cuenta de usuario.
     */
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

    /**
     * Obtiene el identificador del usuario.
     *
     * @return el identificador del usuario
     */
    public Integer getIdUsuario() { return idUsuario; }
    /**
     * Establece el identificador del usuario.
     *
     * @param idUsuario identificador que se asignará
     */
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    /**
     * Obtiene el rol asignado al usuario.
     *
     * @return el rol asociado
     */
    public Rol getRol() { return rol; }
    /**
     * Establece el rol asignado al usuario.
     *
     * @param rol rol que se asignará
     */
    public void setRol(Rol rol) { this.rol = rol; }
    /**
     * Obtiene los nombres del usuario.
     *
     * @return los nombres almacenados
     */
    public String getNombres() { return nombres; }
    /**
     * Establece los nombres del usuario.
     *
     * @param nombres nombres que se asignarán
     */
    public void setNombres(String nombres) { this.nombres = nombres; }
    /**
     * Obtiene los apellidos del usuario.
     *
     * @return los apellidos almacenados
     */
    public String getApellidos() { return apellidos; }
    /**
     * Establece los apellidos del usuario.
     *
     * @param apellidos apellidos que se asignarán
     */
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return el correo almacenado
     */
    public String getCorreo() { return correo; }
    /**
     * Establece el correo electrónico del usuario.
     *
     * @param correo correo que se asignará
     */
    public void setCorreo(String correo) { this.correo = correo; }
    /**
     * Obtiene el hash de la contraseña del usuario.
     *
     * @return el hash almacenado
     */
    public String getPasswordHash() { return passwordHash; }
    /**
     * Establece el hash de la contraseña del usuario.
     *
     * @param passwordHash hash que se asignará
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    /**
     * Obtiene el número telefónico del usuario.
     *
     * @return el teléfono almacenado
     */
    public String getTelefono() { return telefono; }
    /**
     * Establece el número telefónico del usuario.
     *
     * @param telefono teléfono que se asignará
     */
    public void setTelefono(String telefono) { this.telefono = telefono; }
    /**
     * Obtiene la URL de la fotografía del usuario.
     *
     * @return la URL almacenada
     */
    public String getFotoUrl() { return fotoUrl; }
    /**
     * Establece la URL de la fotografía del usuario.
     *
     * @param fotoUrl URL que se asignará
     */
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    /**
     * Obtiene el estado de la cuenta del usuario.
     *
     * @return el estado asignado
     */
    public EstadoUsuario getEstado() { return estado; }
    /**
     * Establece el estado de la cuenta del usuario.
     *
     * @param estado estado que se asignará
     */
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    /**
     * Obtiene la fecha del último acceso del usuario.
     *
     * @return la fecha del último acceso
     */
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    /**
     * Establece la fecha del último acceso del usuario.
     *
     * @param ultimoAcceso fecha que se asignará
     */
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    /**
     * Obtiene la fecha de registro del usuario.
     *
     * @return la fecha de registro
     */
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    /**
     * Establece la fecha de registro del usuario.
     *
     * @param fechaRegistro fecha que se asignará
     */
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    /**
     * Construye el nombre completo con nombres y apellidos, omitiendo valores nulos
     * y eliminando los espacios extremos.
     *
     * @return el nombre completo del usuario
     */
    @Transient
    public String getNombreCompleto() {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
    }

    /**
     * Obtiene las iniciales del nombre y apellido; usa {@code S} y {@code B}
     * respectivamente cuando el dato correspondiente está vacío.
     *
     * @return las iniciales formadas por la entidad
     */
    @Transient
    public String getIniciales() {
        String n = (nombres == null || nombres.isBlank()) ? "S" : nombres.substring(0, 1).toUpperCase();
        String a = (apellidos == null || apellidos.isBlank()) ? "B" : apellidos.substring(0, 1).toUpperCase();
        return n + a;
    }

    /**
     * Convierte el nombre del rol almacenado en una etiqueta visual reconocible.
     *
     * @return el nombre visual del rol o {@code Usuario} si no existe rol
     */
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
