package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Proyecto;

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
    List<Asignacion> findByEstadoInAndProyectoEstadoIn(
            List<Asignacion.EstadoAsignacion> estados, List<Proyecto.EstadoProyecto> estadosProyecto);
}
