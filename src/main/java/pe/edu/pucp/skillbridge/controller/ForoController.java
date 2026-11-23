package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.dto.ForoFormulario;
import pe.edu.pucp.skillbridge.entity.EtiquetaForo;
import pe.edu.pucp.skillbridge.entity.Foro;
import pe.edu.pucp.skillbridge.entity.RespuestaForo;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.EtiquetaForoRepository;
import pe.edu.pucp.skillbridge.repository.ForoRepository;
import pe.edu.pucp.skillbridge.repository.RespuestaForoRepository;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService;
import pe.edu.pucp.skillbridge.service.AccesoProyectosService.Acceso;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Foro de proyectos y temas generales para colaboradores y gestores. */
@Controller
public class ForoController {
    public static final List<String> CATEGORIAS_FORO = List.of(
            "General", "Backend", "Frontend", "API e Integraciones", "Base de Datos",
            "DevOps / Cloud", "Testing / QA", "Arquitectura", "Gestión de Proyecto");
    private static final Set<String> ESTADOS = Set.of("TODOS", "ABIERTO", "RESUELTO", "CERRADO");
    private static final int POR_PAGINA = 5;

    private final ForoRepository foros;
    private final RespuestaForoRepository respuestas;
    private final EtiquetaForoRepository etiquetas;
    private final AccesoProyectosService accesoProyectos;

    public ForoController(ForoRepository foros, RespuestaForoRepository respuestas,
                          EtiquetaForoRepository etiquetas, AccesoProyectosService accesoProyectos) {
        this.foros = foros;
        this.respuestas = respuestas;
        this.etiquetas = etiquetas;
        this.accesoProyectos = accesoProyectos;
    }

    public record SesionForo(Usuario usuario, Map<Integer, Acceso> proyectos) {
        String rolVista() {
            return "PROJECT_MANAGER".equals(usuario.getRol().getNombre()) ? "PM" : "COL";
        }

        boolean puedeConsultar(Foro foro) {
            return foro.getProyecto() == null || proyectos.containsKey(foro.getProyecto().getIdProyecto());
        }

        boolean puedeParticipar(Foro foro) {
            return foro.getProyecto() == null || puedeParticipar(foro.getProyecto().getIdProyecto());
        }

        boolean puedeParticipar(Integer idProyecto) {
            Acceso acceso = proyectos.get(idProyecto);
            return acceso != null && acceso.puedeParticipar();
        }

        boolean modera(Foro foro) {
            return puedeParticipar(foro) && (esUsuario(foro.getAutor())
                    || ("PM".equals(rolVista()) && foro.getProyecto() != null
                        && esUsuario(foro.getProyecto().getProjectManager())));
        }

        boolean esUsuario(Usuario autor) {
            return autor != null && usuario.getIdUsuario().equals(autor.getIdUsuario());
        }
    }

    @ModelAttribute("sesionForo")
    public SesionForo sesion(@RequestAttribute("usuarioVerificado") Usuario usuario,
                            @RequestAttribute("permisosBloqueados") Set<String> bloqueados) {
        return new SesionForo(usuario, accesoProyectos.consultar(usuario, bloqueados));
    }

