package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Rol;
import java.util.Optional;

/**
 * Define las operaciones de acceso a datos para los roles.
 */
public interface RolRepository extends JpaRepository<Rol, Integer> {
    /**
     * Busca un rol por su nombre.
     *
     * @param nombre nombre del rol que se quiere consultar
     * @return el rol encontrado o un resultado vacío si no existe
     */
    Optional<Rol> findByNombre(String nombre);
}
