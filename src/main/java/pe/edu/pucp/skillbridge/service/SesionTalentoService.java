package pe.edu.pucp.skillbridge.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.Interpretacion;
import pe.edu.pucp.skillbridge.service.TalentoMatchingService.Resultado;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SesionTalentoService {
    private final GestionAsignacionesService asignaciones;
    private final TalentoMatchingService matching;

    public SesionTalentoService(GestionAsignacionesService asignaciones, TalentoMatchingService matching) {
        this.asignaciones = asignaciones;
        this.matching = matching;
    }

    public record Turno(String mensaje, String respuesta) {}

    public record Conversacion(String revision, Integer idUsuario, Integer idProyecto, LocalDate desde,
                               LocalDate hasta, Integer dedicacion, Interpretacion criterios,
                               Interpretacion ultimaInterpretacion, List<Turno> turnos) {}

    public record Consulta(String proyecto, LocalDate desde, LocalDate hasta, Resultado resultado) {}

    public Conversacion obtener(HttpSession sesion, Usuario gestor) {
        Object valor = sesion.getAttribute(atributo(gestor));
        return valor instanceof Conversacion c && Objects.equals(c.idUsuario(), gestor.getIdUsuario()) ? c : null;
    }

    public void guardar(HttpSession sesion, Usuario gestor, Conversacion conversacion) {
        sesion.setAttribute(atributo(gestor), conversacion);
    }

    public void limpiar(HttpSession sesion, Usuario gestor) {
        sesion.removeAttribute(atributo(gestor));
    }

    public Resultado resultado(Conversacion conversacion, boolean verHistorial) {
        return matching.buscar(conversacion.ultimaInterpretacion(), conversacion.desde(), conversacion.hasta(),
                conversacion.dedicacion(), verHistorial);
    }

    public Consulta ultimaConsulta(HttpSession sesion, Usuario gestor, boolean verHistorial) {
        Conversacion conversacion = obtener(sesion, gestor);
        if (conversacion == null) return null;

        return asignaciones.proyectosVigentes(gestor).stream()
                .filter(p -> p.getIdProyecto().equals(conversacion.idProyecto()))
                .findFirst()
                .map(p -> new Consulta(p.getNombre(), conversacion.desde(), conversacion.hasta(),
                        resultado(conversacion, verHistorial)))
                .orElse(null);
    }

    private String atributo(Usuario gestor) {
        return "PROJECT_MANAGER".equals(gestor.getRol().getNombre())
                ? "conversacionTalento/pm/recomendaciones" : "conversacionTalento/resource/talent";
    }
}
