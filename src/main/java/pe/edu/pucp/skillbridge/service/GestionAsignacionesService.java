package pe.edu.pucp.skillbridge.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.AsignacionFormulario;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Asignacion.EstadoAsignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService.CapacidadPeriodo;

import java.time.LocalDate;
import java.util.List;

@Service
public class GestionAsignacionesService {
    private final AsignacionRepository asignaciones;
    private final ProyectoRepository proyectos;
    private final ColaboradorRepository colaboradores;
    private final CapacidadColaboradorService capacidad;

    public GestionAsignacionesService(AsignacionRepository asignaciones, ProyectoRepository proyectos,
                                      ColaboradorRepository colaboradores, CapacidadColaboradorService capacidad) {
        this.asignaciones = asignaciones;
        this.proyectos = proyectos;
        this.colaboradores = colaboradores;
        this.capacidad = capacidad;
    }

    public List<Proyecto> proyectosDisponibles(Usuario gestor) {
        if ("PROJECT_MANAGER".equals(gestor.getRol().getNombre())) {
            return proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(gestor.getIdUsuario());
        }
        if ("RESOURCE_MANAGER".equals(gestor.getRol().getNombre())) {
            return proyectos.findAll(Sort.by("nombre", "idProyecto"));
        }
        return List.of();
    }

    public List<Proyecto> proyectosVigentes(Usuario gestor) {
        return proyectosDisponibles(gestor).stream()
                .filter(CapacidadColaboradorService::proyectoVigente)
                .toList();
    }

    public Asignacion consultarAsignacion(Integer id, Usuario gestor) {
        return asignaciones.findById(id).filter(a -> puedeGestionar(gestor, a.getProyecto()))
                .orElseThrow(GestionAsignacionesService::noEncontrada);
    }

