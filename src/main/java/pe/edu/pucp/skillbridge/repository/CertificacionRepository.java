package pe.edu.pucp.skillbridge.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.Certificacion;

import java.util.List;
import java.util.Optional;

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
    List<Certificacion> findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(Integer idColaborador);

    Optional<Certificacion> findByIdCertificacionAndColaboradorIdColaborador(Integer id, Integer idColaborador);

    boolean existsByColaboradorIdColaboradorAndUrlCredencial(Integer idColaborador, String urlCredencial);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Certificacion c where c.idCertificacion = :id and c.colaborador.idColaborador = :idColaborador")
    Optional<Certificacion> buscarParaActualizar(@Param("id") Integer id, @Param("idColaborador") Integer idColaborador);
}
