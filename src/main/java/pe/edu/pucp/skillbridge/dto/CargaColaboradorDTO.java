package pe.edu.pucp.skillbridge.dto;

/**
 * Representa la carga laboral y la disponibilidad de un colaborador.
 */
public class CargaColaboradorDTO {
    private Integer idColaborador;
    private String colaborador;
    private String cargo;
    private String area;
    private Integer disponibilidadBase;
    private Integer carga;
    private Integer disponibilidad;

    /**
     * Crea un DTO con los datos de carga y disponibilidad del colaborador.
     *
     * @param idColaborador identificador del colaborador
     * @param colaborador nombre del colaborador
     * @param cargo cargo desempeñado por el colaborador
     * @param area área a la que pertenece el colaborador
     * @param disponibilidadBase disponibilidad inicial expresada como capacidad disponible
     * @param carga carga asignada al colaborador
     * @param disponibilidad disponibilidad restante del colaborador
     */
    public CargaColaboradorDTO(Integer idColaborador, String colaborador, String cargo, String area,
                               Integer disponibilidadBase, Integer carga, Integer disponibilidad) {
        this.idColaborador = idColaborador;
        this.colaborador = colaborador;
        this.cargo = cargo;
        this.area = area;
        this.disponibilidadBase = disponibilidadBase;
        this.carga = carga;
        this.disponibilidad = disponibilidad;
    }

    /**
     * Obtiene el identificador del colaborador.
     *
     * @return identificador del colaborador
     */
    public Integer getIdColaborador() { return idColaborador; }

    /**
     * Obtiene el nombre del colaborador.
     *
     * @return nombre del colaborador
     */
    public String getColaborador() { return colaborador; }

    /**
     * Obtiene el cargo del colaborador.
     *
     * @return cargo desempeñado por el colaborador
     */
    public String getCargo() { return cargo; }

    /**
     * Obtiene el área del colaborador.
     *
     * @return área a la que pertenece el colaborador
     */
    public String getArea() { return area; }

    /**
     * Obtiene la disponibilidad base del colaborador.
     *
     * @return disponibilidad inicial expresada como capacidad disponible
     */
    public Integer getDisponibilidadBase() { return disponibilidadBase; }

    /**
     * Obtiene la carga asignada al colaborador.
     *
     * @return carga actual del colaborador
     */
    public Integer getCarga() { return carga; }

    /**
     * Obtiene la disponibilidad restante del colaborador.
     *
     * @return disponibilidad restante del colaborador
     */
    public Integer getDisponibilidad() { return disponibilidad; }
}
