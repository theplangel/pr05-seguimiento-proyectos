package mx.uv.proyectos.iot;

import java.time.Instant;

/** Estadísticas del rango consultado (min/max/promedio solo sobre lecturas no anómalas). */
public class ResumenIot {
    private final Instant desde;
    private final Instant hasta;
    private final long lecturas;
    private final long anomalas;
    private final Double minimo;
    private final Double maximo;
    private final Double promedio;

    public ResumenIot(Instant desde, Instant hasta, long lecturas, long anomalas, Double minimo, Double maximo,
                      Double promedio) {
        this.desde = desde;
        this.hasta = hasta;
        this.lecturas = lecturas;
        this.anomalas = anomalas;
        this.minimo = minimo;
        this.maximo = maximo;
        this.promedio = promedio;
    }

    public Instant getDesde() { return desde; }
    public Instant getHasta() { return hasta; }
    public long getLecturas() { return lecturas; }
    public long getAnomalas() { return anomalas; }
    public Double getMinimo() { return minimo; }
    public Double getMaximo() { return maximo; }
    public Double getPromedio() { return promedio; }
}
