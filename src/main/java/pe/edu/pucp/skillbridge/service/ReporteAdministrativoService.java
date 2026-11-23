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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class ReporteAdministrativoService {

    private static final Comparator<Eje> POR_NOMBRE = Comparator
            .comparing(Eje::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Eje::id);
    private static final List<String> COLORES_PROYECTOS = List.of("#8350d2", "#b57a32", "#4b79ab", "#327e67");
    private static final List<String> COLORES_REPARTO = List.of("#327e67", "#65a992", "#a9d7c7");

    private final CapacidadColaboradorService capacidadColaboradorService;

    public ReporteAdministrativoService(CapacidadColaboradorService capacidadColaboradorService) {
        this.capacidadColaboradorService = capacidadColaboradorService;
    }

    public record Tabla(String nombre, List<String> columnas, List<List<Object>> filas) {}

    public record Eje(Integer id, String nombre) {}

    public record DedicacionProyecto(Integer id, String nombre, Map<Integer, Integer> porcentajes, String color) {}

    public record GraficoDedicacion(List<Eje> colaboradores, List<DedicacionProyecto> proyectos) {}

    public record MarcaDedicacion(int valor, double posicion) {}

    public record BarraDedicacion(String proyecto, int valor, double altura, String color) {}

    public record GrupoDedicacion(String colaborador, List<BarraDedicacion> barras) {}

    public record PorcionDedicacion(String colaborador, int dedicacion, double participacion, String color) {}

    public record RepartoDedicacion(DedicacionProyecto proyecto, List<PorcionDedicacion> porciones, String gradiente) {}

    public record VisualizacionDedicacion(int limite, List<MarcaDedicacion> marcas, List<GrupoDedicacion> grupos,
                                         int anchoGrupo, RepartoDedicacion reparto) {}

    public GraficoDedicacion consultarDedicacion(boolean vigenteHoy) {
        List<Asignacion> asignaciones = vigenteHoy ? capacidadColaboradorService.vigentesHoy()
                : capacidadColaboradorService.activasYPlanificadas();
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

        List<DedicacionProyecto> proyectos = new ArrayList<>();
        for (Eje proyecto : porcentajesPorProyecto.keySet().stream().sorted(POR_NOMBRE).toList()) {
            proyectos.add(new DedicacionProyecto(proyecto.id(), proyecto.nombre(),
                    porcentajesPorProyecto.get(proyecto), colorProyecto(proyectos.size())));
        }
        return new GraficoDedicacion(colaboradores.values().stream().sorted(POR_NOMBRE).toList(), proyectos);
    }

    /**
     * Prepara una escala compartida para las barras y la matriz, y el reparto del proyecto seleccionado.
     */
    public VisualizacionDedicacion visualizarDedicacion(GraficoDedicacion grafico, Integer idProyecto) {
        int maximo = grafico.proyectos().stream()
                .flatMap(proyecto -> proyecto.porcentajes().values().stream())
                .mapToInt(Integer::intValue).max().orElse(0);
        int paso = 25 * (int) Math.ceil(Math.max(100, maximo) / 100.0);
        int limite = paso * (int) Math.ceil(Math.max(100, maximo) / (double) paso);
        List<MarcaDedicacion> marcas = IntStream.rangeClosed(0, limite / paso)
                .mapToObj(i -> new MarcaDedicacion(limite - i * paso, 100.0 * i * paso / limite))
                .toList();

        List<GrupoDedicacion> grupos = new ArrayList<>();
        int anchoGrupo = 108;
        for (Eje colaborador : grafico.colaboradores()) {
            List<BarraDedicacion> barras = new ArrayList<>();
            for (DedicacionProyecto proyecto : grafico.proyectos()) {
                Integer valor = proyecto.porcentajes().get(colaborador.id());
                if (valor != null) {
                    barras.add(new BarraDedicacion(proyecto.nombre(), valor, 100.0 * valor / limite, proyecto.color()));
                }
            }
            grupos.add(new GrupoDedicacion(colaborador.nombre(), barras));
            anchoGrupo = Math.max(anchoGrupo, barras.size() * 58 + 16);
        }

        DedicacionProyecto seleccionado = grafico.proyectos().stream()
                .filter(proyecto -> proyecto.id().equals(idProyecto))
                .findFirst().orElseGet(() -> grafico.proyectos().stream()
                        .max(Comparator.comparingInt(proyecto -> proyecto.porcentajes().size()))
                        .orElse(null));
        return new VisualizacionDedicacion(limite, marcas, grupos, anchoGrupo,
                calcularReparto(seleccionado, grafico.colaboradores()));
    }

    private RepartoDedicacion calcularReparto(DedicacionProyecto proyecto, List<Eje> colaboradores) {
        if (proyecto == null) {
            return new RepartoDedicacion(null, List.of(), "");
        }

        Map<Integer, Integer> dedicaciones = proyecto.porcentajes();
        long total = dedicaciones.values().stream().mapToLong(Integer::longValue).sum();
        List<Eje> equipo = colaboradores.stream()
                .filter(colaborador -> dedicaciones.containsKey(colaborador.id()))
                .sorted(Comparator.comparingInt((Eje colaborador) -> dedicaciones.get(colaborador.id())).reversed())
                .toList();

        List<PorcionDedicacion> porciones = new ArrayList<>();
        List<String> segmentos = new ArrayList<>();
        long acumulado = 0;
        long corteAnterior = 0;
        for (Eje colaborador : equipo) {
            int dedicacion = dedicaciones.get(colaborador.id());
            acumulado += dedicacion;
            // Redondear los límites acumulados permite que las porciones visibles sumen 100 %.
            long corte = Math.round(10000.0 * acumulado / total);
            int indice = porciones.size();
            String color = indice < COLORES_REPARTO.size() ? COLORES_REPARTO.get(indice) : colorProyecto(indice + 1);
            porciones.add(new PorcionDedicacion(colaborador.nombre(), dedicacion, (corte - corteAnterior) / 100.0, color));
            segmentos.add(color + " " + corteAnterior / 100.0 + "% " + corte / 100.0 + "%");
            corteAnterior = corte;
        }
        return new RepartoDedicacion(proyecto, porciones, "conic-gradient(" + String.join(", ", segmentos) + ")");
    }

    private String colorProyecto(int indice) {
        return indice < COLORES_PROYECTOS.size() ? COLORES_PROYECTOS.get(indice)
                : "hsl(" + (indice * 137.5 % 360) + " 42% 44%)";
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
