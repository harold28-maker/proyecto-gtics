package pe.edu.pucp.skillbridge.entity;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");

        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        factory.afterPropertiesSet();
        validator = factory;
    }

    @Test
    void usuarioRechazaDatosPersonalesInvalidos() {
        Usuario usuario = new Usuario();
        usuario.setRol(new Rol());
        usuario.setNombres("123");
        usuario.setApellidos("");
        usuario.setCorreo("correo-invalido");
        usuario.setPasswordHash("hash");
        usuario.setTelefono("123");

        var campos = validator.validate(usuario).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(campos).contains("nombres", "apellidos", "correo", "telefono");
    }

    @Test
    void proyectoRechazaRangosYPeriodoInvalidos() {
        Proyecto proyecto = new Proyecto();
        proyecto.setProjectManager(new Usuario());
        proyecto.setNombre(" ");
        proyecto.setVacantes(-1);
        proyecto.setProgreso(101);
        proyecto.setFechaInicio(LocalDate.of(2026, 10, 10));
        proyecto.setFechaFin(LocalDate.of(2026, 10, 1));

        var campos = validator.validate(proyecto).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(campos).contains("nombre", "vacantes", "progreso", "periodoValido");
    }

    @Test
    void asignacionRechazaDedicacionYPeriodoInvalidos() {
        Asignacion asignacion = new Asignacion();
        asignacion.setProyecto(new Proyecto());
        asignacion.setColaborador(new Colaborador());
        asignacion.setRolProyecto("Backend");
        asignacion.setPorcentajeDedicacion(0);
        asignacion.setFechaInicio(LocalDate.of(2026, 10, 10));
        asignacion.setFechaFin(LocalDate.of(2026, 10, 1));

        var campos = validator.validate(asignacion).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(campos).contains("porcentajeDedicacion", "periodoValido");
    }

    @Test
    void certificacionRechazaUrlYPeriodoInvalidos() {
        Certificacion certificacion = new Certificacion();
        certificacion.setColaborador(new Colaborador());
        certificacion.setNombre("Spring");
        certificacion.setUrlCredencial("archivo-local");
        certificacion.setFechaEmision(LocalDate.of(2026, 10, 10));
        certificacion.setFechaExpiracion(LocalDate.of(2026, 10, 1));

        var campos = validator.validate(certificacion).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(campos).contains("urlCredencial", "periodoValido");
    }

    @Test
    void mensajesSeResuelvenDesdeMessagesProperties() {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(" ");

        var mensajeNombre = validator.validate(proyecto).stream()
                .filter(v -> v.getPropertyPath().toString().equals("nombre"))
                .findFirst()
                .orElseThrow()
                .getMessage();

        assertThat(mensajeNombre).isEqualTo("El nombre del proyecto es obligatorio");
    }

    @Test
    void habilidadRechazaCamposVaciosYLimitesDelFormulario() {
        Habilidad habilidad = new Habilidad();
        habilidad.setNombre("A".repeat(41));
        habilidad.setCategoria(" ");
        habilidad.setDescripcion("");

        var campos = validator.validate(habilidad).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(campos).contains("nombre", "categoria", "descripcion");
    }
}
