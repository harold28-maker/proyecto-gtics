package pe.edu.pucp.skillbridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.skillbridge.entity.Etiqueta;

/**
 * Repositorio de acceso a datos para las etiquetas utilizadas en el sistema.
 */
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Integer> {
}
