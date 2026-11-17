package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Notificacion;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para las notificaciones.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    /**
     * Busca las notificaciones de un usuario y las ordena por fecha de creación descendente.
     *
     * @param idUsuario identificador del usuario propietario de las notificaciones
     * @return lista de notificaciones del usuario, desde la más reciente hasta la más antigua
     */
    List<Notificacion> findByUsuarioIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
}
