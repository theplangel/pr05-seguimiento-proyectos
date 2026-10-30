package mx.uv.proyectos;

import java.time.LocalDateTime;
import java.util.List;

public class ProyectoResponse {
    private final Long id;
    private final String titulo;
    private final String descripcion;
    private final String integrantes;
    private final String asesor;
    private final Estado estado;
    private final List<Estado> estadosPermitidos;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    public ProyectoResponse(Proyecto p) {
        this.id = p.getId();
        this.titulo = p.getTitulo();
        this.descripcion = p.getDescripcion();
        this.integrantes = p.getIntegrantes();
        this.asesor = p.getAsesor();
        this.estado = p.getEstado();
        this.estadosPermitidos = p.getEstado().siguientes();
        this.fechaCreacion = p.getFechaCreacion();
        this.fechaActualizacion = p.getFechaActualizacion();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getIntegrantes() { return integrantes; }
    public String getAsesor() { return asesor; }
    public Estado getEstado() { return estado; }
    public List<Estado> getEstadosPermitidos() { return estadosPermitidos; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