    @GetMapping({"/colaborador/foros", "/pm/foros"})
    public String listar(@ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                         @RequestParam(defaultValue = "") String q,
                         @RequestParam(value = "idProyecto", defaultValue = "") String proyecto,
                         @RequestParam(defaultValue = "TODAS") String categoria,
                         @RequestParam(defaultValue = "TODOS") String estado,
                         @RequestParam(value = "page", defaultValue = "1") String page, Model model) {
        String texto = q.strip();
        String categoriaFiltro = categoria.isBlank() ? "TODAS" : categoria.strip();
        String estadoFiltro = estado.isBlank() ? "TODOS" : estado.strip().toUpperCase(Locale.ROOT);
        Map<String, String> errores = new LinkedHashMap<>();
        if (texto.length() > 150) errores.put("q", "Escribe hasta 150 caracteres para buscar.");
        Integer idProyecto = leerNumero(proyecto);
        if (!proyecto.isBlank() && (idProyecto == null || (idProyecto != 0 && !sesion.proyectos().containsKey(idProyecto)))) {
            errores.put("idProyecto", "Selecciona un proyecto disponible.");
        }
        Integer numeroPagina = leerNumero(page);
        if (numeroPagina == null || numeroPagina < 1) errores.put("page", "Selecciona una página válida.");
        if (!"TODAS".equals(categoriaFiltro) && CATEGORIAS_FORO.stream().noneMatch(categoriaFiltro::equalsIgnoreCase)) {
            errores.put("categoria", "Selecciona una categoría válida.");
        }
        if (!ESTADOS.contains(estadoFiltro)) errores.put("estado", "Selecciona un estado válido.");

        List<Foro> visibles = foros.listarVisibles(sesion.proyectos().keySet());
        String termino = texto.toLowerCase(Locale.ROOT);
        List<Foro> filtrados = errores.isEmpty() ? visibles.stream()
                .filter(f -> coincideProyecto(f, idProyecto))
                .filter(f -> "TODAS".equals(categoriaFiltro) || categoriaFiltro.equalsIgnoreCase(f.getCategoria()))
                .filter(f -> "TODOS".equals(estadoFiltro) || (f.getEstado() != null && estadoFiltro.equals(f.getEstado().name())))
                .filter(f -> coincideTexto(f, termino))
                .toList() : List.of();

        boolean esPm = "PM".equals(sesion.rolVista());
        int totalPaginas = Math.max(1, (filtrados.size() + POR_PAGINA - 1) / POR_PAGINA);
        int paginaActual = numeroPagina == null ? 1 : Math.max(1, Math.min(numeroPagina, totalPaginas));
        int desde = (paginaActual - 1) * POR_PAGINA;
        int hasta = Math.min(desde + POR_PAGINA, filtrados.size());
        List<Foro> pagina = esPm ? filtrados : filtrados.subList(desde, hasta);
        model.addAttribute("titulo", "Foro de proyectos");
        model.addAttribute("foros", pagina);
        model.addAttribute("q", texto);
        model.addAttribute("proyectoSeleccionado", idProyecto);
        model.addAttribute("proyectos", sesion.proyectos().values().stream().map(Acceso::proyecto).toList());
        model.addAttribute("categoriaSeleccionada", categoriaFiltro);
        model.addAttribute("estadoSeleccionado", estadoFiltro);
        model.addAttribute("categoriasForo", CATEGORIAS_FORO);
        model.addAttribute("erroresFiltros", errores);
        model.addAttribute("totalForos", visibles.size());
        model.addAttribute("abiertos", visibles.stream().filter(f -> f.getEstado() == Foro.EstadoForo.ABIERTO).count());
        model.addAttribute("resueltos", visibles.stream().filter(f -> f.getEstado() == Foro.EstadoForo.RESUELTO).count());
        model.addAttribute("conteoRespuestas", pagina.stream().collect(Collectors.toMap(Foro::getIdForo,
                f -> respuestas.countByForoIdForo(f.getIdForo()))));
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("resultados", filtrados.size());
        model.addAttribute("desde", desde + 1);
        model.addAttribute("hasta", hasta);
        return esPm ? "pm/foros" : "colaborador/foros";
    }

