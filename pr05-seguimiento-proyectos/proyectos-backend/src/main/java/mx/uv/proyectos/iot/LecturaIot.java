package mx.uv.proyectos.iot;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "lectura_iot")
public class LecturaIot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dispositivo_id", nullable = false)
    private Long dispositivoId;
    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;
    @Column(nullable = false, length = 40)
    private String variable;
    @Column(nullable = false, length = 15)
    private String unidad;
    @Column(nullable = false)
    private Double valor;
    @Column(nullable = false)
    private boolean anomala;
    @Column(name = "motivo_anomalia", length = 40)
    private String motivoAnomalia;

    protected LecturaIot() {
    }

    public LecturaIot(Long dispositivoId, Instant fechaHora, String variable, String unidad, Double valor,
                      boolean anomala, String motivoAnomalia) {
        this.dispositivoId = dispositivoId;
        this.fechaHora = fechaHora;
        this.variable = variable;
        this.unidad = unidad;
        this.valor = valor;
        this.anomala = anomala;
        this.motivoAnomalia = motivoAnomalia;
    }

    public Long getId() { return id; }
    public Long getDispositivoId() { return dispositivoId; }
    public Instant getFechaHora() { return fechaHora; }
    public String getVariable() { return variable; }
    public String getUnidad() { return unidad; }
    public Double getValor() { return valor; }
    public boolean isAnomala() { return anomala; }
    public String getMotivoAnomalia() { return motivoAnomalia; }
}
