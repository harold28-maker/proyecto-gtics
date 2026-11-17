package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Auditoria;
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
}
