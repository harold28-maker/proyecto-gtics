package pe.edu.pucp.skillbridge.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.Foro;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para los temas del foro.
 */
public interface ForoRepository extends JpaRepository<Foro, Integer> {
    @EntityGraph(attributePaths = {"etiquetas"})
    List<Foro> findByProyectoIsNullOrderByFechaCreacionDescIdForoDesc();

    @EntityGraph(attributePaths = {"etiquetas"})
    List<Foro> findByProyectoIdProyectoInOrProyectoIsNullOrderByFechaCreacionDescIdForoDesc(Collection<Integer> proyectos);

    default List<Foro> listarVisibles(Collection<Integer> proyectos) {
        return proyectos.isEmpty() ? findByProyectoIsNullOrderByFechaCreacionDescIdForoDesc()
                : findByProyectoIdProyectoInOrProyectoIsNullOrderByFechaCreacionDescIdForoDesc(proyectos);
    }

    List<Foro> findByProyectoIdProyectoOrderByFechaCreacionDescIdForoDesc(Integer idProyecto, Pageable limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Foro f where f.idForo = :id")
    Optional<Foro> buscarParaActualizar(@Param("id") Integer id);

    /**
     * Obtiene todos los temas del foro ordenados por fecha de creación descendente.
     *
     * @return lista de temas del foro desde el más reciente
     */
    List<Foro> findAllByOrderByFechaCreacionDesc();

    /**
     * Obtiene los temas asociados a un proyecto ordenados por fecha de creación descendente.
     *
     * @param idProyecto identificador del proyecto cuyos temas se consultan
     * @return lista de temas asociados al proyecto
     */
    List<Foro> findByProyectoIdProyectoOrderByFechaCreacionDesc(Integer idProyecto);
}
