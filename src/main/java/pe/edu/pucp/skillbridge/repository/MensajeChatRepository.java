package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.MensajeChat;
import java.util.List;

/**
 * Repositorio de acceso a datos para los mensajes de chat asociados a proyectos.
 */
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {
    /**
     * Obtiene los mensajes de chat de un proyecto en orden cronológico.
     *
     * @param idProyecto identificador del proyecto cuyos mensajes se consultan
     * @return lista de mensajes del proyecto desde el más antiguo
     */
    List<MensajeChat> findByProyectoIdProyectoOrderByFechaEnvioAscIdMensajeAsc(Integer idProyecto);
}
