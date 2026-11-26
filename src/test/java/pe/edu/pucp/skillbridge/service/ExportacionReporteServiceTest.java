package pe.edu.pucp.skillbridge.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.skillbridge.dto.TablaReporte;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportacionReporteServiceTest {
    private final ExportacionReporteService exportacion = new ExportacionReporteService();

    @Test
    void exportaLasMismasColumnasConFechasYPorcentajesNativosEnExcel() throws IOException {
        LocalDate inicio = LocalDate.of(2026, 11, 25);
        var reporte = new TablaReporte("Asignaciones", List.of("Colaborador", "Dedicación (%)", "Inicio"),
                List.of(List.of("María González", 0.4, inicio)));

        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(exportacion.generarExcel(reporte)))) {
            var hoja = libro.getSheet("Asignaciones");
            assertEquals("Dedicación (%)", hoja.getRow(0).getCell(1).getStringCellValue());
            assertEquals("María González", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, hoja.getRow(1).getCell(1).getCellType());
            assertEquals(0.4, hoja.getRow(1).getCell(1).getNumericCellValue());
            assertEquals("0%", hoja.getRow(1).getCell(1).getCellStyle().getDataFormatString());
            assertTrue(DateUtil.isCellDateFormatted(hoja.getRow(1).getCell(2)));
            assertEquals(inicio, hoja.getRow(1).getCell(2).getLocalDateTimeCellValue().toLocalDate());
        }
        assertEquals("\uFEFFColaborador,Dedicación (%),Inicio\n\"María González\",\"40\",\"2026-11-25\"\n",
                exportacion.generarCsv(reporte));
    }

    @Test
    void conservaLosEncabezadosEnUnaDescargaSinCandidatos() throws IOException {
        var reporte = new TablaReporte("Talent Matching", List.of("Proyecto", "Colaborador"), List.of());
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(exportacion.generarExcel(reporte)))) {
            var hoja = libro.getSheet("Talent Matching");
            assertEquals("Proyecto", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Colaborador", hoja.getRow(0).getCell(1).getStringCellValue());
            assertNotNull(hoja.getTables().get(0));
        }
        assertEquals("\uFEFFProyecto,Colaborador\n", exportacion.generarCsv(reporte));
    }
}
