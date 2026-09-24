package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Foro;
import java.util.List;

public interface ForoRepository extends JpaRepository<Foro, Integer> {
    List<Foro> findAllByOrderByFechaCreacionDesc();
    List<Foro> findByProyectoIdProyectoOrderByFechaCreacionDesc(Integer idProyecto);
}
