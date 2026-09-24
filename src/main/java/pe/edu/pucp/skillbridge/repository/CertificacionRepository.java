package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Certificacion;
import java.util.List;

public interface CertificacionRepository extends JpaRepository<Certificacion, Integer> {
    List<Certificacion> findByColaboradorIdColaboradorOrderByFechaEmisionDesc(Integer idColaborador);
}
