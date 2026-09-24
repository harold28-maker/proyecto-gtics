package pe.edu.pucp.skillbridge.dto;

public class HabilidadProyectoDTO {
    private Integer idHabilidad;
    private String nombre;
    private String categoria;
    private String nivel;
    private Integer vacantes;

    public HabilidadProyectoDTO(Integer idHabilidad, String nombre, String categoria, String nivel, Integer vacantes) {
        this.idHabilidad = idHabilidad;
        this.nombre = nombre;
        this.categoria = categoria;
        this.nivel = nivel;
        this.vacantes = vacantes;
    }

    public Integer getIdHabilidad() { return idHabilidad; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getNivel() { return nivel; }
    public Integer getVacantes() { return vacantes; }
}
