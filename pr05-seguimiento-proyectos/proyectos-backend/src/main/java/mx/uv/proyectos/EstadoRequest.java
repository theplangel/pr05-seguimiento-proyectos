package mx.uv.proyectos;

import javax.validation.constraints.NotNull;

/** Cuerpo de PATCH /api/proyectos/{id}/estado. */
public class EstadoRequest {
    @NotNull(message = "El estado es obligatorio")
    private Estado estado;

    public EstadoRequest() {
    }

    public EstadoRequest(Estado estado) {
        this.estado = estado;
    }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
