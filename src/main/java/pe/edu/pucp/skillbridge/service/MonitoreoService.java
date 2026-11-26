package pe.edu.pucp.skillbridge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository.ConteoAccesos;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository.EventoMonitoreo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitoreoService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final AuditoriaRepository auditoria;

    public MonitoreoService(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    public record Evento(Integer id, String usuario, String accion, String modulo, String detalle,
                         LocalDateTime fecha, boolean nuevo) {}

    public record Resumen(long accesosHoy, long usuariosDistintos, LocalDateTime actualizado,
                          String idsAccesos, List<Evento> actividad) {}

    @Transactional(readOnly = true)
    public Resumen consultar(boolean mostrarAccesos) {
        LocalDateTime ahora = LocalDateTime.now(LIMA);
        LocalDate hoy = ahora.toLocalDate();
        long desde = hoy.atStartOfDay(LIMA).toEpochSecond();
        long hasta = hoy.plusDays(1).atStartOfDay(LIMA).toEpochSecond();
        String inicio = Auditoria.ACCION_INICIAR_SESION;
        String modulo = Auditoria.MODULO_AUTENTICACION;

        ConteoAccesos conteo = auditoria.contarAccesos(inicio, modulo, desde, hasta);
        List<EventoMonitoreo> accesos = auditoria.actividadReciente(inicio, modulo, true);
        String idsAccesos = accesos.stream()
                .map(acceso -> acceso.getId().toString())
                .collect(Collectors.joining(","));
        List<EventoMonitoreo> registros = mostrarAccesos ? accesos : auditoria.actividadReciente(inicio, modulo, false);
        List<Evento> actividad = registros.stream()
                .map(registro -> prepararEvento(registro, mostrarAccesos, ahora))
                .toList();

        return new Resumen(conteo.getAccesos(), conteo.getUsuarios(), ahora, idsAccesos, actividad);
    }

    private Evento prepararEvento(EventoMonitoreo registro, boolean acceso, LocalDateTime ahora) {
        Long segundos = registro.getSegundos();
        LocalDateTime fecha = segundos == null ? null : LocalDateTime.ofInstant(Instant.ofEpochSecond(segundos), LIMA);
        String usuario = registro.getUsuario();
        boolean nuevo = acceso && fecha != null && !fecha.isBefore(ahora.minusSeconds(15));
        return new Evento(registro.getId(), usuario == null || usuario.isBlank() ? "Sistema" : usuario,
                registro.getAccion(), registro.getModulo(), registro.getDetalle(), fecha, nuevo);
    }
}
