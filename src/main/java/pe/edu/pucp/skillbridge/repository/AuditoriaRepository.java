package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.Auditoria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para los registros de auditoría del sistema.
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    /**
     * Obtiene los cincuenta registros de auditoría más recientes.
     *
     * @return lista con los registros más recientes de auditoría
     */
    List<Auditoria> findTop50ByOrderByFechaDesc();

    /**
     * Busca coincidencias en todo el historial y pagina después de aplicar los filtros.
     */
    @EntityGraph(attributePaths = "usuario")
    @Query("""
            select a from Auditoria a
            left join a.usuario u
            where (:modulo = '' or lower(trim(a.modulo)) = lower(:modulo))
              and (:desde is null or a.fecha >= :desde)
              and (:hasta is null or a.fecha < :hasta)
              and (:patron = ''
                or lower(trim(concat(coalesce(u.nombres, ''), ' ', coalesce(u.apellidos, '')))) like :patron escape '!'
                or lower(a.accion) like :patron escape '!'
                or lower(a.modulo) like :patron escape '!'
                or lower(a.detalle) like :patron escape '!')
            order by a.fecha desc, a.idAuditoria desc
            """)
    Page<Auditoria> buscar(
            @Param("patron") String patron,
            @Param("modulo") String modulo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pagina
    );

    /**
     * Obtiene los módulos presentes en los registros de auditoría.
     */
    @Query("""
            select distinct trim(a.modulo) from Auditoria a
            where a.modulo is not null and trim(a.modulo) <> ''
            order by trim(a.modulo)
            """)
    List<String> listarModulos();
}
