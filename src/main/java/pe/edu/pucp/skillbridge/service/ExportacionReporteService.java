package pe.edu.pucp.skillbridge.service;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.pucp.skillbridge.dto.TablaReporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportacionReporteService {
    public byte[] generarExcel(TablaReporte reporte) throws IOException {
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
            var formatos = libro.createDataFormat();
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(formatos.getFormat("dd/mm/yyyy"));
            CellStyle fechaHora = libro.createCellStyle();
            fechaHora.setDataFormat(formatos.getFormat("dd/mm/yyyy hh:mm"));
            CellStyle porcentaje = libro.createCellStyle();
            porcentaje.setDataFormat(formatos.getFormat("0%"));

            for (int indice = 0; indice < reporte.filas().size(); indice++) {
                var fila = hoja.createRow(indice + 1);
                List<Object> valores = reporte.filas().get(indice);
                int lineas = 1;
                for (int columna = 0; columna < valores.size(); columna++) {
                    Object valor = valores.get(columna);
                    int longitud = escribirCelda(fila.createCell(columna), valor, texto, fecha, fechaHora, porcentaje);
                    anchos[columna] = Math.max(anchos[columna], longitud + 3);
                    if (valor instanceof String contenido && contenido.contains("\n")) {
                        lineas = Math.max(lineas, (int) contenido.lines().mapToLong(
                                linea -> Math.max(1, (linea.length() + 49) / 50)).sum());
                    }
                }
                if (lineas > 1) {
                    fila.setHeightInPoints(Math.min(400, hoja.getDefaultRowHeightInPoints() * lineas));
                }
            }

            if (reporte.filas().isEmpty()) hoja.createRow(1);
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

    private int escribirCelda(Cell celda, Object valor, CellStyle texto, CellStyle fecha,
                              CellStyle fechaHora, CellStyle porcentaje) {
        if (valor instanceof LocalDate dia) {
            celda.setCellValue(dia);
            celda.setCellStyle(fecha);
            return 10;
        }
        if (valor instanceof LocalDateTime instante) {
            celda.setCellValue(instante);
            celda.setCellStyle(fechaHora);
            return 16;
        }
        if (valor instanceof Double fraccion) {
            celda.setCellValue(fraccion);
            celda.setCellStyle(porcentaje);
            return 5;
        }
        String contenido = valor == null ? "" : valor.toString();
        celda.setCellValue(contenido);
        celda.setCellStyle(texto);
        return contenido.lines().mapToInt(String::length).max().orElse(0);
    }

    public String generarCsv(TablaReporte reporte) {
        StringBuilder salida = new StringBuilder("\uFEFF");
        salida.append(String.join(",", reporte.columnas())).append('\n');
        for (List<Object> fila : reporte.filas()) {
            salida.append(fila.stream().map(this::celdaCsv).collect(Collectors.joining(","))).append('\n');
        }
        return salida.toString();
    }

    private String celdaCsv(Object valor) {
        if (valor instanceof Double fraccion) {
            valor = BigDecimal.valueOf(fraccion).movePointRight(2).stripTrailingZeros().toPlainString();
        }
        String texto = valor == null ? "" : valor.toString();
        String inicio = texto.stripLeading();
        if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0) texto = "'" + texto;
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
