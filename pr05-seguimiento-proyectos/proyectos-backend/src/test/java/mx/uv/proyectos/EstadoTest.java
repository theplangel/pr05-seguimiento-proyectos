package mx.uv.proyectos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoTest {
    @Test
    void permiteElFlujoNormal() {
        assertThat(Estado.REGISTRADO.puedePasarA(Estado.EN_PROGRESO)).isTrue();
        assertThat(Estado.EN_PROGRESO.puedePasarA(Estado.EN_REVISION)).isTrue();
        assertThat(Estado.EN_REVISION.puedePasarA(Estado.ENTREGADO)).isTrue();
        assertThat(Estado.EN_REVISION.puedePasarA(Estado.EN_PROGRESO)).isTrue();
    }

    @Test
    void rechazaSaltosYRetrocesosInvalidos() {
        assertThat(Estado.REGISTRADO.puedePasarA(Estado.ENTREGADO)).isFalse();
        assertThat(Estado.EN_PROGRESO.puedePasarA(Estado.REGISTRADO)).isFalse();
        assertThat(Estado.ENTREGADO.siguientes()).isEmpty();
        assertThat(Estado.ENTREGADO.puedePasarA(Estado.EN_PROGRESO)).isFalse();
    }
}
