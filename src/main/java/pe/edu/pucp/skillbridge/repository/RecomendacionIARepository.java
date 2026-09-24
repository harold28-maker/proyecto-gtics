package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.RecomendacionIA;
import java.util.List;

public interface RecomendacionIARepository extends JpaRepository<RecomendacionIA, Integer> {
    List<RecomendacionIA> findAllByOrderByPorcentajeMatchDesc();
    List<RecomendacionIA> findByProyectoIdProyectoOrderByPorcentajeMatchDesc(Integer idProyecto);
}
