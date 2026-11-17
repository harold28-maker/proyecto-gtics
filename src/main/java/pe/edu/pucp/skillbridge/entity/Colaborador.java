package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Contiene los datos profesionales y de disponibilidad de un usuario colaborador.
 */
@Entity
@Table(name = "colaboradores")
public class Colaborador {
    /**
     * Niveles de seniority disponibles para un colaborador.
     */
    public enum Seniority { JUNIOR, SEMI_SENIOR, SENIOR, LEAD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colaborador")
    private Integer idColaborador;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(length = 20)
    @NotBlank(message = "{validation.colaborador.cargo.required}")
    @Size(max = 20, message = "{validation.colaborador.cargo.size}")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "{validation.colaborador.cargo.pattern}")
    private String cargo;

    @Column(length = 20)
    @NotBlank(message = "{validation.colaborador.area.required}")
    @Size(max = 20, message = "{validation.colaborador.area.size}")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "{validation.colaborador.area.pattern}")
    private String area;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{validation.colaborador.seniority.required}")
    private Seniority seniority = Seniority.JUNIOR;

    @Column(columnDefinition = "TEXT")
    @Size(max = 2000, message = "{validation.colaborador.biografia.size}")
    private String biografia;

    @Column(name = "intereses_profesionales", columnDefinition = "TEXT")
    @Size(max = 2000, message = "{validation.colaborador.intereses.size}")
    private String interesesProfesionales;

    @Column(name = "disponibilidad_base")
    @NotNull(message = "{validation.colaborador.disponibilidad.required}")
    @Min(value = 0, message = "{validation.colaborador.disponibilidad.min}")
    @Max(value = 100, message = "{validation.colaborador.disponibilidad.max}")
    private Integer disponibilidadBase = 100;

    /**
     * Obtiene el identificador del colaborador.
     *
     * @return identificador del colaborador
     */
    public Integer getIdColaborador() { return idColaborador; }
    /**
     * Establece el identificador del colaborador.
     *
     * @param idColaborador identificador asignado
     */
    public void setIdColaborador(Integer idColaborador) { this.idColaborador = idColaborador; }
    /**
     * Obtiene el usuario asociado al colaborador.
     *
     * @return usuario asociado
     */
    public Usuario getUsuario() { return usuario; }
    /**
     * Establece el usuario asociado al colaborador.
     *
     * @param usuario usuario asociado
     */
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    /**
     * Obtiene el cargo profesional del colaborador.
     *
     * @return cargo profesional
     */
    public String getCargo() { return cargo; }
    /**
     * Establece el cargo profesional del colaborador.
     *
     * @param cargo cargo asignado
     */
    public void setCargo(String cargo) { this.cargo = cargo; }
    /**
     * Obtiene el área profesional del colaborador.
     *
     * @return área profesional
     */
    public String getArea() { return area; }
    /**
     * Establece el área profesional del colaborador.
     *
     * @param area área asignada
     */
    public void setArea(String area) { this.area = area; }
    /**
     * Obtiene el nivel de seniority del colaborador.
     *
     * @return nivel de seniority
     */
    public Seniority getSeniority() { return seniority; }
    /**
     * Establece el nivel de seniority del colaborador.
     *
     * @param seniority nivel asignado
     */
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
    /**
     * Obtiene la biografía profesional del colaborador.
     *
     * @return biografía profesional
     */
    public String getBiografia() { return biografia; }
    /**
     * Establece la biografía profesional del colaborador.
     *
     * @param biografia biografía asignada
     */
    public void setBiografia(String biografia) { this.biografia = biografia; }
    /**
     * Obtiene los intereses profesionales del colaborador.
     *
     * @return intereses profesionales
     */
    public String getInteresesProfesionales() { return interesesProfesionales; }
    /**
     * Establece los intereses profesionales del colaborador.
     *
     * @param interesesProfesionales intereses asignados
     */
    public void setInteresesProfesionales(String interesesProfesionales) { this.interesesProfesionales = interesesProfesionales; }
    /**
     * Obtiene la disponibilidad base del colaborador.
     *
     * @return porcentaje de disponibilidad base
     */
    public Integer getDisponibilidadBase() { return disponibilidadBase; }
    /**
     * Establece la disponibilidad base del colaborador.
     *
     * @param disponibilidadBase porcentaje de disponibilidad asignado
     */
    public void setDisponibilidadBase(Integer disponibilidadBase) { this.disponibilidadBase = disponibilidadBase; }
}
