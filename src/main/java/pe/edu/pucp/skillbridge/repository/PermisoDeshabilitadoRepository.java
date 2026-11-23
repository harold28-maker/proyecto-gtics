package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.PermisoDeshabilitado;

import java.util.List;
import java.util.Set;

public interface PermisoDeshabilitadoRepository extends JpaRepository<PermisoDeshabilitado, Long> {
    List<PermisoDeshabilitado> findByRol_IdRol(Integer idRol);

    @Query("select p.codigo from PermisoDeshabilitado p where p.rol.idRol = :idRol")
    Set<String> codigosDeshabilitados(@Param("idRol") Integer idRol);
}
