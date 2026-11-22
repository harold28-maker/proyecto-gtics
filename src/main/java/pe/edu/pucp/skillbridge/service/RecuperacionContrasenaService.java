package pe.edu.pucp.skillbridge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.pucp.skillbridge.entity.RecuperacionContrasena;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.RecuperacionContrasenaRepository;
import pe.edu.pucp.skillbridge.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class RecuperacionContrasenaService {

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionContrasenaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final int VIGENCIA_MINUTOS = 30;
    private static final int ESPERA_MINUTOS = 1;

    private final UsuarioRepository usuarios;
    private final RecuperacionContrasenaRepository recuperaciones;
    private final PasswordEncoder encoder;
    private final CorreoCuentaService correos;
    private final TaskExecutor executor;
    private final TransactionTemplate transacciones;
    private final SessionRegistry sesiones;

    public RecuperacionContrasenaService(UsuarioRepository usuarios, RecuperacionContrasenaRepository recuperaciones,
                                        PasswordEncoder encoder, CorreoCuentaService correos,
                                        @Qualifier("recuperacionExecutor") TaskExecutor executor,
                                        TransactionTemplate transacciones, SessionRegistry sesiones) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.encoder = encoder;
        this.correos = correos;
        this.executor = executor;
        this.transacciones = transacciones;
        this.sesiones = sesiones;
    }

    public void solicitar(String correo) {
        try {
            executor.execute(() -> enviarEnlace(correo));
        } catch (TaskRejectedException ex) {
            LOG.warn("La cola de recuperación de contraseñas está ocupada.");
        }
    }

    private void enviarEnlace(String correo) {
        try {
            var encontrado = usuarios.findByCorreoIgnoreCase(correo);
            if (encontrado.isEmpty()) {
                return;
            }

            Enlace enlace = transacciones.execute(estado -> prepararEnlace(encontrado.get().getIdUsuario(), correo));
            if (enlace != null) {
                correos.enviarRecuperacion(enlace.correo(), enlace.nombre(), enlace.token());
            }
        } catch (DataAccessException | TransactionException ex) {
            LOG.warn("No se pudo preparar una solicitud de recuperación ({}).", ex.getClass().getSimpleName());
        }
    }

    private Enlace prepararEnlace(Integer idUsuario, String correo) {
        // Se bloquea primero el usuario para serializar las solicitudes y el consumo del enlace.
        Usuario usuario = usuarios.buscarParaActualizar(idUsuario).orElse(null);
        if (usuario == null || usuario.getEstado() != Usuario.EstadoUsuario.ACTIVO
                || !usuario.getCorreo().equalsIgnoreCase(correo)) {
            return null;
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        var solicitud = recuperaciones.buscarParaActualizar(idUsuario);
        if (solicitud.isPresent() && solicitud.get().getSolicitadoEn().plusMinutes(ESPERA_MINUTOS).isAfter(ahora)) {
            return null;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RecuperacionContrasena registro = solicitud.orElseGet(() -> new RecuperacionContrasena(idUsuario));
        registro.renovar(huella(token), huellaCuenta(usuario), ahora, ahora.plusMinutes(VIGENCIA_MINUTOS));
        recuperaciones.save(registro);
        return new Enlace(usuario.getCorreo(), usuario.getNombreCompleto(), token);
    }

    public String verificarEnlace(String token) {
        if (token == null || !TOKEN.matcher(token).matches()) {
            return null;
        }
        String tokenHash = huella(token);
        return vigente(tokenHash) ? tokenHash : null;
    }

    public boolean vigente(String tokenHash) {
        if (tokenHash == null) {
            return false;
        }

        return recuperaciones.findByTokenHash(tokenHash)
                .filter(registro -> usuarios.findById(registro.getIdUsuario())
                        .filter(usuario -> vigentePara(registro, usuario))
                        .isPresent())
                .isPresent();
    }

    public boolean cambiarContrasena(String tokenHash, String nuevaContrasena) {
        String correo = transacciones.execute(estado -> consumirEnlace(tokenHash, nuevaContrasena));
        if (correo == null) {
            return false;
        }

        sesiones.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UserDetails usuario && correo.equalsIgnoreCase(usuario.getUsername()))
                .flatMap(principal -> sesiones.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
        return true;
    }

    private String consumirEnlace(String tokenHash, String nuevaContrasena) {
        Integer idUsuario = recuperaciones.buscarIdPorHuella(tokenHash).orElse(null);
        if (idUsuario == null) {
            return null;
        }

        Usuario usuario = usuarios.buscarParaActualizar(idUsuario).orElse(null);
        RecuperacionContrasena registro = recuperaciones.buscarParaActualizar(idUsuario).orElse(null);
        if (usuario == null || registro == null || !registro.getTokenHash().equals(tokenHash)
                || !vigentePara(registro, usuario)) {
            return null;
        }

        usuario.setPasswordHash(encoder.encode(nuevaContrasena));
        recuperaciones.delete(registro);
        return usuario.getCorreo();
    }

    private boolean vigentePara(RecuperacionContrasena registro, Usuario usuario) {
        return usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO
                && registro.getExpiraEn().isAfter(LocalDateTime.now(ZoneOffset.UTC))
                && registro.getHuellaCuenta().equals(huellaCuenta(usuario));
    }

    private static String huellaCuenta(Usuario usuario) {
        return huella(usuario.getCorreo() + "\n" + usuario.getPasswordHash());
    }

    private static String huella(String valor) {
        try {
            var sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private record Enlace(String correo, String nombre, String token) {}
}
