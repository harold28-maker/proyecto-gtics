package pe.edu.pucp.skillbridge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaPreguntaVacia() {
        var errores = validator.validate(new ChatbotRequest("   "));

        assertThat(errores).anyMatch(error -> error.getPropertyPath().toString().equals("mensaje"));
    }

    @Test
    void rechazaPreguntaMayorAMilCaracteres() {
        var errores = validator.validate(new ChatbotRequest("a".repeat(1001)));

        assertThat(errores).anyMatch(error -> error.getPropertyPath().toString().equals("mensaje"));
    }

    @Test
    void aceptaPreguntaValida() {
        assertThat(validator.validate(new ChatbotRequest("¿Dónde reviso mis asignaciones?"))).isEmpty();
    }
}
