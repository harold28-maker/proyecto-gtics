package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO.Clasificacion;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO;
import pe.edu.pucp.skillbridge.dto.FilaAsignacionDTO.EstadoParticipacion;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadProyectoDTO;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Auditoria;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Habilidad;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Proyecto.EstadoProyecto;
import pe.edu.pucp.skillbridge.entity.Proyecto.Prioridad;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.AuditoriaRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.HabilidadRepository;
import pe.edu.pucp.skillbridge.repository.ProyectoRepository;
import pe.edu.pucp.skillbridge.service.CapacidadColaboradorService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pm")
public class ProjectManagerController {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int TAMANO_PAGINA = 6;
    private static final Set<String> NIVELES_HABILIDAD = Set.of("BASICO", "INTERMEDIO", "AVANZADO", "EXPERTO");

    private final ProyectoRepository proyectos;
    private final AsignacionRepository asignaciones;
    private final ColaboradorRepository colaboradores;
    private final HabilidadRepository habilidades;
    private final CapacidadColaboradorService capacidad;
    private final AuditoriaRepository auditoria;

    public ProjectManagerController(ProyectoRepository proyectos, AsignacionRepository asignaciones,
                                    ColaboradorRepository colaboradores, HabilidadRepository habilidades,
                                    CapacidadColaboradorService capacidad, AuditoriaRepository auditoria) {
        this.proyectos = proyectos;
        this.asignaciones = asignaciones;
        this.colaboradores = colaboradores;
        this.habilidades = habilidades;
        this.capacidad = capacidad;
        this.auditoria = auditoria;
    }

    @InitBinder("proyecto")
    public void prepararFormulario(WebDataBinder binder) {
        binder.setAllowedFields("idProyecto", "nombre", "descripcion", "tecnologias", "fechaInicio",
                "fechaFin", "vacantes", "prioridad", "estado", "progreso");
        binder.registerCustomEditor(String.class, "nombre", new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "descripcion", new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "tecnologias", new StringTrimmerEditor(true));
    }

