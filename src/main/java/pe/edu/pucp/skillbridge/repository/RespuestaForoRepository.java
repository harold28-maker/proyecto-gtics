package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.RespuestaForo;
import java.util.List;

public interface RespuestaForoRepository extends JpaRepository<RespuestaForo, Integer> {
    List<RespuestaForo> findByForoIdForoOrderByFechaCreacionAsc(Integer idForo);
    long countByForoIdForo(Integer idForo);
}
