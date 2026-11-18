package mx.uv.proyectos.iot;

/**
 * Reglas de análisis de una lectura (clase pura, sin dependencias de Spring, fácil de probar).
 * 1) Fuera de rango físico      -> anómala (FUERA_DE_RANGO)
 * 2) Salto brusco vs última válida -> anómala (SALTO_BRUSCO)
 * 3) Umbrales sobre lecturas válidas: >= advertencia -> ADVERTENCIA, >= crítico -> CRITICO
 */
public class ReglasIot {
    public static final String FUERA_DE_RANGO = "FUERA_DE_RANGO";
    public static final String SALTO_BRUSCO = "SALTO_BRUSCO";

    private final double umbralAdvertencia;
    private final double umbralCritico;
    private final double rangoMin;
    private final double rangoMax;
    private final double saltoMax;

    public ReglasIot(double umbralAdvertencia, double umbralCritico, double rangoMin, double rangoMax,
                     double saltoMax) {
        this.umbralAdvertencia = umbralAdvertencia;
        this.umbralCritico = umbralCritico;
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
        this.saltoMax = saltoMax;
    }

    /** Resultado de evaluar una lectura. */
    public static class Evaluacion {
        private final boolean anomala;
        private final String motivo;
        private final NivelIot nivel;

        public Evaluacion(boolean anomala, String motivo, NivelIot nivel) {
            this.anomala = anomala;
            this.motivo = motivo;
            this.nivel = nivel;
        }

        public boolean isAnomala() { return anomala; }
        public String getMotivo() { return motivo; }
        public NivelIot getNivel() { return nivel; }
    }

    /** @param previoValido última lectura no anómala (null si no hay). */
    public Evaluacion evaluar(Double previoValido, double valor) {
        if (valor < rangoMin || valor > rangoMax) {
            return new Evaluacion(true, FUERA_DE_RANGO, NivelIot.SIN_DATOS);
        }
        if (previoValido != null && Math.abs(valor - previoValido) > saltoMax) {
            return new Evaluacion(true, SALTO_BRUSCO, NivelIot.SIN_DATOS);
        }
        return new Evaluacion(false, null, nivelPara(valor));
    }

    public NivelIot nivelPara(double valor) {
        if (valor >= umbralCritico) {
            return NivelIot.CRITICO;
        }
        if (valor >= umbralAdvertencia) {
            return NivelIot.ADVERTENCIA;
        }
        return NivelIot.OK;
    }

    public double getUmbralAdvertencia() { return umbralAdvertencia; }
    public double getUmbralCritico() { return umbralCritico; }
    public double getRangoMin() { return rangoMin; }
    public double getRangoMax() { return rangoMax; }
    public double getSaltoMax() { return saltoMax; }
}
