package pe.edu.pucp.skillbridge.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Genera reportes PDF A4 horizontal con Java puro (sin servicios externos).
 * Se reutilizan los datos del reporte existente, por lo que no cambia la fuente
 * de datos ni los controles de acceso de cada rol.
 */
public final class ReportePdf {
    private static final double ANCHO = 842;
    private static final double ALTO = 595;
    private static final double MARGEN = 34;
    private static final double ANCHO_TABLA = ANCHO - 2 * MARGEN;
    private static final Charset ANSI = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"));

    private ReportePdf() { }

    /** Genera directamente el PDF de una tabla, incluyendo encabezados, paginación y filas. */
    public static byte[] generar(String titulo, List<String> columnas, List<? extends List<?>> filas) {
        if (columnas == null || columnas.isEmpty()) {
            throw new IllegalArgumentException("El reporte debe tener al menos una columna");
        }
        int cantidad = columnas.size();
        double anchoCelda = ANCHO_TABLA / cantidad;
        double fuente = cantidad >= 8 ? 7.0 : (cantidad >= 6 ? 7.7 : 8.6);
        List<StringBuilder> paginas = new ArrayList<>();
        StringBuilder pagina = nuevaPagina(titulo, columnas, anchoCelda, fuente);
        paginas.add(pagina);
        double y = 474;
        int filaNumero = 0;
        for (List<?> fila : filas) {
            List<List<String>> celdas = new ArrayList<>();
            int maxLineas = 1;
            for (int c = 0; c < cantidad; c++) {
                Object valor = fila != null && c < fila.size() ? fila.get(c) : "";
                // No truncar datos: una celda larga puede ocupar más de una página.
                List<String> lineas = dividir(formatear(valor), anchoCelda - 12, fuente, Integer.MAX_VALUE);
                celdas.add(lineas);
                maxLineas = Math.max(maxLineas, lineas.size());
            }
            int primeraLinea = 0;
            while (primeraLinea < maxLineas) {
                double interlineado = fuente + 3;
                int posibles = (int) Math.floor((y - 46 - 10) / interlineado);
                if (posibles < 1) {
                    pagina = nuevaPagina(titulo, columnas, anchoCelda, fuente);
                    paginas.add(pagina);
                    y = 474;
                    continue;
                }
                int tomar = Math.min(maxLineas - primeraLinea, posibles);
                double altoFila = Math.max(23, 10 + tomar * interlineado);
                if (filaNumero % 2 == 0) {
                    rect(pagina, MARGEN, y - altoFila, ANCHO_TABLA, altoFila, 0.975, 0.971, 0.985);
                }
                linea(pagina, MARGEN, y - altoFila, MARGEN + ANCHO_TABLA, y - altoFila, .86, .85, .89);
                for (int c = 0; c < cantidad; c++) {
                    List<String> lineas = celdas.get(c);
                    for (int numero = 0; numero < tomar && primeraLinea + numero < lineas.size(); numero++) {
                        texto(pagina, MARGEN + c * anchoCelda + 5,
                                y - 11 - numero * interlineado,
                                lineas.get(primeraLinea + numero), fuente, false, .16, .15, .23);
                    }
                }
                y -= altoFila;
                primeraLinea += tomar;
                if (primeraLinea < maxLineas) {
                    pagina = nuevaPagina(titulo, columnas, anchoCelda, fuente);
                    paginas.add(pagina);
                    y = 474;
                }
            }
            filaNumero++;
        }
        if (filas.isEmpty()) {
            texto(pagina, MARGEN + 8, 450, "No hay registros para este reporte.", 10, false, .45, .43, .49);
        }
        for (int i = 0; i < paginas.size(); i++) {
            texto(paginas.get(i), MARGEN, 24,
                    "SkillBridge AI  |  Reporte generado desde la plataforma", 8, false, .4, .39, .46);
            texto(paginas.get(i), ANCHO - 105, 24,
                    "Pagina " + (i + 1) + " de " + paginas.size(), 8, false, .4, .39, .46);
        }
        return ensamblarPdf(paginas);
    }

    /** Reutiliza el contenido CSV generado por el controlador del rol para producir el PDF. */
    public static byte[] desdeCsv(String titulo, String csv) {
        List<List<String>> datos = leerCsv(csv == null ? "" : csv);
        if (datos.isEmpty()) throw new IllegalArgumentException("El CSV no tiene encabezados");
        List<String> columnas = datos.get(0);
        List<List<String>> filas = datos.subList(1, datos.size());
        return generar(titulo, columnas, filas);
    }

