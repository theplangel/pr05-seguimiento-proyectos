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
@Table(name = "dispositivo")
public class Dispositivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 40)
    private String variable;
    @Column(nullable = false, length = 15)
    private String unidad;
    @Column(name = "proyecto_id")
    private Long proyectoId;
    @Column(name = "frecuencia_seg", nullable = false)
    private Integer frecuenciaSeg;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ConexionIot conexion = ConexionIot.SIN_DATOS;
    @Column(name = "ultima_lectura")
    private Instant ultimaLectura;

    protected Dispositivo() {
    }

    public Dispositivo(String codigo, String nombre, String variable, String unidad, Integer frecuenciaSeg) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.variable = variable;
        this.unidad = unidad;
        this.frecuenciaSeg = frecuenciaSeg;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getVariable() { return variable; }
    public String getUnidad() { return unidad; }
    public Long getProyectoId() { return proyectoId; }
    public Integer getFrecuenciaSeg() { return frecuenciaSeg; }
    public ConexionIot getConexion() { return conexion; }
    public void setConexion(ConexionIot conexion) { this.conexion = conexion; }
    public Instant getUltimaLectura() { return ultimaLectura; }
    public void setUltimaLectura(Instant ultimaLectura) { this.ultimaLectura = ultimaLectura; }
}
