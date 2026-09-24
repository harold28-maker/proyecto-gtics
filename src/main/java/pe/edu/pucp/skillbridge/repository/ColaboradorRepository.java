package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Colaborador;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Integer> {
    Optional<Colaborador> findByUsuarioIdUsuario(Integer idUsuario);

    @Query(value = """
            SELECT c.id_colaborador,
                   CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
                   c.cargo,
                   c.area,
                   c.disponibilidad_base,
                   COALESCE(SUM(CASE WHEN a.estado IN ('PLANIFICADA','ACTIVA')
                                     THEN a.porcentaje_dedicacion ELSE 0 END), 0) AS carga,
                   GREATEST(c.disponibilidad_base - COALESCE(SUM(
                           CASE WHEN a.estado IN ('PLANIFICADA','ACTIVA')
                                THEN a.porcentaje_dedicacion ELSE 0 END), 0), 0) AS disponibilidad
            FROM colaboradores c
            JOIN usuarios u ON u.id_usuario = c.id_usuario
            LEFT JOIN asignaciones a ON a.id_colaborador = c.id_colaborador
            GROUP BY c.id_colaborador, u.nombres, u.apellidos, c.cargo, c.area, c.disponibilidad_base
            ORDER BY carga DESC, colaborador
            """, nativeQuery = true)
    List<Object[]> obtenerCargaColaboradores();

    @Query(value = """
            SELECT h.id_habilidad, h.nombre, h.categoria, ch.nivel, ch.anios_experiencia
            FROM colaborador_habilidad ch
            JOIN habilidades h ON h.id_habilidad = ch.id_habilidad
            WHERE ch.id_colaborador = ?1
            ORDER BY h.categoria, h.nombre
            """, nativeQuery = true)
    List<Object[]> obtenerHabilidades(Integer idColaborador);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO colaborador_habilidad(id_colaborador, id_habilidad, nivel, anios_experiencia)
            VALUES (?1, ?2, ?3, ?4)
            ON DUPLICATE KEY UPDATE nivel = VALUES(nivel), anios_experiencia = VALUES(anios_experiencia)
            """, nativeQuery = true)
    int guardarHabilidad(Integer idColaborador, Integer idHabilidad, String nivel, BigDecimal aniosExperiencia);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM colaborador_habilidad
            WHERE id_colaborador = ?1 AND id_habilidad = ?2
            """, nativeQuery = true)
    int eliminarHabilidad(Integer idColaborador, Integer idHabilidad);

    @Query(value = """
            SELECT DISTINCT c.*
            FROM colaboradores c
            JOIN usuarios u ON u.id_usuario = c.id_usuario
            LEFT JOIN colaborador_habilidad ch ON ch.id_colaborador = c.id_colaborador
            LEFT JOIN habilidades h ON h.id_habilidad = ch.id_habilidad
            WHERE LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', ?1, '%'))
               OR LOWER(c.cargo) LIKE LOWER(CONCAT('%', ?1, '%'))
               OR LOWER(h.nombre) LIKE LOWER(CONCAT('%', ?1, '%'))
            """, nativeQuery = true)
    List<Colaborador> buscar(String texto);
}
