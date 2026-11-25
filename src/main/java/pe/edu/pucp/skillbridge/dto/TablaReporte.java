package pe.edu.pucp.skillbridge.dto;

import java.util.List;

public record TablaReporte(String nombre, List<String> columnas, List<List<Object>> filas) {}
