package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Permiso;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para los permisos asociados a los roles.
 */
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    /**
     * Busca todos los permisos asignados a un rol determinado.
     *
     * @param idRol identificador del rol cuyos permisos se quieren consultar
     * @return lista de permisos asociados al rol
     */
    List<Permiso> findByRolIdRol(Integer idRol);
}
