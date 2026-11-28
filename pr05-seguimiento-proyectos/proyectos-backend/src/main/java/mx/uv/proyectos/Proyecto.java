package mx.uv.proyectos;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "proyectos")
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 250)
    private String integrantes;

    @Column(nullable = false, length = 120)
    private String asesor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.REGISTRADO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    protected Proyecto() {
    }

    public Proyecto(String titulo, String descripcion, String integrantes, String asesor) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.integrantes = integrantes;
        this.asesor = asesor;
    }

    public void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getIntegrantes() { return integrantes; }
    public String getAsesor() { return asesor; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
