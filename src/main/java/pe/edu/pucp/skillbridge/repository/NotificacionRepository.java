package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Notificacion;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByUsuarioIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
}
