package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import java.util.List;

public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
    List<Asignacion> findByColaboradorIdColaboradorOrderByFechaInicioDesc(Integer idColaborador);
    List<Asignacion> findByProyectoIdProyectoOrderByFechaInicioDesc(Integer idProyecto);
    List<Asignacion> findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(Integer idUsuario);
}
