package pe.edu.pucp.skillbridge.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.CategoriaHabilidad;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las categorías y de su uso en el catálogo de habilidades.
 */
public interface CategoriaHabilidadRepository extends JpaRepository<CategoriaHabilidad, Integer> {

    List<CategoriaHabilidad> findAllByOrderByNombreAsc();

    /**
     * Mantiene estable el nombre de la categoría mientras se registra una habilidad.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from CategoriaHabilidad c where c.nombre = :nombre")
    Optional<CategoriaHabilidad> buscarParaAsignar(@Param("nombre") String nombre);

    /**
     * Bloquea el registro durante el cambio de nombre y de sus asociaciones.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CategoriaHabilidad c where c.idCategoria = :id")
    Optional<CategoriaHabilidad> buscarParaRenombrar(@Param("id") Integer id);

    @Query(value = """
            SELECT c.id_categoria AS idCategoria, c.nombre,
                   COUNT(h.id_habilidad) AS totalHabilidades,
                   SUM(CASE WHEN h.estado = TRUE THEN 1 ELSE 0 END) AS activas
            FROM categorias_habilidades c
            LEFT JOIN habilidades h ON h.categoria COLLATE utf8mb4_0900_as_ci = c.nombre
            GROUP BY c.id_categoria, c.nombre
            ORDER BY c.nombre
            """, nativeQuery = true)
    List<ResumenCategoria> resumirCatalogo();

    interface ResumenCategoria {
        Integer getIdCategoria();
        String getNombre();
        long getTotalHabilidades();
        long getActivas();
    }
}
