package pe.edu.pucp.skillbridge.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.skillbridge.dto.TablaReporte;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.service.ExportacionReporteService;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService.Grupo;
import pe.edu.pucp.skillbridge.service.ReporteProyectosService.Tipo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/pm/reportes")
public class ReportesProyectosController {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ProyectoRepository proyectos;
    private final ReporteProyectosService reportes;
    private final ExportacionReporteService exportador;

    public ReportesProyectosController(ProyectoRepository proyectos, ReporteProyectosService reportes,
                                       ExportacionReporteService exportador) {
        this.proyectos = proyectos;
        this.reportes = reportes;
        this.exportador = exportador;
    }

    @GetMapping
    public String reportes(@ModelAttribute("filtro") Filtro filtro, BindingResult errores,
                           @RequestParam(defaultValue = "") String modo, @RequestParam(defaultValue = "") String reporte,
                           @RequestAttribute("usuarioVerificado") Usuario pm,
                           @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        List<Proyecto> propios = portafolio(pm);
        validar(filtro, errores, propios);
        boolean dedicacion = disponible(Tipo.DEDICACION, bloqueados);
        boolean avance = disponible(Tipo.PROYECTOS, bloqueados);
        if (!modo.isBlank() && !"dedicacion".equals(modo) && !"avance".equals(modo)) {
            errores.reject("modo", "Selecciona Dedicación del equipo o Avance de proyectos.");
        }
        Tipo principal = !dedicacion || ("avance".equals(modo) && avance)
                ? (avance ? Tipo.PROYECTOS : null) : Tipo.DEDICACION;
        Tipo vistaPrevia = reporte.isBlank() ? null : Tipo.desde(reporte);
        if (!reporte.isBlank() && vistaPrevia == null) errores.reject("reporte", "Selecciona un reporte válido.");
        if (vistaPrevia != null) exigirPermiso(vistaPrevia, bloqueados);

        model.addAttribute("titulo", "Reportes");
        model.addAttribute("propios", propios);
        model.addAttribute("opcionesGrupo", Grupo.values());
        model.addAttribute("verDedicacion", dedicacion);
        model.addAttribute("verAvance", avance);
        model.addAttribute("modo", principal == Tipo.PROYECTOS ? "avance" : "dedicacion");
        model.addAttribute("principal", principal);
        model.addAttribute("filtrosValidos", !errores.hasErrors());
        model.addAttribute("otrosReportes", List.of(Tipo.CARGA, Tipo.HISTORIAL).stream()
                .filter(tipo -> disponible(tipo, bloqueados)).toList());
        if (!errores.hasErrors() && (principal != null || vistaPrevia != null)) {
            var datos = reportes.consultar(pm.getIdUsuario(), seleccionar(propios, filtro), filtro.getFecha(), filtro.getAsignaciones(),
                    necesitaAsignaciones(principal) || necesitaAsignaciones(vistaPrevia));
            model.addAttribute("datos", datos);
            if (vistaPrevia != null) {
                TablaReporte tabla = reportes.tabla(vistaPrevia, datos);
                model.addAttribute("tabla", tabla);
                model.addAttribute("filasVista", tabla.filas().stream()
                        .map(fila -> fila.stream().map(this::mostrarCelda).toList()).toList());
                model.addAttribute("vistaPrevia", vistaPrevia);
            }
        }
        return "pm/reportes";
    }

