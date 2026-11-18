package mx.uv.proyectos.iot;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "alerta_iot")
public class AlertaIot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dispositivo_id", nullable = false)
    private Long dispositivoId;
    @Column(name = "lectura_id")
    private Long lecturaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAlerta tipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Severidad severidad;
    @Column(nullable = false, length = 300)
    private String mensaje;
    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    protected AlertaIot() {
    }

    public AlertaIot(Long dispositivoId, Long lecturaId, TipoAlerta tipo, Severidad severidad, String mensaje,
                     Instant fechaHora) {
        this.dispositivoId = dispositivoId;
        this.lecturaId = lecturaId;
        this.tipo = tipo;
        this.severidad = severidad;
        this.mensaje = mensaje;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public Long getDispositivoId() { return dispositivoId; }
    public Long getLecturaId() { return lecturaId; }
    public TipoAlerta getTipo() { return tipo; }
    public Severidad getSeveridad() { return severidad; }
    public String getMensaje() { return mensaje; }
    public Instant getFechaHora() { return fechaHora; }
}
