package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.RespuestaForo;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para las respuestas de los foros.
 */
public interface RespuestaForoRepository extends JpaRepository<RespuestaForo, Integer> {
    /**
     * Busca las respuestas de un foro y las ordena por fecha de creación ascendente.
     *
     * @param idForo identificador del foro cuyas respuestas se quieren consultar
     * @return lista de respuestas del foro, desde la más antigua hasta la más reciente
     */
    List<RespuestaForo> findByForoIdForoOrderByFechaCreacionAsc(Integer idForo);
    /**
     * Cuenta las respuestas registradas para un foro determinado.
     *
     * @param idForo identificador del foro cuya cantidad de respuestas se quiere obtener
     * @return número total de respuestas asociadas al foro
     */
    long countByForoIdForo(Integer idForo);
}
