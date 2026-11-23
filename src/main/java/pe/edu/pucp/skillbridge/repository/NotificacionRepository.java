package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Notificacion;

import java.util.Optional;

/**
 * Define las operaciones de acceso a datos para las notificaciones.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    Page<Notificacion> findByUsuarioIdUsuarioOrderByFechaCreacionDescIdNotificacionDesc(
            Integer idUsuario, Pageable pagina);

    @Query("""
            select n from Notificacion n
            where n.usuario.idUsuario = :idUsuario and (n.leida = false or n.leida is null)
            order by n.fechaCreacion desc, n.idNotificacion desc
            """)
    Page<Notificacion> buscarPendientes(@Param("idUsuario") Integer idUsuario, Pageable pagina);

    @Query("select count(n) from Notificacion n where n.usuario.idUsuario = :idUsuario and (n.leida = false or n.leida is null)")
    long contarPendientes(@Param("idUsuario") Integer idUsuario);

    long countByUsuarioIdUsuario(Integer idUsuario);

    Optional<Notificacion> findByIdNotificacionAndUsuarioIdUsuario(Integer idNotificacion, Integer idUsuario);

    @Transactional
    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuario.idUsuario = :idUsuario and (n.leida = false or n.leida is null)")
    int marcarTodasLeidas(@Param("idUsuario") Integer idUsuario);
}
