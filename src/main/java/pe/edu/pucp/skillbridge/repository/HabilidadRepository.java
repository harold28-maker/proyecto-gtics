package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Habilidad;
import java.util.List;

/**
 * Repositorio de acceso a datos para las habilidades disponibles en el sistema.
 */
public interface HabilidadRepository extends JpaRepository<Habilidad, Integer> {
    /**
     * Obtiene las habilidades activas ordenadas alfabéticamente por nombre.
     *
     * @return lista de habilidades cuyo estado es activo
     */
    List<Habilidad> findByEstadoTrueOrderByNombreAsc();

    /**
     * Busca habilidades cuyo nombre contiene el texto indicado, sin distinguir mayúsculas.
     *
     * @param nombre texto parcial que debe aparecer en el nombre de la habilidad
     * @return lista de habilidades cuyo nombre coincide parcialmente con el texto
     */
    List<Habilidad> findByNombreContainingIgnoreCase(String nombre);
}
