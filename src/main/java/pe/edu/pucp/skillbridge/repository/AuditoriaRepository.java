package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    List<Auditoria> findTop50ByOrderByFechaDesc();
}
