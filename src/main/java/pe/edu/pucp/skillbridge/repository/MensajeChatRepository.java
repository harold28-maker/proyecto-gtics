package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.MensajeChat;
import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {
    List<MensajeChat> findByProyectoIdProyectoOrderByFechaEnvioAsc(Integer idProyecto);
}
