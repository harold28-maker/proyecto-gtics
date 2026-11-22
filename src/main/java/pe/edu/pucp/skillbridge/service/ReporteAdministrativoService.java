package pe.edu.pucp.skillbridge.service;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.entity.Habilidad;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteAdministrativoService {

    private static final Comparator<Eje> POR_NOMBRE = Comparator
            .comparing(Eje::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Eje::id);

    private final AsignacionRepository asignacionRepository;

    public ReporteAdministrativoService(AsignacionRepository asignacionRepository) {
        this.asignacionRepository = asignacionRepository;
    }

    public record Tabla(String nombre, List<String> columnas, List<List<Object>> filas) {}

    public record Eje(Integer id, String nombre) {}

    public record DedicacionProyecto(String nombre, Map<Integer, Integer> porcentajes) {}

    public record GraficoDedicacion(List<Eje> colaboradores, List<DedicacionProyecto> proyectos) {}

    public GraficoDedicacion consultarDedicacion() {
        List<Asignacion> asignaciones = asignacionRepository.findByEstadoIn(List.of(
                Asignacion.EstadoAsignacion.ACTIVA, Asignacion.EstadoAsignacion.PLANIFICADA));
        Map<Eje, Map<Integer, Integer>> porcentajesPorProyecto = new LinkedHashMap<>();
        Map<Integer, Eje> colaboradores = new LinkedHashMap<>();

        for (Asignacion asignacion : asignaciones) {
            var proyecto = asignacion.getProyecto();
            var colaborador = asignacion.getColaborador();
            Integer idColaborador = colaborador.getIdColaborador();
            var ejeProyecto = new Eje(proyecto.getIdProyecto(), proyecto.getNombre());

            colaboradores.putIfAbsent(idColaborador,
                    new Eje(idColaborador, colaborador.getUsuario().getNombreCompleto()));
            porcentajesPorProyecto.computeIfAbsent(ejeProyecto, clave -> new LinkedHashMap<>())
                    .merge(idColaborador, asignacion.getPorcentajeDedicacion(), Integer::sum);
        }

        List<DedicacionProyecto> proyectos = porcentajesPorProyecto.keySet().stream()
                .sorted(POR_NOMBRE)
                .map(proyecto -> new DedicacionProyecto(proyecto.nombre(), porcentajesPorProyecto.get(proyecto)))
                .toList();
        return new GraficoDedicacion(colaboradores.values().stream().sorted(POR_NOMBRE).toList(), proyectos);
    }

    public Tabla tablaDedicacion(GraficoDedicacion grafico) {
        List<List<Object>> filas = new ArrayList<>();
        for (DedicacionProyecto proyecto : grafico.proyectos()) {
            for (Eje colaborador : grafico.colaboradores()) {
                Integer porcentaje = proyecto.porcentajes().get(colaborador.id());
                if (porcentaje != null) {
                    filas.add(List.of(colaborador.nombre(), proyecto.nombre(), porcentaje / 100.0));
                }
            }
        }
        return new Tabla("Dedicación por proyecto", List.of("Colaborador", "Proyecto", "Dedicación"), filas);
    }

    public Tabla tablaUsuarios(List<Usuario> usuarios) {
        List<List<Object>> filas = usuarios.stream()
                .map(u -> List.<Object>of(u.getNombreCompleto(), u.getCorreo(), u.getRolVisual(),
                        texto(u.getEstado()), texto(u.getTelefono())))
                .toList();
        return new Tabla("Usuarios y accesos", List.of("Nombre", "Correo", "Rol", "Estado", "Teléfono"), filas);
    }

    public Tabla tablaHabilidades(List<Habilidad> habilidades) {
        List<List<Object>> filas = habilidades.stream()
                .map(h -> List.<Object>of(h.getNombre(), texto(h.getCategoria()),
                        Boolean.TRUE.equals(h.getEstado()) ? "ACTIVA" : "INACTIVA", texto(h.getDescripcion())))
                .toList();
        return new Tabla("Catálogo de habilidades", List.of("Habilidad", "Categoría", "Estado", "Descripción"), filas);
    }

    public Tabla tablaAuditoria(List<Auditoria> auditorias) {
        List<List<Object>> filas = auditorias.stream()
                .map(a -> List.<Object>of(a.getFecha() == null ? "-" : a.getFecha(),
                        a.getUsuario() == null ? "Sistema" : a.getUsuario().getNombreCompleto(),
                        texto(a.getAccion()), texto(a.getModulo()), texto(a.getDetalle())))
                .toList();
        return new Tabla("Auditoría reciente", List.of("Fecha", "Usuario", "Acción", "Módulo", "Detalle"), filas);
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "-" : valor.toString();
    }

    public byte[] generarExcel(Tabla reporte) throws IOException {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet(reporte.nombre());
            int[] anchos = new int[reporte.columnas().size()];
            var encabezado = hoja.createRow(0);
            for (int columna = 0; columna < anchos.length; columna++) {
                String titulo = reporte.columnas().get(columna);
                encabezado.createCell(columna).setCellValue(titulo);
                anchos[columna] = titulo.length() + 6;
            }

            CellStyle texto = libro.createCellStyle();
            texto.setWrapText(true);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            CellStyle porcentaje = libro.createCellStyle();
            porcentaje.setDataFormat(libro.createDataFormat().getFormat("0%"));

            for (int indice = 0; indice < reporte.filas().size(); indice++) {
                var fila = hoja.createRow(indice + 1);
                List<Object> valores = reporte.filas().get(indice);
                for (int columna = 0; columna < valores.size(); columna++) {
                    Object valor = valores.get(columna);
                    int longitud = escribirCelda(fila.createCell(columna), valor, texto, fecha, porcentaje);
                    anchos[columna] = Math.max(anchos[columna], longitud + 3);
                }
            }

            if (reporte.filas().isEmpty()) {
                hoja.createRow(1);
            }
            var rango = new AreaReference(new CellReference(0, 0),
                    new CellReference(hoja.getLastRowNum(), anchos.length - 1), SpreadsheetVersion.EXCEL2007);
            var tabla = hoja.createTable(rango);
            tabla.setName("DatosReporte");
            tabla.setDisplayName("DatosReporte");
            tabla.setStyleName("TableStyleMedium2");
            tabla.getCTTable().getTableStyleInfo().setShowRowStripes(true);
            tabla.getCTTable().addNewAutoFilter().setRef(rango.formatAsString());

            for (int columna = 0; columna < anchos.length; columna++) {
                hoja.setColumnWidth(columna, Math.min(60, Math.max(13, anchos[columna])) * 256);
            }
            hoja.createFreezePane(0, 1);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private int escribirCelda(Cell celda, Object valor, CellStyle texto, CellStyle fecha, CellStyle porcentaje) {
        if (valor instanceof LocalDateTime instante) {
            celda.setCellValue(instante);
            celda.setCellStyle(fecha);
            return 16;
        }
        if (valor instanceof Double fraccion) {
            celda.setCellValue(fraccion);
            celda.setCellStyle(porcentaje);
            return 5;
        }
        String contenido = valor.toString();
        celda.setCellValue(contenido);
        celda.setCellStyle(texto);
        return contenido.length();
    }
}
