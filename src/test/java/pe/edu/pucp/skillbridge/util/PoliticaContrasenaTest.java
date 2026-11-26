package pe.edu.pucp.skillbridge.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaContrasenaTest {

    @Test
    void laClaveTemporalGeneradaCumpleLaPoliticaDeContrasenas() {
        for (int i = 0; i < 20; i++) {
            String clave = PoliticaContrasena.generarTemporal();

            assertThat(clave.length()).isEqualTo(16);
            assertThat(PoliticaContrasena.validar(clave, clave)).isNull();
        }
    }
}
