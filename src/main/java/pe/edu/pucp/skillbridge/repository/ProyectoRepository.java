package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Proyecto;

import java.util.List;

/**
 * Define las operaciones de acceso y modificación de datos para los proyectos.
 */
public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {
    /**
     * Busca los proyectos administrados por un usuario y los ordena por fecha de creación descendente.
     *
     * @param idUsuario identificador del usuario que administra los proyectos
     * @return lista de proyectos administrados por el usuario, desde el más reciente hasta el más antiguo
     */
    List<Proyecto> findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
    /**
     * Busca los proyectos que tienen un estado específico y los ordena por fecha de inicio descendente.
     *
     * @param estado estado que deben tener los proyectos consultados
     * @return lista de proyectos que tienen el estado indicado
     */
    List<Proyecto> findByEstadoOrderByFechaInicioDesc(Proyecto.EstadoProyecto estado);
    /**
     * Busca los proyectos cuyo nombre contiene el texto indicado, sin distinguir mayúsculas de minúsculas.
     *
     * @param nombre texto que debe estar contenido en el nombre del proyecto
     * @return lista de proyectos cuyos nombres coinciden con el texto indicado
     */
    List<Proyecto> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Obtiene las habilidades requeridas por un proyecto junto con su nivel requerido y número de vacantes.
     *
     * @param idProyecto identificador del proyecto cuyas habilidades se quieren consultar
     * @return filas con el identificador, nombre, categoría, nivel requerido y vacantes de cada habilidad
     */
    @Query(value = """
            SELECT h.id_habilidad, h.nombre, h.categoria, ph.nivel_requerido, ph.vacantes
            FROM proyecto_habilidad ph
            JOIN habilidades h ON h.id_habilidad = ph.id_habilidad
            WHERE ph.id_proyecto = ?1
            ORDER BY h.categoria, h.nombre
            """, nativeQuery = true)
    List<Object[]> obtenerHabilidadesRequeridas(Integer idProyecto);

    /**
     * Guarda una habilidad requerida por un proyecto o actualiza su nivel y vacantes si ya estaba registrada.
     *
     * @param idProyecto identificador del proyecto al que se asigna la habilidad
     * @param idHabilidad identificador de la habilidad requerida
     * @param nivel nivel requerido para la habilidad
     * @param vacantes número de vacantes asociadas a la habilidad
     * @return número de filas insertadas o actualizadas
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO proyecto_habilidad(id_proyecto, id_habilidad, nivel_requerido, vacantes)
            VALUES (?1, ?2, ?3, ?4)
            ON DUPLICATE KEY UPDATE nivel_requerido = VALUES(nivel_requerido), vacantes = VALUES(vacantes)
            """, nativeQuery = true)
    int guardarHabilidadRequerida(Integer idProyecto, Integer idHabilidad, String nivel, Integer vacantes);

    /**
     * Elimina la asignación de una habilidad de un proyecto determinado.
     *
     * @param idProyecto identificador del proyecto del que se elimina la habilidad
     * @param idHabilidad identificador de la habilidad que se elimina
     * @return número de filas eliminadas
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM proyecto_habilidad
            WHERE id_proyecto = ?1 AND id_habilidad = ?2
            """, nativeQuery = true)
    int eliminarHabilidadRequerida(Integer idProyecto, Integer idHabilidad);
}
