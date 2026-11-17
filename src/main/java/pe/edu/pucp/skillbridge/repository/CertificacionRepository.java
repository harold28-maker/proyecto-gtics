package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Certificacion;
import java.util.List;

/**
 * Repositorio de acceso a datos para las certificaciones de los colaboradores.
 */
public interface CertificacionRepository extends JpaRepository<Certificacion, Integer> {
    /**
     * Obtiene las certificaciones de un colaborador ordenadas por fecha de emisión descendente.
     *
     * @param idColaborador identificador del colaborador cuyas certificaciones se consultan
     * @return lista de certificaciones asociadas al colaborador
     */
    List<Certificacion> findByColaboradorIdColaboradorOrderByFechaEmisionDesc(Integer idColaborador);
}
