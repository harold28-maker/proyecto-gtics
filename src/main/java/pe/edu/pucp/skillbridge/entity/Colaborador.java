package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "colaboradores")
public class Colaborador {
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

    public Integer getIdColaborador() { return idColaborador; }
    public void setIdColaborador(Integer idColaborador) { this.idColaborador = idColaborador; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public Seniority getSeniority() { return seniority; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public String getInteresesProfesionales() { return interesesProfesionales; }
    public void setInteresesProfesionales(String interesesProfesionales) { this.interesesProfesionales = interesesProfesionales; }
    public Integer getDisponibilidadBase() { return disponibilidadBase; }
    public void setDisponibilidadBase(Integer disponibilidadBase) { this.disponibilidadBase = disponibilidadBase; }
}
