package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;

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
    private String cargo;

    @Column(length = 20)
    private String area;

    @Enumerated(EnumType.STRING)
    private Seniority seniority = Seniority.JUNIOR;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "intereses_profesionales", columnDefinition = "TEXT")
    private String interesesProfesionales;

    @Column(name = "disponibilidad_base")
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
