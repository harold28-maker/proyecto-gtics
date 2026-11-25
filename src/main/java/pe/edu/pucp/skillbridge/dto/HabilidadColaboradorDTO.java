package pe.edu.pucp.skillbridge.dto;

import java.math.BigDecimal;

/**
 * Representa una habilidad registrada para un colaborador.
 */
public class HabilidadColaboradorDTO {

    public enum Nivel {
        BASICO("Básico"),
        INTERMEDIO("Intermedio"),
        AVANZADO("Avanzado"),
        EXPERTO("Experto");

        private final String etiqueta;

        Nivel(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() { return etiqueta; }
    }

    private Integer idHabilidad;
    private String nombre;
    private String categoria;
    private String nivel;
    private BigDecimal aniosExperiencia;

    /**
     * Crea un DTO con los datos de una habilidad del colaborador.
     *
     * @param idHabilidad identificador de la habilidad
     * @param nombre nombre de la habilidad
     * @param categoria categoría a la que pertenece la habilidad
     * @param nivel nivel asignado al colaborador en la habilidad
     * @param aniosExperiencia años de experiencia del colaborador en la habilidad
     */
    public HabilidadColaboradorDTO(Integer idHabilidad, String nombre, String categoria,
                                   String nivel, BigDecimal aniosExperiencia) {
        this.idHabilidad = idHabilidad;
        this.nombre = nombre;
        this.categoria = categoria;
        this.nivel = nivel;
        this.aniosExperiencia = aniosExperiencia;
    }

    /**
     * Obtiene el identificador de la habilidad.
     *
     * @return identificador de la habilidad
     */
    public Integer getIdHabilidad() { return idHabilidad; }

    /**
     * Obtiene el nombre de la habilidad.
     *
     * @return nombre de la habilidad
     */
    public String getNombre() { return nombre; }

    /**
     * Obtiene la categoría de la habilidad.
     *
     * @return categoría de la habilidad
     */
    public String getCategoria() { return categoria; }

    /**
     * Obtiene el nivel asignado a la habilidad.
     *
     * @return nivel del colaborador en la habilidad
     */
    public String getNivel() { return nivel; }

    /**
     * Obtiene los años de experiencia del colaborador en la habilidad.
     *
     * @return años de experiencia
     */
    public BigDecimal getAniosExperiencia() { return aniosExperiencia; }

    public static HabilidadColaboradorDTO desdeFila(Object[] fila) {
        return new HabilidadColaboradorDTO(((Number) fila[0]).intValue(),
                String.valueOf(fila[1]), String.valueOf(fila[2]), String.valueOf(fila[3]),
                fila[4] == null ? BigDecimal.ZERO : new BigDecimal(fila[4].toString()));
    }

    public Nivel getNivelHabilidad() {
        for (Nivel opcion : Nivel.values()) {
            if (opcion.name().equals(nivel)) return opcion;
        }
        return null;
    }

    public String getAniosExperienciaLocal() {
        return aniosExperiencia == null ? "" : aniosExperiencia.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