    @RequestMapping(value = {"/inicio", "/dashboard"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String inicio(@RequestAttribute("usuarioVerificado") Usuario pm,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        LocalDate hoy = LocalDate.now(LIMA);
        model.addAttribute("titulo", "Inicio");
        model.addAttribute("pm", pm);
        boolean verProyectos = !bloqueados.contains("proyectos");
        boolean verAsignaciones = !bloqueados.contains("asignaciones");
        model.addAttribute("verProyectos", verProyectos);
        model.addAttribute("verAsignaciones", verAsignaciones);
        if (verProyectos) {
            List<Proyecto> portafolio = portafolio(pm);
            List<Proyecto> enCurso = portafolio.stream().filter(p -> p.getEstado() == EstadoProyecto.ACTIVE).toList();
            model.addAttribute("activos", enCurso.size());
            model.addAttribute("planificacion", contarEstado(portafolio, EstadoProyecto.PLANNING));
            model.addAttribute("vencidos", enCurso.stream().filter(p -> finVencido(p, hoy)).count());
            model.addAttribute("fechas", enCurso.stream().filter(p -> p.getFechaFin() != null)
                    .sorted(Comparator.comparing(Proyecto::getFechaFin).thenComparing(Proyecto::getIdProyecto))
                    .limit(5).map(p -> new FechaProyecto(p, ChronoUnit.DAYS.between(hoy, p.getFechaFin()))).toList());
        }
        if (verAsignaciones) {
            var vigentes = asignacionesPm(pm).stream().filter(a -> a.estaVigente(hoy)).toList();
            model.addAttribute("vigentes", vigentes.size());
            model.addAttribute("ultimasAsignaciones", vigentes.stream().limit(5).toList());
        }
        return "pm/inicio";
    }

    public record FechaProyecto(Proyecto proyecto, long dias) {
        public String getEtiqueta() {
            if (dias < 0) return "Vencido";
            if (dias == 0) return "Hoy";
            return dias == 1 ? "En 1 día" : "En " + dias + " días";
        }
    }

    @GetMapping("/proyectos")
    public String proyectos(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "") String estado,
                            @RequestParam(defaultValue = "") String prioridad,
                            @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        var errores = new LinkedHashMap<String, String>();
        String texto = buscarTexto(q, errores);
        EstadoProyecto estadoFiltro = filtro(estado, EstadoProyecto.class, "estado", "Selecciona un estado válido.", errores);
        Prioridad prioridadFiltro = filtro(prioridad, Prioridad.class, "prioridad", "Selecciona una prioridad válida.", errores);
        List<Proyecto> propios = portafolio(pm);
        var visibles = errores.isEmpty() ? propios.stream()
                .filter(p -> (estadoFiltro == null || p.getEstado() == estadoFiltro)
                        && (prioridadFiltro == null || p.getPrioridad() == prioridadFiltro)
                        && contiene(p.getNombre(), texto)).toList() : List.<Proyecto>of();
        model.addAttribute("titulo", "Proyectos");
        model.addAttribute("total", propios.size());
        model.addAttribute("activos", contarEstado(propios, EstadoProyecto.ACTIVE));
        model.addAttribute("planificacion", contarEstado(propios, EstadoProyecto.PLANNING));
        model.addAttribute("cerrados", propios.stream().filter(Proyecto::isCerrado).count());
        model.addAttribute("proyectos", visibles);
        model.addAttribute("hoy", LocalDate.now(LIMA));
        model.addAttribute("q", q.strip());
        model.addAttribute("estados", EstadoProyecto.values());
        model.addAttribute("prioridades", Prioridad.values());
        model.addAttribute("estadoSeleccionado", estadoFiltro);
        model.addAttribute("prioridadSeleccionada", prioridadFiltro);
        model.addAttribute("erroresFiltros", errores);
        return "pm/proyectos";
    }

    @GetMapping("/proyectos/nuevo")
    public String nuevoProyecto(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        return formularioProyecto(null, model);
    }

    @GetMapping("/proyectos/{id}")
    public String fichaProyecto(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        Proyecto proyecto = propio(id, pm);
        model.addAttribute("titulo", "Proyectos");
        cargarFicha(proyecto, model);
        return "pm/proyecto-ficha";
    }

    @GetMapping("/proyectos/editar/{id}")
    public String editarProyecto(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        Proyecto proyecto = propio(id, pm);
        if (proyecto.isCerrado()) return "redirect:/pm/proyectos/" + proyecto.getIdProyecto();
        model.addAttribute("proyecto", proyecto);
        return formularioProyecto(id, model);
    }

    @PostMapping("/proyectos/guardar")
    @Transactional
    public String guardarProyecto(@Valid @ModelAttribute("proyecto") Proyecto formulario, BindingResult errores,
                                  @RequestAttribute("usuarioVerificado") Usuario pm, Model model,
                                  RedirectAttributes redirect) {
        Integer id = formulario.getIdProyecto();
        Proyecto proyecto = id == null ? new Proyecto() : propioParaEditar(id, pm);
        if (errores.hasFieldErrors("idProyecto")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa los datos del proyecto.");
        }
        validarFechaProyecto(formulario.getFechaInicio(), "fechaInicio", errores);
        validarFechaProyecto(formulario.getFechaFin(), "fechaFin", errores);
        if (errores.hasErrors()) return formularioProyecto(id, model);

        if (id == null) proyecto.setProjectManager(pm);
        proyecto.setNombre(formulario.getNombre());
        proyecto.setDescripcion(formulario.getDescripcion());
        proyecto.setTecnologias(formulario.getTecnologias());
        proyecto.setFechaInicio(formulario.getFechaInicio());
        proyecto.setFechaFin(formulario.getFechaFin());
        proyecto.setVacantes(formulario.getVacantes());
        proyecto.setPrioridad(formulario.getPrioridad());
        proyecto.setEstado(formulario.getEstado());
        proyecto.setProgreso(formulario.getProgreso());
        proyectos.save(proyecto);
        registrarEvento(pm, id == null ? "Crear proyecto" : "Editar proyecto", proyecto.getNombre());
        redirect.addFlashAttribute("mensajeExito", "Proyecto guardado correctamente.");
        return "redirect:/pm/proyectos";
    }

    private void validarFechaProyecto(LocalDate fecha, String campo, BindingResult errores) {
        if (fecha != null && (fecha.getYear() < 1000 || fecha.getYear() > 9999)) {
            errores.rejectValue(campo, "proyecto.fecha", "Ingresa un año entre 1000 y 9999.");
        }
    }

    private String formularioProyecto(Integer id, Model model) {
        model.addAttribute("titulo", id == null ? "Nuevo proyecto" : "Editar proyecto");
        model.addAttribute("estados", EstadoProyecto.values());
        model.addAttribute("prioridades", Prioridad.values());
        return "pm/proyecto-form";
    }

    @GetMapping("/proyectos/{id}/cancelar")
    public String confirmarCancelacion(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        Proyecto proyecto = propio(id, pm);
        if (proyecto.isCerrado()) return "redirect:/pm/proyectos/" + id;
        model.addAttribute("titulo", "Cancelar proyecto");
        model.addAttribute("proyecto", proyecto);
        return "pm/proyecto-cancelar";
    }

    @PostMapping("/proyectos/{id}/cancelar")
    @Transactional
    public String cancelarProyecto(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm,
                                   RedirectAttributes redirect) {
        Proyecto proyecto = propioParaEditar(id, pm);
        proyecto.setEstado(EstadoProyecto.CANCELLED);
        registrarEvento(pm, "Cancelar proyecto", proyecto.getNombre());
        redirect.addFlashAttribute("mensajeExito", "Proyecto cancelado. Puedes consultarlo en Historial.");
        return "redirect:/pm/proyectos";
    }

    @GetMapping("/proyectos/{id}/habilidades")
    public String habilidadesRequeridas(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        return vistaHabilidades(propio(id, pm), model);
    }

    @PostMapping("/proyectos/{id}/habilidades/guardar")
    @Transactional
    public String guardarHabilidadProyecto(@PathVariable Integer id,
                                           @RequestParam(defaultValue = "") String idHabilidad,
                                           @RequestParam(defaultValue = "") String nivel,
                                           @RequestParam(defaultValue = "") String vacantes,
                                           @RequestAttribute("usuarioVerificado") Usuario pm, Model model,
                                           RedirectAttributes redirect) {
        Proyecto proyecto = propioParaEditar(id, pm);
        Map<String, String> errores = new LinkedHashMap<>();
        Integer habilidad = enteroPositivo(idHabilidad);
        Integer cantidad = enteroPositivo(vacantes);
        if (habilidad == null || !habilidades.buscarParaActualizar(habilidad).map(Habilidad::getEstado).orElse(false)) {
            errores.put("idHabilidad", "Selecciona una habilidad activa del catálogo.");
        }
        if (!NIVELES_HABILIDAD.contains(nivel)) errores.put("nivel", "Selecciona un nivel válido.");
        if (cantidad == null) errores.put("vacantes", "Ingresa un número entero mayor que cero.");
        if (!errores.isEmpty()) {
            model.addAttribute("erroresFiltros", errores);
            model.addAttribute("habilidadSeleccionada", habilidad);
            model.addAttribute("nivelSeleccionado", nivel);
            model.addAttribute("vacantes", vacantes);
            return vistaHabilidades(proyecto, model);
        }
        proyectos.guardarHabilidadRequerida(id, habilidad, nivel, cantidad);
        registrarEvento(pm, "Actualizar habilidad requerida", proyecto.getNombre());
        redirect.addFlashAttribute("mensajeExito", "Requerimiento técnico actualizado.");
        return "redirect:/pm/proyectos/" + id + "/habilidades";
    }

    @PostMapping("/proyectos/{id}/habilidades/{idHabilidad}/eliminar")
    @Transactional
    public String eliminarHabilidadProyecto(@PathVariable Integer id, @PathVariable Integer idHabilidad,
                                             @RequestAttribute("usuarioVerificado") Usuario pm, RedirectAttributes redirect) {
        Proyecto proyecto = propioParaEditar(id, pm);
        if (proyectos.eliminarHabilidadRequerida(id, idHabilidad) > 0) {
            registrarEvento(pm, "Retirar habilidad requerida", proyecto.getNombre());
            redirect.addFlashAttribute("mensajeExito", "Habilidad retirada del proyecto.");
        }
        return "redirect:/pm/proyectos/" + id + "/habilidades";
    }

    private String vistaHabilidades(Proyecto proyecto, Model model) {
        model.addAttribute("titulo", "Habilidades requeridas");
        cargarFicha(proyecto, model);
        if (!proyecto.isCerrado()) model.addAttribute("catalogo", habilidades.findByEstadoTrueOrderByNombreAsc());
        return "pm/proyecto-habilidades";
    }

    private void cargarFicha(Proyecto proyecto, Model model) {
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("requerimientos", proyectos.obtenerHabilidadesRequeridas(proyecto.getIdProyecto()).stream()
                .map(fila -> new HabilidadProyectoDTO(((Number) fila[0]).intValue(), String.valueOf(fila[1]),
                        (String) fila[2], (String) fila[3], fila[4] == null ? 0 : ((Number) fila[4]).intValue()))
                .toList());
    }

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "") String proyecto,
                               @RequestParam(defaultValue = "") String estado,
                               @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        LocalDate hoy = LocalDate.now(LIMA);
        var errores = new LinkedHashMap<String, String>();
        String texto = buscarTexto(q, errores);
        EstadoParticipacion estadoFiltro = filtro(estado, EstadoParticipacion.class, "estado", "Selecciona un estado válido.", errores);
        List<Proyecto> opciones = portafolio(pm);
        Integer idProyecto = proyecto.isBlank() ? null : enteroPositivo(proyecto);
        if (!proyecto.isBlank() && (idProyecto == null || opciones.stream().noneMatch(p -> p.getIdProyecto().equals(idProyecto)))) {
            errores.put("proyecto", "Selecciona un proyecto a tu cargo.");
        }
        var filas = asignacionesPm(pm).stream().map(a -> FilaAsignacionDTO.desde(a, hoy)).toList();
        var visibles = errores.isEmpty() ? filas.stream()
                .filter(f -> idProyecto == null || f.asignacion().getProyecto().getIdProyecto().equals(idProyecto))
                .filter(f -> estadoFiltro == null || f.estado() == estadoFiltro)
                .filter(f -> contiene(f.asignacion().getColaborador().getUsuario().getNombreCompleto(), texto)
                        || contiene(f.asignacion().getRolProyecto(), texto)).toList() : List.<FilaAsignacionDTO>of();
        model.addAttribute("titulo", "Asignaciones");
        model.addAttribute("total", filas.size());
        model.addAttribute("vigentes", filas.stream().filter(f -> f.estado() == EstadoParticipacion.VIGENTE).count());
        model.addAttribute("planificadas", filas.stream().filter(f -> f.estado() == EstadoParticipacion.PLANIFICADA).count());
        model.addAttribute("vencidas", filas.stream().filter(f -> f.estado() == EstadoParticipacion.VENCIDA).count());
        model.addAttribute("filas", visibles);
        model.addAttribute("proyectos", opciones);
        model.addAttribute("proyectoSeleccionado", idProyecto);
        model.addAttribute("estados", EstadoParticipacion.values());
        model.addAttribute("estadoSeleccionado", estadoFiltro);
        model.addAttribute("q", q.strip());
        model.addAttribute("erroresFiltros", errores);
        return "pm/asignaciones";
    }

    @GetMapping("/colaboradores")
    public String colaboradores(@RequestParam(defaultValue = "") String q,
                                @RequestParam(defaultValue = "") String nivel,
                                @RequestParam(defaultValue = "") String disponibilidad,
                                @RequestParam(defaultValue = "1") String page,
                                @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Map<String, String> errores = new LinkedHashMap<>();
        buscarTexto(q, errores);
        var nivelFiltro = filtro(nivel, Colaborador.Seniority.class, "nivel", "Selecciona un nivel válido.", errores);
        var clasificacion = filtro(disponibilidad, Clasificacion.class, "disponibilidad", "Selecciona una disponibilidad válida.", errores);
        boolean verCapacidad = !bloqueados.contains("disponibilidad");
        if (clasificacion != null && !verCapacidad) errores.put("disponibilidad", "Contacta a Administración para consultar la disponibilidad.");
        Integer pagina = enteroPositivo(page);
        if (pagina == null) errores.put("page", "Selecciona una página válida.");

        List<Colaborador> encontrados = errores.isEmpty()
                ? (q.isBlank() ? colaboradores.listarConUsuario() : colaboradores.buscar(q.strip())).stream()
                    .filter(c -> nivelFiltro == null || c.getSeniority() == nivelFiltro)
                    .sorted(Comparator.comparing(Colaborador::getIdColaborador)).toList()
                : List.of();
        var cargas = verCapacidad ? capacidad.consultarCapacidades(encontrados) : Map.<Integer, CargaColaboradorDTO>of();
        if (clasificacion != null && verCapacidad) {
            encontrados = encontrados.stream()
                    .filter(c -> cargas.get(c.getIdColaborador()).getClasificacion() == clasificacion).toList();
        }
        int total = encontrados.size();
        int paginas = Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA);
        int actual = pagina == null ? 1 : Math.min(pagina, paginas);
        int inicio = (actual - 1) * TAMANO_PAGINA;
        int fin = Math.min(inicio + TAMANO_PAGINA, total);
        var visibles = encontrados.subList(inicio, fin);
        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaboradores", visibles);
        model.addAttribute("habilidadesPorPersona", visibles.stream().collect(Collectors.toMap(
                Colaborador::getIdColaborador, c -> habilidadesDe(c.getIdColaborador()))));
        model.addAttribute("capacidades", cargas);
        model.addAttribute("verCapacidad", verCapacidad);
        model.addAttribute("niveles", Colaborador.Seniority.values());
        model.addAttribute("nivelSeleccionado", nivelFiltro);
        model.addAttribute("clasificaciones", Clasificacion.values());
        model.addAttribute("disponibilidadSeleccionada", clasificacion);
        model.addAttribute("q", q.strip());
        model.addAttribute("paginaActual", actual);
        model.addAttribute("totalPaginas", paginas);
        model.addAttribute("total", total);
        model.addAttribute("desde", inicio + 1);
        model.addAttribute("hasta", fin);
        model.addAttribute("erroresFiltros", errores);
        return "pm/colaboradores";
    }

    @GetMapping("/colaboradores/{id}")
    public String perfil(@PathVariable Integer id, @RequestAttribute("usuarioVerificado") Usuario pm,
                         @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        Colaborador colaborador = colaboradores.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El colaborador no está disponible."));
        model.addAttribute("titulo", "Colaboradores");
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("habilidades", habilidadesDe(id));
        boolean verAsignaciones = !bloqueados.contains("asignaciones");
        boolean verHistorial = !bloqueados.contains("historial");
        model.addAttribute("puedeAsignar", verAsignaciones && colaborador.getUsuario().getEstado() == Usuario.EstadoUsuario.ACTIVO);
        if (!bloqueados.contains("disponibilidad")) model.addAttribute("capacidad", capacidad.consultarActual(colaborador));
        if (verAsignaciones || verHistorial) {
            LocalDate hoy = LocalDate.now(LIMA);
            var participaciones = asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(id).stream()
                    .filter(a -> a.getProyecto().getProjectManager() != null
                            && pm.getIdUsuario().equals(a.getProyecto().getProjectManager().getIdUsuario())).toList();
            if (verAsignaciones) model.addAttribute("asignacionesActuales", participaciones.stream()
                    .filter(a -> a.estaVigente(hoy)).map(a -> FilaAsignacionDTO.desde(a, hoy)).toList());
            if (verHistorial) model.addAttribute("proyectosAnteriores", participaciones.stream()
                    .filter(a -> a.getFechaInicio() != null && !a.getFechaInicio().isAfter(hoy) && a.esHistorica(hoy))
                    .map(a -> FilaAsignacionDTO.desde(a, hoy)).toList());
        }
        return "resource/colaborador-perfil";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(@RequestParam(defaultValue = "") String desde,
                                 @RequestParam(defaultValue = "") String hasta,
                                 @RequestParam(defaultValue = "") String modo,
                                 @RequestAttribute("permisosBloqueados") Set<String> bloqueados, Model model) {
        LocalDate hoy = LocalDate.now(LIMA);
        boolean periodo = "periodo".equals(modo) || !desde.isBlank() || !hasta.isBlank();
        var errores = new LinkedHashMap<String, String>();
        if (!modo.isBlank() && !"periodo".equals(modo)) errores.put("modo", "Selecciona un período válido.");
        LocalDate inicio = periodo ? fechaValida(desde, "desde", errores) : hoy;
        LocalDate fin = periodo ? fechaValida(hasta, "hasta", errores) : hoy;
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            errores.put("hasta", "La fecha de fin debe ser igual o posterior a la de inicio.");
        }
        List<CargaColaboradorDTO> cargas = List.of();
        if (errores.isEmpty()) {
            var activos = capacidad.listarPerfilesActivos();
            if (!bloqueados.contains("colaboradores")) {
                model.addAttribute("usuariosPorColaborador", activos.stream().collect(Collectors.toMap(
                        Colaborador::getIdColaborador, Colaborador::getUsuario)));
            }
            var resumen = periodo ? capacidad.consultarCapacidadesPeriodo(activos, inicio, fin)
                    : capacidad.consultarCapacidades(activos, hoy);
            cargas = resumen.values().stream()
                    .sorted(Comparator.comparing(CargaColaboradorDTO::getDisponibilidad)
                            .thenComparing(CargaColaboradorDTO::getColaborador, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        model.addAttribute("titulo", "Disponibilidad");
        model.addAttribute("hoy", hoy);
        model.addAttribute("porPeriodo", periodo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("cargas", cargas);
        model.addAttribute("disponibles", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.DISPONIBLE).count());
        model.addAttribute("parciales", cargas.stream().filter(c -> c.getClasificacion() == Clasificacion.PARCIAL).count());
        model.addAttribute("sinDisponibilidad", cargas.stream().filter(c -> c.getDisponibilidad() == 0).count());
        model.addAttribute("erroresFiltros", errores);
        return "pm/disponibilidad";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "") String estado,
                            @RequestParam(required = false) Integer idProyecto,
                            @RequestAttribute("usuarioVerificado") Usuario pm, Model model) {
        var errores = new LinkedHashMap<String, String>();
        String texto = buscarTexto(q, errores);
        EstadoProyecto estadoFiltro = filtro(estado, EstadoProyecto.class, "estado", "Selecciona un estado final válido.", errores);
        if (estadoFiltro != null && estadoFiltro != EstadoProyecto.COMPLETED && estadoFiltro != EstadoProyecto.CANCELLED) {
            errores.put("estado", "Selecciona Completado o Cancelado.");
        }
        List<Proyecto> cerrados = portafolio(pm).stream().filter(Proyecto::isCerrado).toList();
        var visibles = errores.isEmpty() ? cerrados.stream()
                .filter(p -> (estadoFiltro == null || p.getEstado() == estadoFiltro) && contiene(p.getNombre(), texto))
                .sorted(Comparator.comparing(Proyecto::getFechaFin, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Proyecto::getIdProyecto, Comparator.reverseOrder())).toList() : List.<Proyecto>of();
        if (idProyecto != null) {
            Proyecto ficha = propio(idProyecto, pm);
            if (!ficha.isCerrado()) throw noEncontrado();
            cargarFicha(ficha, model);
        } else if (visibles.size() == 1) {
            cargarFicha(visibles.get(0), model);
        }
        model.addAttribute("titulo", "Historial");
        model.addAttribute("proyectos", visibles);
        model.addAttribute("cerrados", cerrados.size());
        model.addAttribute("completados", contarEstado(cerrados, EstadoProyecto.COMPLETED));
        model.addAttribute("cancelados", contarEstado(cerrados, EstadoProyecto.CANCELLED));
        model.addAttribute("estadoSeleccionado", estadoFiltro);
        model.addAttribute("q", q.strip());
        model.addAttribute("erroresFiltros", errores);
        return "pm/historial";
    }

    private List<Proyecto> portafolio(Usuario pm) {
        return proyectos.findByProjectManagerIdUsuarioOrderByFechaCreacionDesc(pm.getIdUsuario());
    }

    private List<Asignacion> asignacionesPm(Usuario pm) {
        return asignaciones.findByProyectoProjectManagerIdUsuarioOrderByFechaInicioDesc(pm.getIdUsuario());
    }

    private List<HabilidadColaboradorDTO> habilidadesDe(Integer id) {
        return colaboradores.obtenerHabilidades(id).stream().map(HabilidadColaboradorDTO::desdeFila).toList();
    }

    private Proyecto propio(Integer id, Usuario pm) {
        return proyectos.findById(id).filter(p -> p.getProjectManager() != null
                && pm.getIdUsuario().equals(p.getProjectManager().getIdUsuario())).orElseThrow(ProjectManagerController::noEncontrado);
    }

    private Proyecto propioParaEditar(Integer id, Usuario pm) {
        Proyecto proyecto = proyectos.buscarPropioParaActualizar(id, pm.getIdUsuario())
                .orElseThrow(ProjectManagerController::noEncontrado);
        if (proyecto.isCerrado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Los proyectos cerrados se consultan en Historial.");
        }
        return proyecto;
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "El proyecto no está disponible.");
    }

    private void registrarEvento(Usuario pm, String accion, String detalle) {
        Auditoria registro = new Auditoria();
        registro.setUsuario(pm);
        registro.setAccion(accion);
        registro.setModulo("Proyectos");
        registro.setDetalle(detalle);
        auditoria.save(registro);
    }

    private static long contarEstado(List<Proyecto> lista, EstadoProyecto estado) {
        return lista.stream().filter(p -> p.getEstado() == estado).count();
    }

    private static boolean finVencido(Proyecto proyecto, LocalDate hoy) {
        return proyecto.getFechaFin() != null && proyecto.getFechaFin().isBefore(hoy);
    }

    private static boolean contiene(String valor, String texto) {
        return texto.isEmpty() || (valor != null && valor.toLowerCase(Locale.ROOT).contains(texto));
    }

    private String buscarTexto(String q, Map<String, String> errores) {
        String texto = q.strip();
        if (texto.length() > 150) errores.put("q", "La búsqueda admite hasta 150 caracteres.");
        return texto.toLowerCase(Locale.ROOT);
    }

    private <T extends Enum<T>> T filtro(String valor, Class<T> tipo, String campo, String mensaje, Map<String, String> errores) {
        String codigo = valor.strip().toUpperCase(Locale.ROOT);
        if (codigo.isEmpty() || "TODOS".equals(codigo)) return null;
        try {
            return Enum.valueOf(tipo, codigo);
        } catch (IllegalArgumentException ex) {
            errores.put(campo, mensaje);
            return null;
        }
    }

    private Integer enteroPositivo(String valor) {
        try {
            int numero = Integer.parseInt(valor.strip());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate fechaValida(String valor, String campo, Map<String, String> errores) {
        try {
            LocalDate fecha = LocalDate.parse(valor.strip());
            if (fecha.getYear() >= 1000 && fecha.getYear() <= 9999) return fecha;
        } catch (DateTimeParseException ignored) {
        }
        errores.put(campo, "Selecciona una fecha válida.");
        return null;
    }
}
