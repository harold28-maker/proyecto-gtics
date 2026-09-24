package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Habilidad;
import java.util.List;

public interface HabilidadRepository extends JpaRepository<Habilidad, Integer> {
    List<Habilidad> findByEstadoTrueOrderByNombreAsc();
    List<Habilidad> findByNombreContainingIgnoreCase(String nombre);
}