    @Transactional(readOnly = true)
    public CapacidadPeriodo consultarPeriodo(AsignacionFormulario formulario, Usuario gestor) {
        Asignacion registrada = formulario.getIdAsignacion() == null ? null
                : consultarAsignacion(formulario.getIdAsignacion(), gestor);
        validarEstadoFormulario(formulario.getEstado(), registrada);
        Proyecto proyecto = consultarProyecto(formulario.getIdProyecto(), gestor);
        Colaborador colaborador = colaboradores.findById(formulario.getIdColaborador())
                .orElseThrow(GestionAsignacionesService::colaboradorInvalido);
        if (CapacidadColaboradorService.ocupaCapacidad(formulario.getEstado())) validarSeleccion(proyecto, colaborador);
        return capacidad.consultarPeriodo(colaborador, formulario.getFechaInicio(),
                formulario.getFechaFin(), formulario.getIdAsignacion());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void guardar(AsignacionFormulario formulario, Usuario gestor) {
        Integer id = formulario.getIdAsignacion();
        Asignacion registrada = id == null ? null : buscarParaActualizar(id, gestor);
        validarEstadoFormulario(formulario.getEstado(), registrada);
        Asignacion asignacion = registrada == null ? new Asignacion() : registrada;
        Proyecto proyecto = consultarProyecto(formulario.getIdProyecto(), gestor);
        Colaborador colaborador = colaboradores.buscarParaActualizar(formulario.getIdColaborador())
                .orElseThrow(GestionAsignacionesService::colaboradorInvalido);

        if (CapacidadColaboradorService.ocupaCapacidad(formulario.getEstado())) {
            validarDedicacion(proyecto, colaborador, formulario.getFechaInicio(), formulario.getFechaFin(),
                    formulario.getPorcentajeDedicacion(), id);
        }
        asignacion.setProyecto(proyecto);
        asignacion.setColaborador(colaborador);
        asignacion.setRolProyecto(formulario.getRolProyecto().strip());
        asignacion.setFechaInicio(formulario.getFechaInicio());
        asignacion.setFechaFin(formulario.getFechaFin());
        asignacion.setPorcentajeDedicacion(formulario.getPorcentajeDedicacion());
        asignacion.setEstado(formulario.getEstado());
        asignaciones.save(asignacion);
    }

    public Asignacion consultarCambioEstado(Integer id, EstadoAsignacion estado, Usuario gestor) {
        Asignacion asignacion = consultarAsignacion(id, gestor);
        validarCambioEstado(asignacion, estado);
        return asignacion;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cambiarEstado(Integer id, EstadoAsignacion estado, Usuario gestor) {
        Asignacion asignacion = buscarParaActualizar(id, gestor);
        validarCambioEstado(asignacion, estado);
        if (estado == EstadoAsignacion.ACTIVA) {
            Colaborador colaborador = colaboradores.buscarParaActualizar(asignacion.getColaborador().getIdColaborador())
                    .orElseThrow(GestionAsignacionesService::colaboradorInvalido);
            validarDedicacion(asignacion.getProyecto(), colaborador, asignacion.getFechaInicio(),
                    asignacion.getFechaFin(), asignacion.getPorcentajeDedicacion(), id);
        }
        asignacion.setEstado(estado);
    }

    private void validarEstadoFormulario(EstadoAsignacion estado, Asignacion registrada) {
        if (registrada == null && !CapacidadColaboradorService.ocupaCapacidad(estado)) {
            throw new CampoInvalido("estado", "Una nueva asignación debe estar planificada o activa.");
        }
        if (registrada != null && estado != registrada.getEstado()) {
            throw new CampoInvalido("estado",
                    "El estado se cambia desde Asignaciones. Vuelve a abrir el formulario para continuar.");
        }
    }

    private void validarCambioEstado(Asignacion asignacion, EstadoAsignacion destino) {
        if (asignacion.getEstado() == null || !asignacion.getEstado().permiteCambiarA(destino)) {
            throw new CampoInvalido("estado", "El estado actual de la asignación no permite este cambio.");
        }
    }

    private void validarDedicacion(Proyecto proyecto, Colaborador colaborador, LocalDate inicio, LocalDate fin,
                                   Integer porcentaje, Integer idAsignacion) {
        validarSeleccion(proyecto, colaborador);
        if (inicio == null || (fin != null && fin.isBefore(inicio))) {
            throw new CampoInvalido("fechaInicio", "Revisa las fechas de la asignación.");
        }
        if (porcentaje == null || porcentaje < 1 || porcentaje > 100) {
            throw new CampoInvalido("porcentajeDedicacion", "La dedicación debe estar entre 1 % y 100 %.");
        }
        CapacidadPeriodo periodo = capacidad.consultarPeriodo(colaborador, inicio, fin, idAsignacion);
        if (porcentaje > periodo.disponible()) {
            throw new CampoInvalido("porcentajeDedicacion",
                    "La dedicación supera el " + periodo.disponible() + " % disponible para ese período.");
        }
    }

    private void validarSeleccion(Proyecto proyecto, Colaborador colaborador) {
        if (!CapacidadColaboradorService.proyectoVigente(proyecto)) {
            throw new CampoInvalido("idProyecto", "Selecciona un proyecto vigente para reservar capacidad.");
        }
        if (colaborador.getUsuario().getEstado() != Usuario.EstadoUsuario.ACTIVO) {
            throw new CampoInvalido("idColaborador", "Selecciona un colaborador activo.");
        }
    }

    private Asignacion buscarParaActualizar(Integer id, Usuario gestor) {
        return asignaciones.buscarParaActualizar(id).filter(a -> puedeGestionar(gestor, a.getProyecto()))
                .orElseThrow(GestionAsignacionesService::noEncontrada);
    }

    private Proyecto consultarProyecto(Integer id, Usuario gestor) {
        return proyectos.findById(id).filter(p -> puedeGestionar(gestor, p))
                .orElseThrow(() -> new CampoInvalido("idProyecto", "Selecciona un proyecto que puedas gestionar."));
    }

    private boolean puedeGestionar(Usuario gestor, Proyecto proyecto) {
        String rol = gestor.getRol().getNombre();
        return "RESOURCE_MANAGER".equals(rol) || ("PROJECT_MANAGER".equals(rol)
                && proyecto.getProjectManager() != null
                && gestor.getIdUsuario().equals(proyecto.getProjectManager().getIdUsuario()));
    }

    private static CampoInvalido colaboradorInvalido() {
        return new CampoInvalido("idColaborador", "Selecciona un colaborador registrado.");
    }

    private static ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "La asignación no está disponible.");
    }

    public static class CampoInvalido extends RuntimeException {
        private final String campo;

        public CampoInvalido(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }

        public String getCampo() { return campo; }
    }
}