    @GetMapping(value = "/exportar", produces = "text/csv")
    public ResponseEntity<String> descargarCsv(@RequestParam String tipo,
                                                @ModelAttribute("filtro") Filtro filtro, BindingResult errores,
                                                @RequestAttribute("usuarioVerificado") Usuario pm,
                                                @RequestAttribute("permisosBloqueados") Set<String> bloqueados) {
        TablaReporte tabla = obtenerTabla(tipo, filtro, errores, pm, bloqueados);
        return ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, nombreArchivo(Tipo.desde(tipo), "csv"))
                .body(exportador.generarCsv(tabla));
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(@RequestParam String tipo,
                                                  @ModelAttribute("filtro") Filtro filtro, BindingResult errores,
                                                  @RequestAttribute("usuarioVerificado") Usuario pm,
                                                  @RequestAttribute("permisosBloqueados") Set<String> bloqueados) throws IOException {
        TablaReporte tabla = obtenerTabla(tipo, filtro, errores, pm, bloqueados);
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, nombreArchivo(Tipo.desde(tipo), "xlsx"))
                .body(exportador.generarExcel(tabla));
    }

    private TablaReporte obtenerTabla(String tipo, Filtro filtro, BindingResult errores, Usuario pm, Set<String> bloqueados) {
        Tipo elegido = Tipo.desde(tipo);
        if (elegido == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un reporte válido.");
        exigirPermiso(elegido, bloqueados);
        List<Proyecto> propios = portafolio(pm);
        validar(filtro, errores, propios);
        if (errores.hasErrors()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa los filtros del reporte.");
        var datos = reportes.consultar(pm.getIdUsuario(), seleccionar(propios, filtro), filtro.getFecha(),
                filtro.getAsignaciones(), necesitaAsignaciones(elegido));
        return reportes.tabla(elegido, datos);
    }

    private List<Proyecto> portafolio(Usuario pm) {
        return proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
    }

    private List<Proyecto> seleccionar(List<Proyecto> propios, Filtro filtro) {
        return filtro.getProyecto() == null ? propios : propios.stream()
                .filter(p -> p.getIdProyecto().equals(filtro.getProyecto())).toList();
    }

    private void validar(Filtro filtro, BindingResult errores, List<Proyecto> propios) {
        if (filtro.getProyecto() != null && propios.stream().noneMatch(p -> p.getIdProyecto().equals(filtro.getProyecto()))) {
            errores.rejectValue("proyecto", "proyecto", "Selecciona un proyecto a tu cargo.");
        }
        if (filtro.getFecha() == null) filtro.setFecha(LocalDate.now(LIMA));
        if (filtro.getFecha().getYear() < 1000 || filtro.getFecha().getYear() > 9999) {
            errores.rejectValue("fecha", "fecha", "Ingresa una fecha válida.");
        }
        if (filtro.getAsignaciones() == null) filtro.setAsignaciones(Grupo.TODAS);
    }

    private boolean necesitaAsignaciones(Tipo tipo) {
        return tipo == Tipo.DEDICACION || tipo == Tipo.ASIGNACIONES || tipo == Tipo.CARGA;
    }

    private String mostrarCelda(Object valor) {
        if (valor instanceof LocalDate dia) return dia.format(FORMATO_FECHA);
        if (valor instanceof Double fraccion) return Math.round(fraccion * 100) + " %";
        return valor == null ? "—" : valor.toString();
    }

    private boolean disponible(Tipo tipo, Set<String> bloqueados) {
        return !bloqueados.contains(tipo.getPermiso());
    }

    private void exigirPermiso(Tipo tipo, Set<String> bloqueados) {
        if (!disponible(tipo, bloqueados)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contacta a Administración para consultar este reporte.");
        }
    }

    private String nombreArchivo(Tipo tipo, String extension) {
        return ContentDisposition.attachment().filename("reporte_" + tipo.getClave() + "_pm." + extension).build().toString();
    }

    public static class Filtro {
        private Integer proyecto;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate fecha = LocalDate.now(LIMA);
        private Grupo asignaciones = Grupo.TODAS;

        public Integer getProyecto() { return proyecto; }
        public void setProyecto(Integer proyecto) { this.proyecto = proyecto; }
        public LocalDate getFecha() { return fecha; }
        public void setFecha(LocalDate fecha) { this.fecha = fecha; }
        public Grupo getAsignaciones() { return asignaciones; }
        public void setAsignaciones(Grupo asignaciones) { this.asignaciones = asignaciones; }
    }
}
