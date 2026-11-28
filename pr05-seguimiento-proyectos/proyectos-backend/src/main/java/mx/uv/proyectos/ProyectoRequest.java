package mx.uv.proyectos;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Cuerpo de POST /api/proyectos. */
public class ProyectoRequest {
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 120, message = "El título admite máximo 120 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción admite máximo 500 caracteres")
    private String descripcion;

    @NotBlank(message = "Los integrantes son obligatorios")
    @Size(max = 250, message = "Los integrantes admiten máximo 250 caracteres")
    private String integrantes;

    @NotBlank(message = "El asesor es obligatorio")
    @Size(max = 120, message = "El asesor admite máximo 120 caracteres")
    private String asesor;

    public ProyectoRequest() {
    }

    public ProyectoRequest(String titulo, String descripcion, String integrantes, String asesor) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.integrantes = integrantes;
        this.asesor = asesor;
    }

    private static String limpia(String valor) {
        return valor == null ? null : valor.trim();
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = limpia(titulo); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = limpia(descripcion); }
    public String getIntegrantes() { return integrantes; }
    public void setIntegrantes(String integrantes) { this.integrantes = limpia(integrantes); }
    public String getAsesor() { return asesor; }
    public void setAsesor(String asesor) { this.asesor = limpia(asesor); }
}
