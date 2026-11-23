package pe.edu.pucp.skillbridge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "permisos_deshabilitados",
        uniqueConstraints = @UniqueConstraint(name = "uk_permiso_deshabilitado_rol",
                columnNames = {"id_rol", "codigo"}))
public class PermisoDeshabilitado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_deshabilitado")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(nullable = false, length = 40)
    private String codigo;

    protected PermisoDeshabilitado() {}

    public PermisoDeshabilitado(Rol rol, String codigo) {
        this.rol = rol;
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
