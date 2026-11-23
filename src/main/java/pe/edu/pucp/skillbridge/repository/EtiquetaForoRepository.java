package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.EtiquetaForo;

import java.util.List;

public interface EtiquetaForoRepository extends JpaRepository<EtiquetaForo, Integer> {
    List<EtiquetaForo> findAllByOrderByNombreAsc();
}
