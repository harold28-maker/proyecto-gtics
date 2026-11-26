package pe.edu.pucp.skillbridge.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.pucp.skillbridge.dto.TablaReporte;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.service.ExportacionReporteService;
import pe.edu.pucp.skillbridge.service.ReporteRecursosService;
import pe.edu.pucp.skillbridge.service.ReporteRecursosService.Resumen;
import pe.edu.pucp.skillbridge.service.SesionTalentoService;
import pe.edu.pucp.skillbridge.util.ReportePdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Controller
@RequestMapping("/resource/reportes")
public class ReportesRecursosController {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReporteRecursosService reportes;
    private final ExportacionReporteService exportacion;
    private final SesionTalentoService talento;

    public ReportesRecursosController(ReporteRecursosService reportes, ExportacionReporteService exportacion,
                                     SesionTalentoService talento) {
        this.reportes = reportes;
        this.exportacion = exportacion;
        this.talento = talento;
    }

    @GetMapping
    public String reportes(@RequestParam(value = "reporte", defaultValue = "") String reporte,
                           @RequestParam(value = "vista", defaultValue = "barras") String vista,
                           @RequestAttribute("usuarioVerificado") Usuario gestor,
                           @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                           HttpSession sesion, HttpServletResponse respuesta, Model model) {
        Tipo tipo = Tipo.buscar(reporte);
        boolean matriz = "matriz".equalsIgnoreCase(vista);
        boolean vistaValida = matriz || "barras".equalsIgnoreCase(vista);
        if ((!reporte.isEmpty() && tipo == null) || !vistaValida) {
            respuesta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorReporte", "Selecciona un reporte y una vista disponibles.");
        }

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        boolean verCapacidad = !bloqueados.contains("capacidad");
        boolean verAsignaciones = !bloqueados.contains("asignaciones");
        boolean verProyectos = !bloqueados.contains("historial");
        Resumen resumen = reportes.consultarResumen(hoy, verCapacidad, verAsignaciones, verProyectos);

        model.addAttribute("titulo", "Reportes");
        model.addAttribute("hoy", hoy);
        model.addAttribute("resumen", resumen);
        model.addAttribute("modo", matriz ? "matriz" : "barras");
        model.addAttribute("verCapacidad", verCapacidad);
        model.addAttribute("verAsignaciones", verAsignaciones);
        model.addAttribute("verProyectos", verProyectos);
        model.addAttribute("reportes", Arrays.stream(Tipo.values()).filter(t -> t.permitido(bloqueados)).toList());
        if (vistaValida && tipo != null && tipo.permitido(bloqueados)) {
            model.addAttribute("reporteActivo", tipo.getClave());
            model.addAttribute("vistaPrevia", tabla(tipo, hoy, gestor, bloqueados, sesion, resumen));
        }
        return "resource/reportes";
    }

    @GetMapping("/exportar")
    public ResponseEntity<String> descargarCsv(@RequestParam(value = "tipo", defaultValue = "") String clave,
                                               @RequestAttribute("usuarioVerificado") Usuario gestor,
                                               @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                                               HttpSession sesion) {
        Tipo tipo = Tipo.buscar(clave);
        if (tipo == null) return ResponseEntity.badRequest().body("Selecciona un tipo de reporte válido.");
        if (!tipo.permitido(bloqueados)) return ResponseEntity.status(403).build();
        TablaReporte tabla = tabla(tipo, LocalDate.now(ZONA_HORARIA), gestor, bloqueados, sesion, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, tipo.adjunto(".csv"))
                .cacheControl(CacheControl.noStore())
                .contentType(CSV)
                .body(exportacion.generarCsv(tabla));
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(@RequestParam(value = "tipo", defaultValue = "") String clave,
                                                  @RequestAttribute("usuarioVerificado") Usuario gestor,
                                                  @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                                                  HttpSession sesion) throws IOException {
        Tipo tipo = Tipo.buscar(clave);
        if (tipo == null) return ResponseEntity.badRequest().build();
        if (!tipo.permitido(bloqueados)) return ResponseEntity.status(403).build();
        TablaReporte tabla = tabla(tipo, LocalDate.now(ZONA_HORARIA), gestor, bloqueados, sesion, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, tipo.adjunto(".xlsx"))
                .cacheControl(CacheControl.noStore())
                .contentType(XLSX)
                .body(exportacion.generarExcel(tabla));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@RequestParam(value = "tipo", defaultValue = "") String clave,
                                                @RequestAttribute("usuarioVerificado") Usuario gestor,
                                                @RequestAttribute("permisosBloqueados") Set<String> bloqueados,
                                                HttpSession sesion) {
        Tipo tipo = Tipo.buscar(clave);
        if (tipo == null) return ResponseEntity.badRequest().build();
        if (!tipo.permitido(bloqueados)) return ResponseEntity.status(403).build();
        TablaReporte tabla = tabla(tipo, LocalDate.now(ZONA_HORARIA), gestor, bloqueados, sesion, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, tipo.adjunto(".pdf"))
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .body(ReportePdf.generar(tabla.nombre(), tabla.columnas(), tabla.filas()));
    }

    private TablaReporte tabla(Tipo tipo, LocalDate hoy, Usuario gestor, Set<String> bloqueados,
                               HttpSession sesion, Resumen resumen) {
        return switch (tipo) {
            case CAPACIDAD -> resumen == null ? reportes.tablaCapacidad(hoy) : reportes.tablaCapacidad(resumen.cargas());
            case ASIGNACIONES -> reportes.tablaAsignaciones();
            case TALENTO -> reportes.tablaTalento(talento.ultimaConsulta(sesion, gestor, !bloqueados.contains("historial")),
                    !bloqueados.contains("capacidad"), hoy);
            case PROYECTOS -> reportes.tablaProyectos();
        };
    }

    public enum Tipo {
        CAPACIDAD("capacidad", "capacidad", "Capacidad y carga",
                "Capacidad base, carga vigente y disponibilidad restante.", "reporte_capacidad"),
        ASIGNACIONES("asignaciones", "asignaciones", "Asignaciones",
                "Colaborador, proyecto, rol, fechas, estado y dedicación.", "reporte_asignaciones_recursos"),
        TALENTO("talento", "talento", "Talent Matching",
                "Candidatos y criterios de tu última consulta.", "reporte_talent_matching"),
        PROYECTOS("proyectos", "historial", "Proyectos",
                "Estado, prioridad, avance y período de cada proyecto.", "reporte_proyectos_recursos");

        private final String clave;
        private final String permiso;
        private final String titulo;
        private final String descripcion;
        private final String archivo;

        Tipo(String clave, String permiso, String titulo, String descripcion, String archivo) {
            this.clave = clave;
            this.permiso = permiso;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.archivo = archivo;
        }

        static Tipo buscar(String clave) {
            String normalizada = clave.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(t -> t.clave.equals(normalizada)).findFirst().orElse(null);
        }

        boolean permitido(Set<String> bloqueados) {
            return !bloqueados.contains(permiso);
        }

        String adjunto(String extension) {
            return "attachment; filename=\"" + archivo + extension + "\"";
        }

        public String getClave() { return clave; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
    }
}
