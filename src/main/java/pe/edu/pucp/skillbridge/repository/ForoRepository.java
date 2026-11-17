package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Foro;
import java.util.List;

/**
 * Repositorio de acceso a datos para los temas del foro.
 */
public interface ForoRepository extends JpaRepository<Foro, Integer> {
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
