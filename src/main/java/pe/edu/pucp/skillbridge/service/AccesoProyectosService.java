package pe.edu.pucp.skillbridge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Proyectos que la cuenta puede consultar y aquellos en los que puede participar. */
@Service
@Transactional(readOnly = true)
public class AccesoProyectosService {
    private final AsignacionRepository asignaciones;
    private final ColaboradorRepository colaboradores;
    private final ProyectoRepository proyectos;

    public AccesoProyectosService(AsignacionRepository asignaciones, ColaboradorRepository colaboradores,
                                   ProyectoRepository proyectos) {
        this.asignaciones = asignaciones;
        this.colaboradores = colaboradores;
        this.proyectos = proyectos;
    }

    public record Acceso(Proyecto proyecto, boolean puedeParticipar) {}

    public Map<Integer, Acceso> consultar(Usuario usuario, Set<String> bloqueados) {
        Map<Integer, Acceso> resultado = new LinkedHashMap<>();
        boolean historial = !bloqueados.contains("historial");
        if ("PROJECT_MANAGER".equals(usuario.getRol().getNombre())) {
            for (Proyecto proyecto : proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())) {
                boolean abierto = proyectoAbierto(proyecto);
                if (abierto || historial) resultado.put(proyecto.getIdProyecto(), new Acceso(proyecto, abierto));
            }
            return resultado;
        }

        if (!"COLABORADOR".equals(usuario.getRol().getNombre())) return resultado;
        var colaborador = colaboradores.findByUsuarioIdUsuario(usuario.getIdUsuario()).orElse(null);
        if (colaborador == null) return resultado;

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        for (Asignacion asignacion : asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(colaborador.getIdColaborador())) {
            if (asignacion.getEstado() == Asignacion.EstadoAsignacion.CANCELADA
                    || (!historial && asignacion.esHistorica(hoy))) continue;
            Proyecto proyecto = asignacion.getProyecto();
            boolean puedeParticipar = asignacion.estaVigente(hoy) && proyectoAbierto(proyecto);
            resultado.merge(proyecto.getIdProyecto(), new Acceso(proyecto, puedeParticipar),
                    (actual, nuevo) -> nuevo.puedeParticipar() ? nuevo : actual);
        }
        return resultado;
    }

    private boolean proyectoAbierto(Proyecto proyecto) {
        return proyecto.getEstado() == Proyecto.EstadoProyecto.ACTIVE
                || proyecto.getEstado() == Proyecto.EstadoProyecto.PLANNING
                || proyecto.getEstado() == Proyecto.EstadoProyecto.ON_HOLD;
    }
}
