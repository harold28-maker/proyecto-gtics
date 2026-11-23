package pe.edu.pucp.skillbridge.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.skillbridge.entity.RecuperacionContrasena;

import java.util.Optional;

public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, Integer> {

    Optional<RecuperacionContrasena> findByTokenHash(String tokenHash);

    @Query("select r.idUsuario from RecuperacionContrasena r where r.tokenHash = :tokenHash")
    Optional<Integer> buscarIdPorHuella(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecuperacionContrasena r where r.idUsuario = :idUsuario")
    Optional<RecuperacionContrasena> buscarParaActualizar(@Param("idUsuario") Integer idUsuario);
}