    /** Soporta coma, campos entre comillas, comillas escapadas, saltos de línea y BOM. */
    static List<List<String>> leerCsv(String csv) {
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (i == 0 && ch == '\uFEFF') continue;
            if (ch == '"') {
                if (enComillas && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    celda.append('"'); i++;
                } else { enComillas = !enComillas; }
            } else if (ch == ',' && !enComillas) {
                fila.add(celda.toString()); celda.setLength(0);
            } else if ((ch == '\n' || ch == '\r') && !enComillas) {
                if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                fila.add(celda.toString()); celda.setLength(0);
                if (fila.size() > 1 || !fila.get(0).isEmpty()) filas.add(fila);
                fila = new ArrayList<>();
            } else { celda.append(ch); }
        }
        if (!fila.isEmpty() || celda.length() > 0) {
            fila.add(celda.toString()); filas.add(fila);
        }
        return filas;
    }

    private static String formatear(Object valor) {
        if (valor == null) return "-";
        if (valor instanceof LocalDateTime fecha) return FECHA.format(fecha);
        if (valor instanceof Double decimal && decimal >= 0 && decimal <= 1) {
            return String.format(Locale.ROOT, "%.0f%%", decimal * 100);
        }
        return valor.toString().replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').strip();
    }

    private static List<String> dividir(String valor, double ancho, double tam, int maxLineas) {
        // Aproximación conservadora de caracteres para Helvetica: evita sobrepasar la columna.
        int limite = Math.max(7, (int) (ancho / (tam * .54)));
        List<String> lineas = new ArrayList<>();
        String texto = valor == null ? "" : valor.replaceAll("\\s+", " ").strip();
        while (texto.length() > limite && lineas.size() < maxLineas - 1) {
            int corte = texto.lastIndexOf(' ', limite);
            if (corte < limite / 2) corte = limite;
            lineas.add(texto.substring(0, corte).strip());
            texto = texto.substring(corte).strip();
        }
        if (texto.length() > limite) texto = texto.substring(0, Math.max(1, limite - 3)) + "...";
        lineas.add(texto);
        return lineas;
    }

    private static StringBuilder nuevaPagina(String titulo, List<String> columnas, double anchoCelda, double tam) {
        StringBuilder contenido = new StringBuilder(4096);
        // Fondo, encabezado, separador y titulos en la paleta sobria del panel Admin.
        rect(contenido, 0, ALTO - 8, ANCHO, 8, .31, .12, .75);
        texto(contenido, MARGEN, 553, "SkillBridge AI", 11, true, .31, .12, .75);
        texto(contenido, MARGEN, 528, titulo, 17, true, .09, .08, .17);
        texto(contenido, MARGEN, 509, "Exportacion de datos - " + FECHA.format(LocalDateTime.now()), 8, false, .45, .43, .49);
        rect(contenido, MARGEN, 482, ANCHO_TABLA, 26, .19, .16, .29);
        for (int i = 0; i < columnas.size(); i++) {
            String cabecera = dividir(columnas.get(i), anchoCelda - 10, tam, 1).get(0);
            texto(contenido, MARGEN + i * anchoCelda + 5, 491, cabecera, tam, true, 1, 1, 1);
        }
        return contenido;
    }

    private static void rect(StringBuilder contenido, double x, double y, double w, double h,
                             double r, double g, double b) {
        contenido.append(n(r)).append(' ').append(n(g)).append(' ').append(n(b)).append(" rg ")
                .append(n(x)).append(' ').append(n(y)).append(' ').append(n(w)).append(' ')
                .append(n(h)).append(" re f\n");
    }

    private static void linea(StringBuilder contenido, double x1, double y1, double x2, double y2,
                              double r, double g, double b) {
        contenido.append(n(r)).append(' ').append(n(g)).append(' ').append(n(b)).append(" RG .4 w ")
                .append(n(x1)).append(' ').append(n(y1)).append(" m ")
                .append(n(x2)).append(' ').append(n(y2)).append(" l S\n");
    }

    private static String n(double numero) { return String.format(Locale.ROOT, "%.2f", numero); }

    private static void texto(StringBuilder contenido, double x, double y, String texto,
                              double size, boolean negrita, double r, double g, double b) {
        contenido.append(n(r)).append(' ').append(n(g)).append(' ').append(n(b))
                .append(" rg BT /F").append(negrita ? '2' : '1').append(' ').append(n(size))
                .append(" Tf 1 0 0 1 ").append(n(x)).append(' ').append(n(y))
                .append(" Tm (").append(escapar(texto)).append(") Tj ET\n");
    }

    private static String escapar(String entrada) {
        // Helvetica WinAnsi admite caracteres españoles; controla caracteres externos.
        byte[] bytes = (entrada == null ? "" : entrada).getBytes(ANSI);
        String latin = new String(bytes, StandardCharsets.ISO_8859_1);
        return latin.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace('\n', ' ').replace('\r', ' ');
    }

    private static byte[] ensamblarPdf(List<StringBuilder> paginas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        escribir(salida, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        offsets.add(0); // objeto cero, libre
        escribirObjeto(salida, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) hijos.append(5 + 2 * i).append(" 0 R ");
        escribirObjeto(salida, offsets, 2,
                "<< /Type /Pages /Count " + paginas.size() + " /Kids [ " + hijos + "] >>");
        escribirObjeto(salida, offsets, 3,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        escribirObjeto(salida, offsets, 4,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < paginas.size(); i++) {
            int pag = 5 + i * 2;
            escribirObjeto(salida, offsets, pag,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (pag + 1) + " 0 R >>");
            byte[] cuerpo = paginas.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            escribirObjeto(salida, offsets, pag + 1,
                    "<< /Length " + cuerpo.length + " >>\nstream\n"
                    + paginas.get(i) + "endstream");
        }
        int inicioXref = salida.size();
        escribir(salida, "xref\n0 " + offsets.size() + "\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            escribir(salida, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
        }
        escribir(salida, "trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n"
                + inicioXref + "\n%%EOF\n");
        return salida.toByteArray();
    }

    private static void escribirObjeto(ByteArrayOutputStream out, List<Integer> offsets, int numero, String contenido) {
        if (offsets.size() != numero) throw new IllegalStateException("Orden de objetos PDF incorrecto");
        offsets.add(out.size());
        escribir(out, numero + " 0 obj\n" + contenido + "\nendobj\n");
    }

    private static void escribir(ByteArrayOutputStream out, String contenido) {
        byte[] bytes = contenido.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }
}
