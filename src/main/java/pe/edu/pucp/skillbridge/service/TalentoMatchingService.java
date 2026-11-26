package pe.edu.pucp.skillbridge.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.skillbridge.dto.CargaColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO;
import pe.edu.pucp.skillbridge.dto.HabilidadColaboradorDTO.Nivel;
import pe.edu.pucp.skillbridge.entity.Asignacion;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Habilidad;
import pe.edu.pucp.skillbridge.entity.Proyecto;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.AsignacionRepository;
import pe.edu.pucp.skillbridge.repository.CertificacionRepository;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.repository.HabilidadRepository;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.Criterio;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.Interpretacion;
import pe.edu.pucp.skillbridge.service.GeminiTalentoService.Tipo;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TalentoMatchingService {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");

    private final ColaboradorRepository colaboradores;
    private final HabilidadRepository habilidades;
    private final CertificacionRepository certificaciones;
    private final AsignacionRepository asignaciones;
    private final CapacidadColaboradorService capacidad;

    public TalentoMatchingService(ColaboradorRepository colaboradores, HabilidadRepository habilidades,
                                  CertificacionRepository certificaciones, AsignacionRepository asignaciones,
                                  CapacidadColaboradorService capacidad) {
        this.colaboradores = colaboradores;
        this.habilidades = habilidades;
        this.certificaciones = certificaciones;
        this.asignaciones = asignaciones;
        this.capacidad = capacidad;
    }

    public List<String> catalogoActivo() {
        return habilidades.findByEstadoTrueOrderByNombreAsc().stream().map(Habilidad::getNombre).toList();
    }

    @Transactional(readOnly = true)
    public Resultado buscar(Interpretacion consulta, LocalDate desde, LocalDate hasta, Integer dedicacion,
                            boolean verHistorial) {
        if (consulta.pideAclaracion() || consulta.criterios().isEmpty()) {
            return new Resultado(consulta.cantidad(), 0, List.of(), List.of(),
                    "Indica las habilidades o el tipo de proyectos en los que buscas experiencia.");
        }

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        List<Colaborador> equipo = colaboradores.listarConUsuario().stream()
                .filter(c -> c.getUsuario().getEstado() == Usuario.EstadoUsuario.ACTIVO)
                .toList();
        Map<Integer, CargaColaboradorDTO> capacidades = desde == null
                ? capacidad.consultarCapacidades(equipo, hoy)
                : capacidad.consultarCapacidadesPeriodo(equipo, desde, hasta);
        Set<String> catalogo = catalogoActivo().stream().map(TalentoMatchingService::normalizar)
                .collect(Collectors.toSet());
        List<Criterio> criterios = consulta.criterios().stream().distinct().toList();
        String comparar = normalizar(consulta.compararHabilidad());
        List<Candidato> compatibles = new ArrayList<>();
        int dedicacionMinima = dedicacion == null ? 1 : dedicacion;
        for (Colaborador perfil : equipo) {
            int libre = capacidades.get(perfil.getIdColaborador()).getDisponibilidad();
            if (libre < dedicacionMinima) continue;
            Candidato candidato = evaluar(perfil, criterios, comparar, catalogo, libre, hoy, verHistorial);
            if (candidato != null) compatibles.add(candidato);
        }

        Comparator<Candidato> orden = Comparator.comparing(Candidato::completo).reversed();
        if (!comparar.isEmpty()) {
            orden = orden.thenComparing(Candidato::aniosComparados, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        List<Candidato> candidatos = compatibles.stream()
                .sorted(orden.thenComparing(Comparator.comparingInt(Candidato::coincidencias).reversed())
                        .thenComparing(Candidato::aniosRelacionados, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingInt(Candidato::proyectosRelacionados).reversed())
                        .thenComparing(Comparator.comparingInt(Candidato::disponible).reversed())
                        .thenComparing(Candidato::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Candidato::idColaborador))
                .limit(consulta.cantidad())
                .toList();
        int completos = (int) candidatos.stream().filter(Candidato::completo).count();
        List<String> criteriosVisibles = criterios.stream()
                .map(c -> etiqueta(c) + (c.obligatorio() ? " · obligatorio" : ""))
                .toList();
        return new Resultado(consulta.cantidad(), completos, criteriosVisibles, candidatos,
                resumir(candidatos, completos, consulta.compararHabilidad()));
    }

    private Candidato evaluar(Colaborador perfil, List<Criterio> criterios, String comparar, Set<String> catalogo,
                              int libre, LocalDate hoy, boolean verHistorial) {
        Integer id = perfil.getIdColaborador();
        Map<String, HabilidadColaboradorDTO> registradas = colaboradores.obtenerHabilidades(id).stream()
                .map(HabilidadColaboradorDTO::desdeFila)
                .filter(h -> catalogo.contains(normalizar(h.getNombre())))
                .collect(Collectors.toMap(h -> normalizar(h.getNombre()), h -> h, (primera, otra) -> primera));
        List<Proyecto> historial = asignaciones.findByColaboradorIdColaboradorOrderByFechaInicioDesc(id).stream()
                .filter(a -> a.registraExperiencia(hoy))
                .map(Asignacion::getProyecto).distinct().toList();
        List<String> certificados = certificaciones.findByColaboradorIdColaboradorOrderByFechaEmisionDescIdCertificacionDesc(id)
                .stream().map(c -> c.getNombre()).toList();

        List<Evidencia> evidencias = new ArrayList<>();
        int coincidencias = 0;
        BigDecimal anios = BigDecimal.ZERO;
        for (Criterio criterio : criterios) {
            Evidencia evidencia;
            if (criterio.tipo() == Tipo.HABILIDAD) {
                HabilidadColaboradorDTO habilidad = registradas.get(normalizar(criterio.termino()));
                evidencia = evaluarHabilidad(criterio, habilidad, certificados);
                if (evidencia.cumple() && habilidad.getAniosExperiencia() != null) anios = anios.add(habilidad.getAniosExperiencia());
            } else {
                Proyecto anterior = historial.stream()
                        .filter(pr -> menciona(pr.getNombre() + " " + pr.getDescripcion(), criterio.termino()))
                        .findFirst().orElse(null);
                String motivo = anterior == null ? "Sin evidencia en el historial"
                        : verHistorial ? "Proyecto registrado · " + anterior.getNombre() : "Proyecto relacionado registrado";
                evidencia = new Evidencia(etiqueta(criterio), motivo, anterior != null);
            }
            if (criterio.obligatorio() && !evidencia.cumple()) return null;
            if (evidencia.cumple()) coincidencias++;
            evidencias.add(evidencia);
        }
        if (coincidencias == 0) return null;

        int antecedentes = (int) historial.stream().filter(pr -> criterios.stream()
                .anyMatch(c -> menciona(pr.getNombre() + " " + pr.getDescripcion(), c.termino()))).count();
        if (verHistorial && criterios.stream().noneMatch(c -> c.tipo() == Tipo.HISTORIAL) && antecedentes > 0) {
            evidencias.add(new Evidencia("Proyectos anteriores", antecedentes == 1 ? "1 proyecto relacionado"
                    : antecedentes + " proyectos relacionados", true));
        }
        HabilidadColaboradorDTO comparada = registradas.get(comparar);
        return new Candidato(id, perfil.getUsuario().getNombreCompleto(), perfil.getCargo(),
                perfil.getSeniority() == null ? "" : perfil.getSeniority().getEtiqueta(),
                libre, coincidencias == criterios.size(), List.copyOf(evidencias), coincidencias, anios, antecedentes,
                comparada == null ? null : comparada.getAniosExperiencia());
    }

    private Evidencia evaluarHabilidad(Criterio criterio, HabilidadColaboradorDTO habilidad, List<String> certificados) {
        if (habilidad == null) {
            boolean certificado = certificados.stream().anyMatch(titulo -> menciona(titulo, criterio.termino()));
            return new Evidencia(etiqueta(criterio), certificado ? "Certificación registrada · práctica por verificar"
                    : "Sin habilidad registrada", false);
        }

        Nivel nivel = nivelRegistrado(habilidad.getNivel());
        BigDecimal anios = habilidad.getAniosExperiencia();
        boolean nivelCumple = criterio.nivel() == null || nivel != null && nivel.ordinal() >= criterio.nivel().ordinal();
        boolean experienciaCumple = criterio.aniosMinimos() == null
                || anios != null && anios.compareTo(criterio.aniosMinimos()) >= 0;
        String prueba = nivel == null ? "Nivel por verificar" : nivel.getEtiqueta();
        if (anios != null) prueba += " · " + aniosTexto(anios);
        return new Evidencia(etiqueta(criterio), prueba, nivelCumple && experienciaCumple);
    }

    private Nivel nivelRegistrado(String codigo) {
        if (codigo == null) return null;
        for (Nivel nivel : Nivel.values()) {
            if (nivel.name().equalsIgnoreCase(codigo)) return nivel;
        }
        return null;
    }

    private String resumir(List<Candidato> candidatos, int completos, String comparar) {
        if (candidatos.isEmpty()) {
            return "No encontré perfiles que cumplan las condiciones obligatorias y la capacidad solicitada.";
        }
        if (!normalizar(comparar).isEmpty()) {
            BigDecimal mayor = candidatos.stream().map(Candidato::aniosComparados).filter(a -> a != null)
                    .max(Comparator.naturalOrder()).orElse(null);
            if (mayor == null) return "La experiencia en " + comparar.strip() + " queda por verificar en estos perfiles.";
            String nombres = candidatos.stream().filter(c -> c.aniosComparados() != null
                            && c.aniosComparados().compareTo(mayor) == 0)
                    .map(Candidato::nombre).collect(Collectors.joining(", "));
            return "Mayor experiencia registrada en " + comparar.strip() + " entre los candidatos mostrados · "
                    + nombres + " · " + aniosTexto(mayor) + ".";
        }
        String resumen = candidatos.size() == 1 ? "Encontré un perfil para revisar."
                : "Encontré " + candidatos.size() + " perfiles para revisar.";
        return completos == candidatos.size() ? resumen
                : resumen + " Revisa los requisitos pendientes en las tarjetas.";
    }

    private static String etiqueta(Criterio criterio) {
        String etiqueta = criterio.tipo() == Tipo.HISTORIAL ? "Proyectos de " + criterio.termino().strip()
                : criterio.termino().strip();
        if (criterio.tipo() == Tipo.HABILIDAD && criterio.nivel() != null) etiqueta += " · " + criterio.nivel().getEtiqueta();
        if (criterio.tipo() == Tipo.HABILIDAD && criterio.aniosMinimos() != null) etiqueta += " · " + aniosTexto(criterio.aniosMinimos()) + " o más";
        return etiqueta;
    }

    private static String aniosTexto(BigDecimal anios) {
        String texto = anios.stripTrailingZeros().toPlainString().replace('.', ',');
        return texto + (anios.compareTo(BigDecimal.ONE) == 0 ? " año" : " años");
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .strip().toLowerCase(Locale.ROOT);
    }

    private static boolean menciona(String texto, String termino) {
        String contenido = " " + normalizar(texto).replaceAll("[^\\p{L}\\p{N}+#]+", " ").strip() + " ";
        String palabra = normalizar(termino).replaceAll("[^\\p{L}\\p{N}+#]+", " ").strip();
        return !palabra.isEmpty() && contenido.contains(" " + palabra + " ");
    }

    public record Evidencia(String etiqueta, String motivo, boolean cumple) {}
    public record Candidato(Integer idColaborador, String nombre, String cargo, String nivel, int disponible,
                            boolean completo, List<Evidencia> evidencias, int coincidencias, BigDecimal aniosRelacionados,
                            int proyectosRelacionados, BigDecimal aniosComparados) {}
    public record Resultado(int solicitados, int completos, List<String> criterios, List<Candidato> candidatos,
                            String respuesta) {}
}
