package mx.uv.proyectos.iot;

import java.time.Instant;

/** Estado actual de la fuente IoT para el dashboard. */
public class EstadoIotResponse {
    private final String dispositivo;
    private final String nombre;
    private final String variable;
    private final String unidad;
    private final ConexionIot conexion;
    private final NivelIot nivel;
    private final Double valorActual;
    private final Instant fechaHoraUltimaLectura;
    private final Instant ultimaRecepcion;
    private final Instant ahora;
    private final int frecuenciaSegundos;
    private final double umbralAdvertencia;
    private final double umbralCritico;
    private final int desconexionSegundos;

    public EstadoIotResponse(Dispositivo d, NivelIot nivel, LecturaIot ultimaValida, ReglasIot reglas,
                             int desconexionSegundos, Instant ahora) {
        this.dispositivo = d.getCodigo();
        this.nombre = d.getNombre();
        this.variable = d.getVariable();
        this.unidad = d.getUnidad();
        this.conexion = d.getConexion();
        this.nivel = nivel;
        this.valorActual = ultimaValida == null ? null : ultimaValida.getValor();
        this.fechaHoraUltimaLectura = ultimaValida == null ? null : ultimaValida.getFechaHora();
        this.ultimaRecepcion = d.getUltimaLectura();
        this.ahora = ahora;
        this.frecuenciaSegundos = d.getFrecuenciaSeg();
        this.umbralAdvertencia = reglas.getUmbralAdvertencia();
        this.umbralCritico = reglas.getUmbralCritico();
        this.desconexionSegundos = desconexionSegundos;
    }

    public String getDispositivo() { return dispositivo; }
    public String getNombre() { return nombre; }
    public String getVariable() { return variable; }
    public String getUnidad() { return unidad; }
    public ConexionIot getConexion() { return conexion; }
    public NivelIot getNivel() { return nivel; }
    public Double getValorActual() { return valorActual; }
    public Instant getFechaHoraUltimaLectura() { return fechaHoraUltimaLectura; }
    public Instant getUltimaRecepcion() { return ultimaRecepcion; }
    public Instant getAhora() { return ahora; }
    public int getFrecuenciaSegundos() { return frecuenciaSegundos; }
    public double getUmbralAdvertencia() { return umbralAdvertencia; }
    public double getUmbralCritico() { return umbralCritico; }
    public int getDesconexionSegundos() { return desconexionSegundos; }
}
