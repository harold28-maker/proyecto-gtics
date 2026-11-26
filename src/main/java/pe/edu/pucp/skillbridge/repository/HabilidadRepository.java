package pe.edu.pucp.skillbridge.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.Habilidad;

import java.util.List;
import java.util.Optional;

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

    List<Habilidad> findByNombreIgnoreCase(String nombre);

    /**
     * Bloquea la habilidad durante una modificación de estado o el registro de una nueva asociación.
     *
     * @param id identificador de la habilidad
     * @return habilidad encontrada dentro de la transacción
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habilidad h where h.idHabilidad = :id")
    Optional<Habilidad> buscarParaActualizar(@Param("id") Integer id);

    /**
     * Cuenta las asociaciones de cada habilidad consultada.
     *
     * @param ids identificadores de las habilidades del catálogo
     * @return cantidad de colaboradores y proyectos vinculados por habilidad
     */
    @Query(value = """
            SELECT h.id_habilidad AS idHabilidad,
                   (SELECT COUNT(*) FROM colaborador_habilidad ch
                    WHERE ch.id_habilidad = h.id_habilidad) AS colaboradores,
                   (SELECT COUNT(*) FROM proyecto_habilidad ph
                    WHERE ph.id_habilidad = h.id_habilidad) AS proyectos
            FROM habilidades h
            WHERE h.id_habilidad IN (:ids)
            """, nativeQuery = true)
    List<UsoHabilidad> obtenerUsos(@Param("ids") List<Integer> ids);

    /**
     * Actualiza el nombre de la categoría en las habilidades vinculadas.
     */
    @Modifying
    @Query(value = """
            UPDATE habilidades
            SET categoria = :nuevo
            WHERE categoria COLLATE utf8mb4_0900_as_ci = :anterior
            """, nativeQuery = true)
    void renombrarCategoria(@Param("anterior") String anterior, @Param("nuevo") String nuevo);

    /**
     * Totales de asociaciones ya registradas para una habilidad.
     */
    interface UsoHabilidad {
        Integer getIdHabilidad();
        long getColaboradores();
        long getProyectos();
    }
}
