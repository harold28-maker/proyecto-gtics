package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.RecomendacionIA;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para las recomendaciones generadas por inteligencia artificial.
 */
public interface RecomendacionIARepository extends JpaRepository<RecomendacionIA, Integer> {
    /**
     * Busca todas las recomendaciones y las ordena por porcentaje de coincidencia descendente.
     *
     * @return lista de recomendaciones desde la coincidencia más alta hasta la más baja
     */
    List<RecomendacionIA> findAllByOrderByPorcentajeMatchDesc();
    /**
     * Busca las recomendaciones asociadas a un proyecto y las ordena por porcentaje de coincidencia descendente.
     *
     * @param idProyecto identificador del proyecto cuyas recomendaciones se quieren consultar
     * @return lista de recomendaciones del proyecto, desde la coincidencia más alta hasta la más baja
     */
    List<RecomendacionIA> findByProyectoIdProyectoOrderByPorcentajeMatchDesc(Integer idProyecto);
}