    private Integer leerNumero(String valor) {
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean coincideProyecto(Foro foro, Integer id) {
        if (id == null) return true;
        if (foro.getProyecto() == null) return id == 0;
        return id.equals(foro.getProyecto().getIdProyecto());
    }

    private boolean coincideTexto(Foro foro, String texto) {
        return texto.isEmpty() || foro.getTitulo().toLowerCase(Locale.ROOT).contains(texto)
                || foro.getContenido().toLowerCase(Locale.ROOT).contains(texto)
                || foro.getEtiquetas().stream().anyMatch(e -> e.getNombre().toLowerCase(Locale.ROOT).contains(texto));
    }

    @GetMapping("/foros/{id}")
    public String detalle(@PathVariable Integer id, @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                          Model model) {
        return vistaDetalle(consultarForo(id, sesion, false), sesion, model);
    }

    @GetMapping("/foros/nuevo")
    public String nuevo(@ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion, Model model) {
        model.addAttribute("foroForm", new ForoFormulario());
        return vistaFormulario(sesion, false, model);
    }

    @GetMapping("/foros/{id}/editar")
    public String editar(@PathVariable Integer id, @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                         Model model) {
        Foro foro = consultarForo(id, sesion, false);
        exigir(sesion.modera(foro));
        ForoFormulario formulario = new ForoFormulario();
        formulario.setIdForo(foro.getIdForo());
        formulario.setTitulo(foro.getTitulo());
        formulario.setContenido(foro.getContenido());
        formulario.setCategoria(foro.getCategoria() == null ? "General" : foro.getCategoria());
        formulario.setIdProyecto(foro.getProyecto() == null ? null : foro.getProyecto().getIdProyecto());
        formulario.setEtiquetas(foro.getEtiquetas().stream().map(EtiquetaForo::getIdEtiqueta).toList());
        model.addAttribute("foroForm", formulario);
        model.addAttribute("proyectoPublicacion", foro.getProyecto() == null ? "Tema general" : foro.getProyecto().getNombre());
        return vistaFormulario(sesion, true, model);
    }

    @PostMapping("/foros/guardar")
    @Transactional
    public String guardar(@Valid @ModelAttribute("foroForm") ForoFormulario formulario, BindingResult errores,
                          @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                          Model model, RedirectAttributes redirect) {
        Foro foro = formulario.getIdForo() == null || errores.hasFieldErrors("idForo")
                ? new Foro() : consultarForo(formulario.getIdForo(), sesion, true);
        boolean edicion = foro.getIdForo() != null;
        Integer idProyecto = formulario.getIdProyecto();
        if (edicion) {
            exigir(sesion.modera(foro));
            var proyectoActual = foro.getProyecto();
            Integer idActual = proyectoActual == null ? null : proyectoActual.getIdProyecto();
            model.addAttribute("proyectoPublicacion", proyectoActual == null ? "Tema general" : proyectoActual.getNombre());
            if (!errores.hasFieldErrors("idProyecto") && !Objects.equals(idActual, idProyecto)) {
                errores.rejectValue("idProyecto", "foro.proyecto.inmutable", "La publicación conserva su proyecto original.");
            }
            formulario.setIdProyecto(idActual);
        } else if (!errores.hasFieldErrors("idProyecto") && idProyecto != null && !sesion.puedeParticipar(idProyecto)) {
            errores.rejectValue("idProyecto", "foro.proyecto.invalido", "Selecciona un proyecto en el que puedas publicar.");
        }
        String categoria = formulario.getCategoria() == null ? "" : formulario.getCategoria().strip();
        if (!errores.hasFieldErrors("categoria") && !CATEGORIAS_FORO.contains(categoria)) {
            errores.rejectValue("categoria", "foro.categoria.invalida", "Selecciona una categoría válida.");
        }

        Set<EtiquetaForo> seleccionadas = new LinkedHashSet<>();
        if (!errores.hasFieldErrors("etiquetas")) {
            Set<Integer> ids = new LinkedHashSet<>(formulario.getEtiquetas());
            if (!ids.contains(null)) seleccionadas.addAll(etiquetas.findAllById(ids));
            if (seleccionadas.size() != ids.size()) {
                errores.rejectValue("etiquetas", "foro.etiquetas.invalidas", "Selecciona etiquetas del catálogo.");
            }
        }
        if (errores.hasErrors()) return vistaFormulario(sesion, edicion, model);

        if (!edicion) {
            foro.setAutor(sesion.usuario());
            foro.setEstado(Foro.EstadoForo.ABIERTO);
            foro.setProyecto(idProyecto == null ? null : sesion.proyectos().get(idProyecto).proyecto());
        }
        foro.setTitulo(formulario.getTitulo().strip());
        foro.setContenido(formulario.getContenido().strip());
        foro.setCategoria(categoria);
        foro.setEtiquetas(seleccionadas);
        Foro guardado = foros.save(foro);
        return volverAlTema(guardado.getIdForo(), redirect, "mensajeExito", "Publicación guardada.");
    }

    @PostMapping("/foros/{id}/responder")
    @Transactional
    public String responder(@PathVariable Integer id, @RequestParam(defaultValue = "") String contenido,
                            @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                            Model model, RedirectAttributes redirect) {
        Foro foro = consultarForo(id, sesion, true);
        exigir(sesion.puedeParticipar(foro) && foro.getEstado() != Foro.EstadoForo.CERRADO);
        String texto = contenido.strip();
        if (texto.isBlank() || texto.length() > 5000) {
            model.addAttribute("contenidoRespuesta", contenido);
            model.addAttribute("errorRespuesta", texto.isBlank() ? "Escribe una respuesta." : "La respuesta admite hasta 5000 caracteres.");
            return vistaDetalle(foro, sesion, model);
        }
        RespuestaForo respuesta = new RespuestaForo();
        respuesta.setForo(foro);
        respuesta.setAutor(sesion.usuario());
        respuesta.setContenido(texto);
        respuesta.setEsSolucion(false);
        respuestas.save(respuesta);
        return volverAlTema(id, redirect, "mensajeExito", "Respuesta publicada correctamente.");
    }

    @PostMapping("/foros/{idForo}/solucion/{idRespuesta}")
    @Transactional
    public String marcarSolucion(@PathVariable Integer idForo, @PathVariable Integer idRespuesta,
                                 @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                                 RedirectAttributes redirect) {
        Foro foro = consultarForo(idForo, sesion, true);
        exigir(sesion.modera(foro) && foro.getEstado() != Foro.EstadoForo.CERRADO);
        List<RespuestaForo> lista = respuestas.findByForoIdForoOrderByFechaCreacionAsc(idForo);
        if (lista.stream().noneMatch(r -> idRespuesta.equals(r.getIdRespuesta()))) throw noEncontrado();
        lista.forEach(r -> r.setEsSolucion(idRespuesta.equals(r.getIdRespuesta())));
        foro.setEstado(Foro.EstadoForo.RESUELTO);
        return volverAlTema(idForo, redirect, "mensajeExito", "Respuesta marcada como solución.");
    }

    @PostMapping("/foros/{id}/estado")
    @Transactional
    public String cambiarEstado(@PathVariable Integer id, @RequestParam String estado,
                                @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                                RedirectAttributes redirect) {
        Foro foro = consultarForo(id, sesion, true);
        exigir(sesion.modera(foro));
        if ("ABIERTO".equals(estado)) {
            respuestas.findByForoIdForoOrderByFechaCreacionAsc(id).forEach(r -> r.setEsSolucion(false));
            foro.setEstado(Foro.EstadoForo.ABIERTO);
        } else if ("CERRADO".equals(estado)) {
            foro.setEstado(Foro.EstadoForo.CERRADO);
        } else {
            return volverAlTema(id, redirect, "mensajeError", "Selecciona un estado válido.");
        }
        return volverAlTema(id, redirect, "mensajeExito", "Estado de la publicación actualizado.");
    }

    @PostMapping("/foros/{idForo}/respuestas/{idRespuesta}/eliminar")
    @Transactional
    public String eliminarRespuesta(@PathVariable Integer idForo, @PathVariable Integer idRespuesta,
                                    @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                                    RedirectAttributes redirect) {
        Foro foro = consultarForo(idForo, sesion, true);
        List<RespuestaForo> lista = respuestas.findByForoIdForoOrderByFechaCreacionAsc(idForo);
        RespuestaForo respuesta = lista.stream().filter(r -> idRespuesta.equals(r.getIdRespuesta()))
                .findFirst().orElseThrow(ForoController::noEncontrado);
        exigir(puedeEliminarRespuesta(foro, respuesta, sesion));
        respuestas.delete(respuesta);
        if (foro.getEstado() == Foro.EstadoForo.RESUELTO
                && lista.stream().noneMatch(r -> !idRespuesta.equals(r.getIdRespuesta()) && Boolean.TRUE.equals(r.getEsSolucion()))) {
            foro.setEstado(Foro.EstadoForo.ABIERTO);
        }
        return volverAlTema(idForo, redirect, "mensajeExito", "Respuesta eliminada.");
    }

    @PostMapping("/foros/{id}/eliminar")
    @Transactional
    public String eliminarForo(@PathVariable Integer id, @ModelAttribute(value = "sesionForo", binding = false) SesionForo sesion,
                               RedirectAttributes redirect) {
        Foro foro = consultarForo(id, sesion, true);
        exigir("PM".equals(sesion.rolVista()) && sesion.modera(foro));
        foros.delete(foro);
        redirect.addFlashAttribute("mensajeExito", "Publicación eliminada.");
        return "redirect:/pm/foros";
    }

    private String vistaFormulario(SesionForo sesion, boolean edicion, Model model) {
        model.addAttribute("titulo", edicion ? "Editar publicación" : "Nueva publicación");
        model.addAttribute("rolVista", sesion.rolVista());
        model.addAttribute("proyectos", sesion.proyectos().values().stream()
                .filter(Acceso::puedeParticipar).map(Acceso::proyecto).toList());
        model.addAttribute("categoriasForo", CATEGORIAS_FORO);
        model.addAttribute("etiquetasDisponibles", etiquetas.findAllByOrderByNombreAsc());
        return "foro/form";
    }

    private String vistaDetalle(Foro foro, SesionForo sesion, Model model) {
        List<RespuestaForo> lista = respuestas.findByForoIdForoOrderByFechaCreacionAsc(foro.getIdForo());
        model.addAttribute("titulo", "Publicación del foro");
        model.addAttribute("rolVista", sesion.rolVista());
        model.addAttribute("foro", foro);
        model.addAttribute("respuestas", lista);
        model.addAttribute("totalRespuestas", lista.size());
        model.addAttribute("soluciones", lista.stream().filter(r -> Boolean.TRUE.equals(r.getEsSolucion())).count());
        model.addAttribute("puedeModerar", sesion.modera(foro));
        model.addAttribute("puedeResponder", sesion.puedeParticipar(foro) && foro.getEstado() != Foro.EstadoForo.CERRADO);
        model.addAttribute("respuestasEliminables", lista.stream().filter(r -> puedeEliminarRespuesta(foro, r, sesion))
                .map(RespuestaForo::getIdRespuesta).collect(Collectors.toSet()));
        return "foro/detalle";
    }

    private boolean puedeEliminarRespuesta(Foro foro, RespuestaForo respuesta, SesionForo sesion) {
        return sesion.modera(foro) || (sesion.puedeParticipar(foro) && sesion.esUsuario(respuesta.getAutor()));
    }

    private Foro consultarForo(Integer id, SesionForo sesion, boolean actualizar) {
        var busqueda = actualizar ? foros.buscarParaActualizar(id) : foros.findById(id);
        return busqueda.filter(sesion::puedeConsultar).orElseThrow(ForoController::noEncontrado);
    }

    private String volverAlTema(Integer id, RedirectAttributes redirect, String atributo, String mensaje) {
        redirect.addFlashAttribute(atributo, mensaje);
        return "redirect:/foros/" + id;
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "La publicación o respuesta no está disponible.");
    }

    private void exigir(boolean autorizado) {
        if (!autorizado) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta no puede realizar esta acción.");
    }
}
