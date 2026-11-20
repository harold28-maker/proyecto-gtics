package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.MensajeChatbot;

import java.util.List;

public interface MensajeChatbotRepository extends JpaRepository<MensajeChatbot, Long> {
    List<MensajeChatbot> findTop12ByUsuarioIdUsuarioOrderByIdMensajeChatbotDesc(Integer idUsuario);
    List<MensajeChatbot> findTop30ByUsuarioIdUsuarioOrderByIdMensajeChatbotDesc(Integer idUsuario);
    long deleteByUsuarioIdUsuario(Integer idUsuario);
}
