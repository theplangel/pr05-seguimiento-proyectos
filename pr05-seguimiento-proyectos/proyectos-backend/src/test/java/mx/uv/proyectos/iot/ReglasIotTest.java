package mx.uv.proyectos.iot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReglasIotTest {
    // advertencia 30, crítico 35, rango [-10, 80], salto máximo 8
    private final ReglasIot reglas = new ReglasIot(30, 35, -10, 80, 8);

    @Test
    void valorNormalEsOk() {
        ReglasIot.Evaluacion e = reglas.evaluar(24.0, 25.0);
        assertThat(e.isAnomala()).isFalse();
        assertThat(e.getNivel()).isEqualTo(NivelIot.OK);
    }

    @Test
    void umbralesSeEvaluanEnElLimite() {
        assertThat(reglas.nivelPara(29.99)).isEqualTo(NivelIot.OK);
        assertThat(reglas.nivelPara(30.0)).isEqualTo(NivelIot.ADVERTENCIA);
        assertThat(reglas.nivelPara(34.99)).isEqualTo(NivelIot.ADVERTENCIA);
        assertThat(reglas.nivelPara(35.0)).isEqualTo(NivelIot.CRITICO);
    }

    @Test
    void fueraDeRangoEsAnomala() {
        ReglasIot.Evaluacion alto = reglas.evaluar(25.0, 120.0);
        assertThat(alto.isAnomala()).isTrue();
        assertThat(alto.getMotivo()).isEqualTo(ReglasIot.FUERA_DE_RANGO);
        assertThat(reglas.evaluar(25.0, -40.0).isAnomala()).isTrue();
    }

    @Test
    void saltoBruscoEsAnomalo() {
        ReglasIot.Evaluacion e = reglas.evaluar(25.0, 34.0);
        assertThat(e.isAnomala()).isTrue();
        assertThat(e.getMotivo()).isEqualTo(ReglasIot.SALTO_BRUSCO);
    }

    @Test
    void saltoIgualAlMaximoNoEsAnomalo() {
        assertThat(reglas.evaluar(25.0, 33.0).isAnomala()).isFalse();
    }

    @Test
    void sinLecturaPreviaNoHaySalto() {
        assertThat(reglas.evaluar(null, 70.0).isAnomala()).isFalse();
    }
}
