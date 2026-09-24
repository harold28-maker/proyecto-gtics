package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findByRolNombre(String nombre);
    Optional<Usuario> findFirstByRolNombreOrderByIdUsuarioAsc(String nombre);
    Optional<Usuario> findByCorreo(String correo);
}
