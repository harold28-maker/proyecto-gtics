package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Define las operaciones de acceso a datos para los usuarios.
 */
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    /**
     * Busca todos los usuarios cuyo rol tiene el nombre indicado.
     *
     * @param nombre nombre del rol asociado a los usuarios
     * @return lista de usuarios que tienen el rol indicado
     */
    List<Usuario> findByRolNombre(String nombre);

    /**
     * Busca el primer usuario de un rol según el identificador de usuario ascendente.
     *
     * @param nombre nombre del rol asociado al usuario buscado
     * @return el primer usuario encontrado para el rol o un resultado vacío si no existe
     */
    Optional<Usuario>
    findFirstByRolNombreOrderByIdUsuarioAsc(String nombre);

    /**
     * Busca un usuario por su correo electrónico sin distinguir mayúsculas de minúsculas.
     *
     * @param correo dirección de correo electrónico del usuario
     * @return el usuario encontrado o un resultado vacío si no existe
     */
    Optional<Usuario>
    findByCorreoIgnoreCase(String correo);
}