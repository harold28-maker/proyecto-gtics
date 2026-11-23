package pe.edu.pucp.skillbridge.util;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test de exportación local, independiente de la base de datos y servicios externos. */
class ReportePdfTest {
    @Test
    void generaPdfConVariasPaginasYCaracteresEspanoles() {
        StringBuilder csv = new StringBuilder("Nombre,Detalle\n");
        for (int i = 0; i < 110; i++) {
            csv.append("\"María, Ñuñez\",\"Proyecto ").append(i).append("\"\n");
        }
        byte[] bytes = ReportePdf.desdeCsv("Colaboradores", csv.toString());
        String texto = new String(bytes, StandardCharsets.ISO_8859_1);
        assertTrue(texto.startsWith("%PDF-1.4"));
        assertTrue(texto.contains("/Type /Page"));
        assertTrue(bytes.length > 1000);
    }

    @Test
    void admiteTablaSinRegistros() {
        byte[] bytes = ReportePdf.generar("Usuarios", List.of("Nombre", "Rol"), List.of());
        assertTrue(new String(bytes, StandardCharsets.ISO_8859_1).contains("No hay registros"));
    }
}
