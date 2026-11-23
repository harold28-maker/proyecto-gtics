package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Proyecto;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de acceso a datos para las asignaciones de colaboradores a proyectos.
 */
public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
    /**
     * Obtiene las asignaciones de un colaborador ordenadas desde la más reciente.
     *
     * @param idColaborador identificador del colaborador cuyas asignaciones se consultan
     * @return lista de asignaciones asociadas al colaborador
     */
    List<Asignacion> findByColaboradorIdColaboradorOrderByFechaInicioDesc(Integer idColaborador);

    /**
     * Obtiene las asignaciones registradas para un proyecto ordenadas desde la más reciente.
     *
     * @param idProyecto identificador del proyecto cuyas asignaciones se consultan
     * @return lista de asignaciones asociadas al proyecto
     */
    List<Asignacion> findByProyectoIdProyectoOrderByFechaInicioDesc(Integer idProyecto);

    /**
     * Obtiene las asignaciones de los proyectos administrados por un gestor ordenadas desde la más reciente.
     *
     * @param idUsuario identificador del usuario que actúa como gestor de proyectos
     * @return lista de asignaciones pertenecientes a proyectos administrados por el gestor
     */
    List<Asignacion> findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(Integer idUsuario);

    @EntityGraph(attributePaths = {"proyecto", "colaborador.usuario"})
    @Query("""
            select a from Asignacion a
            where a.estado = :activa and a.proyecto.estado in :estadosProyecto
              and a.fechaInicio <= :fecha and (a.fechaFin is null or a.fechaFin >= :fecha)
              and (:idColaborador is null or a.colaborador.idColaborador = :idColaborador)
            order by a.fechaInicio desc, a.idAsignacion desc
            """)
    List<Asignacion> buscarVigentes(@Param("fecha") LocalDate fecha,
                                   @Param("idColaborador") Integer idColaborador,
                                   @Param("activa") Asignacion.EstadoAsignacion activa,
                                   @Param("estadosProyecto") List<Proyecto.EstadoProyecto> estadosProyecto);

    @EntityGraph(attributePaths = {"proyecto"})
    @Query("""
            select a from Asignacion a
            where a.colaborador.idColaborador = :idColaborador and a.proyecto.estado in :estadosProyecto
              and ((a.estado = :planificada and a.fechaInicio >= :fecha)
                   or (a.estado = :activa and a.fechaInicio > :fecha))
            order by a.fechaInicio asc, a.idAsignacion asc
            """)
    List<Asignacion> buscarProximas(@Param("idColaborador") Integer idColaborador,
                                   @Param("fecha") LocalDate fecha,
                                   @Param("planificada") Asignacion.EstadoAsignacion planificada,
                                   @Param("activa") Asignacion.EstadoAsignacion activa,
                                   @Param("estadosProyecto") List<Proyecto.EstadoProyecto> estadosProyecto,
                                   Pageable pagina);

    @EntityGraph(attributePaths = {"proyecto", "colaborador.usuario"})
    List<Asignacion> findByEstadoInAndProyectoEstadoIn(
            List<Asignacion.EstadoAsignacion> estados, List<Proyecto.EstadoProyecto> estadosProyecto);
}
