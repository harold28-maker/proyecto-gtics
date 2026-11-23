package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Colaborador;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para los colaboradores y sus habilidades.
 */
public interface ColaboradorRepository extends JpaRepository<Colaborador, Integer> {
    /**
     * Busca el colaborador asociado a un usuario específico.
     *
     * @param idUsuario identificador del usuario asociado al colaborador
     * @return colaborador encontrado o una ausencia si no existe una relación para el usuario
     */
    Optional<Colaborador> findByUsuarioIdUsuario(Integer idUsuario);

    /** Obtiene los perfiles y sus cuentas para el resumen de capacidad del equipo. */
    @Query("select c from Colaborador c join fetch c.usuario")
    List<Colaborador> listarConUsuario();

    /**
     * Obtiene las habilidades registradas para un colaborador.
     *
     * @param idColaborador identificador del colaborador cuyas habilidades se consultan
     * @return filas con el identificador, nombre, categoría, nivel y experiencia de cada habilidad
     */
    @Query(value = """
            SELECT h.id_habilidad, h.nombre, h.categoria, ch.nivel, ch.anios_experiencia
            FROM colaborador_habilidad ch
            JOIN habilidades h ON h.id_habilidad = ch.id_habilidad
            WHERE ch.id_colaborador = ?1
            ORDER BY h.categoria, h.nombre
            """, nativeQuery = true)
    List<Object[]> obtenerHabilidades(Integer idColaborador);

    /**
     * Registra o actualiza la habilidad y la experiencia asignadas a un colaborador.
     *
     * @param idColaborador identificador del colaborador
     * @param idHabilidad identificador de la habilidad
     * @param nivel nivel actual de dominio de la habilidad
     * @param aniosExperiencia años de experiencia asociados a la habilidad
     * @return número de filas insertadas o actualizadas
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO colaborador_habilidad(id_colaborador, id_habilidad, nivel, anios_experiencia)
            VALUES (?1, ?2, ?3, ?4)
            ON DUPLICATE KEY UPDATE nivel = VALUES(nivel), anios_experiencia = VALUES(anios_experiencia)
            """, nativeQuery = true)
    int guardarHabilidad(Integer idColaborador, Integer idHabilidad, String nivel, BigDecimal aniosExperiencia);

    /**
     * Actualiza el nivel y la experiencia de una habilidad ya registrada en el perfil.
     *
     * @param idColaborador identificador del colaborador
     * @param idHabilidad identificador de la habilidad
     * @param nivel nuevo nivel de dominio
     * @param aniosExperiencia años de experiencia
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE colaborador_habilidad
            SET nivel = ?3, anios_experiencia = ?4
            WHERE id_colaborador = ?1 AND id_habilidad = ?2
            """, nativeQuery = true)
    void actualizarHabilidad(Integer idColaborador, Integer idHabilidad, String nivel, BigDecimal aniosExperiencia);

    /**
     * Elimina la relación entre un colaborador y una habilidad.
     *
     * @param idColaborador identificador del colaborador
     * @param idHabilidad identificador de la habilidad que se eliminará
     * @return número de filas eliminadas
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM colaborador_habilidad
            WHERE id_colaborador = ?1 AND id_habilidad = ?2
            """, nativeQuery = true)
    int eliminarHabilidad(Integer idColaborador, Integer idHabilidad);

    /**
     * Busca colaboradores por coincidencias en nombre, cargo o habilidad.
     *
     * @param texto texto parcial que se compara con nombre, cargo y nombre de habilidad
     * @return lista de colaboradores que cumplen al menos una coincidencia
     */
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
