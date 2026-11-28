package mx.uv.proyectos.iot;

import java.time.Instant;

public class LecturaResponse {
    private final Long id;
    private final String dispositivo;
    private final Instant fechaHora;
    private final String variable;
    private final String unidad;
    private final Double valor;
    private final boolean anomala;
    private final String motivoAnomalia;

    public LecturaResponse(LecturaIot l, String codigoDispositivo) {
        this.id = l.getId();
        this.dispositivo = codigoDispositivo;
        this.fechaHora = l.getFechaHora();
        this.variable = l.getVariable();
        this.unidad = l.getUnidad();
        this.valor = l.getValor();
        this.anomala = l.isAnomala();
        this.motivoAnomalia = l.getMotivoAnomalia();
    }

    public Long getId() { return id; }
    public String getDispositivo() { return dispositivo; }
    public Instant getFechaHora() { return fechaHora; }
    public String getVariable() { return variable; }
    public String getUnidad() { return unidad; }
    public Double getValor() { return valor; }
    public boolean isAnomala() { return anomala; }
    public String getMotivoAnomalia() { return motivoAnomalia; }
}
