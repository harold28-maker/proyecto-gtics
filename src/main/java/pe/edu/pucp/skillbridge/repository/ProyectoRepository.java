package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Proyecto;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Integer> {
    List<Proyecto> findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
    List<Proyecto> findByEstadoOrderByFechaInicioDesc(Proyecto.EstadoProyecto estado);
    List<Proyecto> findByNombreContainingIgnoreCase(String nombre);

    @Query(value = """
            SELECT h.id_habilidad, h.nombre, h.categoria, ph.nivel_requerido, ph.vacantes
            FROM proyecto_habilidad ph
            JOIN habilidades h ON h.id_habilidad = ph.id_habilidad
            WHERE ph.id_proyecto = ?1
            ORDER BY h.categoria, h.nombre
            """, nativeQuery = true)
    List<Object[]> obtenerHabilidadesRequeridas(Integer idProyecto);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO proyecto_habilidad(id_proyecto, id_habilidad, nivel_requerido, vacantes)
            VALUES (?1, ?2, ?3, ?4)
            ON DUPLICATE KEY UPDATE nivel_requerido = VALUES(nivel_requerido), vacantes = VALUES(vacantes)
            """, nativeQuery = true)
    int guardarHabilidadRequerida(Integer idProyecto, Integer idHabilidad, String nivel, Integer vacantes);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM proyecto_habilidad
            WHERE id_proyecto = ?1 AND id_habilidad = ?2
            """, nativeQuery = true)
    int eliminarHabilidadRequerida(Integer idProyecto, Integer idHabilidad);
}
