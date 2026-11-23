package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ForoFormulario {
    @Positive(message = "Selecciona una publicación válida.")
    private Integer idForo;
    @Positive(message = "Selecciona un proyecto válido.")
    private Integer idProyecto;
    @NotBlank(message = "{validation.foro.titulo.required}")
    @Size(max = 180, message = "{validation.foro.titulo.size}")
    private String titulo;
    @NotBlank(message = "{validation.foro.contenido.required}")
    @Size(max = 5000, message = "{validation.foro.contenido.size}")
    private String contenido;
    @NotBlank(message = "Selecciona una categoría.")
    private String categoria = "General";
    @Size(max = 20, message = "Selecciona hasta 20 etiquetas.")
    private List<Integer> etiquetas = new ArrayList<>();

    public Integer getIdForo() { return idForo; }
    public void setIdForo(Integer idForo) { this.idForo = idForo; }
    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public List<Integer> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<Integer> etiquetas) { this.etiquetas = etiquetas == null ? new ArrayList<>() : etiquetas; }
}
