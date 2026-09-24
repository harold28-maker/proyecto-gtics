package pe.edu.pucp.skillbridge.dto;

public class CargaColaboradorDTO {
    private Integer idColaborador;
    private String colaborador;
    private String cargo;
    private String area;
    private Integer disponibilidadBase;
    private Integer carga;
    private Integer disponibilidad;

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

    public Integer getIdColaborador() { return idColaborador; }
    public String getColaborador() { return colaborador; }
    public String getCargo() { return cargo; }
    public String getArea() { return area; }
    public Integer getDisponibilidadBase() { return disponibilidadBase; }
    public Integer getCarga() { return carga; }
    public Integer getDisponibilidad() { return disponibilidad; }
}
