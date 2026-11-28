package mx.uv.proyectos;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String mensaje;
    private final String ruta;
    private final Map<String, String> campos;

    public ErrorResponse(int status, String error, String mensaje, String ruta, Map<String, String> campos) {
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.ruta = ruta;
        this.campos = campos;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMensaje() { return mensaje; }
    public String getRuta() { return ruta; }
    public Map<String, String> getCampos() { return campos; }
}
