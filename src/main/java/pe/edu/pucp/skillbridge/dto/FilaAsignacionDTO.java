package pe.edu.pucp.skillbridge.dto;

import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Proyecto;

import java.time.LocalDate;

public record FilaAsignacionDTO(Asignacion asignacion, EstadoParticipacion estado) {
    public static FilaAsignacionDTO desde(Asignacion asignacion, LocalDate hoy) {
        return new FilaAsignacionDTO(asignacion, estadoParticipacion(asignacion, hoy));
    }

    public LocalDate fechaOrden() {
        if (asignacion.getFechaFin() != null) return asignacion.getFechaFin();
        if (asignacion.getProyecto().getFechaFin() != null) return asignacion.getProyecto().getFechaFin();
        return asignacion.getFechaInicio();
    }

    private static EstadoParticipacion estadoParticipacion(Asignacion asignacion, LocalDate hoy) {
        Asignacion.EstadoAsignacion estado = asignacion.getEstado();
        if (estado == Asignacion.EstadoAsignacion.CANCELADA) return EstadoParticipacion.CANCELADA;
        if (estado == Asignacion.EstadoAsignacion.FINALIZADA) return EstadoParticipacion.FINALIZADA;
        Proyecto.EstadoProyecto proyecto = asignacion.getProyecto().getEstado();
        if (proyecto == Proyecto.EstadoProyecto.CANCELLED) return EstadoParticipacion.CANCELADA;
        if (proyecto == Proyecto.EstadoProyecto.COMPLETED) return EstadoParticipacion.FINALIZADA;
        if (asignacion.esHistorica(hoy)) return EstadoParticipacion.VENCIDA;
        return asignacion.estaVigente(hoy) ? EstadoParticipacion.VIGENTE : EstadoParticipacion.PLANIFICADA;
    }

    public enum EstadoParticipacion {
        VIGENTE("Vigente", "badge-success", false),
        PLANIFICADA("Planificada", "badge-blue", false),
        FINALIZADA("Finalizada", "badge-success", true),
        CANCELADA("Cancelada", "badge-danger", true),
        VENCIDA("Vencida", "badge-warning", true);

        private final String etiqueta;
        private final String estilo;
        private final boolean historica;

        EstadoParticipacion(String etiqueta, String estilo, boolean historica) {
            this.etiqueta = etiqueta;
            this.estilo = estilo;
            this.historica = historica;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getEstilo() { return estilo; }
        public boolean historica() { return historica; }
    }
}
