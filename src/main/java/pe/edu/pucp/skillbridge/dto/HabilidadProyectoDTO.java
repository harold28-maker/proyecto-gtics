package pe.edu.pucp.skillbridge.dto;

/**
 * Representa una habilidad requerida por un proyecto.
 */
public class HabilidadProyectoDTO {
    private Integer idHabilidad;
    private String nombre;
    private String categoria;
    private String nivel;
    private Integer vacantes;

    /**
     * Crea un DTO con los datos de una habilidad requerida por el proyecto.
     *
     * @param idHabilidad identificador de la habilidad
     * @param nombre nombre de la habilidad
     * @param categoria categoría a la que pertenece la habilidad
     * @param nivel nivel requerido para la habilidad
     * @param vacantes cantidad de vacantes asociadas a la habilidad
     */
    public HabilidadProyectoDTO(Integer idHabilidad, String nombre, String categoria, String nivel, Integer vacantes) {
        this.idHabilidad = idHabilidad;
        this.nombre = nombre;
        this.categoria = categoria;
        this.nivel = nivel;
        this.vacantes = vacantes;
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
     * Obtiene el nivel requerido para la habilidad.
     *
     * @return nivel requerido
     */
    public String getNivel() { return nivel; }

    /**
     * Obtiene la cantidad de vacantes asociadas a la habilidad.
     *
     * @return cantidad de vacantes
     */
    public Integer getVacantes() { return vacantes; }
}
