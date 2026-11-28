package mx.uv.proyectos.iot;

import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Cuerpo de POST /api/iot/lecturas. */
public class LecturaRequest {
    @NotBlank(message = "El dispositivo es obligatorio")
    @Size(max = 40, message = "El dispositivo admite máximo 40 caracteres")
    private String dispositivo;

    /** Opcional: si falta se usa la hora de recepción. Formato ISO-8601 UTC (2026-11-10T18:30:00Z). */
    private Instant fechaHora;

    @NotBlank(message = "La variable es obligatoria")
    @Size(max = 40, message = "La variable admite máximo 40 caracteres")
    private String variable;

    @NotBlank(message = "La unidad es obligatoria")
    @Size(max = 15, message = "La unidad admite máximo 15 caracteres")
    private String unidad;

    @NotNull(message = "El valor es obligatorio")
    private Double valor;

    public LecturaRequest() {
    }

    public LecturaRequest(String dispositivo, Instant fechaHora, String variable, String unidad, Double valor) {
        this.dispositivo = dispositivo;
        this.fechaHora = fechaHora;
        this.variable = variable;
        this.unidad = unidad;
        this.valor = valor;
    }

    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo == null ? null : dispositivo.trim(); }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable == null ? null : variable.trim(); }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad == null ? null : unidad.trim(); }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
}
