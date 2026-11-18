package mx.uv.proyectos.iot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IotServiceTest {
    private static final Instant AHORA = Instant.parse("2026-11-10T18:00:00Z");

    @Mock DispositivoRepository dispositivos;
    @Mock LecturaIotRepository lecturas;
    @Mock AlertaIotRepository alertas;

    IotService servicio;
    Dispositivo sensor;

    @BeforeEach
    void preparar() {
        sensor = new Dispositivo("sensor-lab-01", "Sensor", "temperatura", "°C", 5);
        when(dispositivos.findByCodigo("sensor-lab-01")).thenReturn(Optional.of(sensor));
        when(dispositivos.findFirstByOrderByIdAsc()).thenReturn(Optional.of(sensor));
        when(lecturas.save(any(LecturaIot.class))).thenAnswer(i -> i.getArgument(0));
        servicio = new IotService(dispositivos, lecturas, alertas, new ReglasIot(30, 35, -10, 80, 8), 15);
    }

    private LecturaRequest lectura(double valor) {
        return new LecturaRequest("sensor-lab-01", AHORA, "temperatura", "°C", valor);
    }

    private void conPrevio(double valor) {
        when(lecturas.findFirstByDispositivoIdAndAnomalaFalseOrderByFechaHoraDescIdDesc(any()))
            .thenReturn(Optional.of(new LecturaIot(1L, AHORA.minusSeconds(5), "temperatura", "°C", valor, false, null)));
    }

    private void sinPrevio() {
        when(lecturas.findFirstByDispositivoIdAndAnomalaFalseOrderByFechaHoraDescIdDesc(any())).thenReturn(Optional.empty());
    }

    // ---------------- ingesta
    @Test
    void lecturaNormalSeGuardaSinAlertaYMarcaConectado() {
        conPrevio(24.0);
        LecturaResponse r = servicio.registrar(lectura(25.0), AHORA);
        assertThat(r.isAnomala()).isFalse();
        assertThat(sensor.getConexion()).isEqualTo(ConexionIot.CONECTADO);
        assertThat(sensor.getUltimaLectura()).isEqualTo(AHORA);
        verify(alertas, never()).save(any());
    }

    @Test
    void cruzarElUmbralDeAdvertenciaGeneraUnaAlerta() {
        conPrevio(29.0);
        servicio.registrar(lectura(31.0), AHORA);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.ADVERTENCIA && a.getSeveridad() == Severidad.ADVERTENCIA));
    }

    @Test
    void cruzarElUmbralCriticoGeneraAlertaCritica() {
        conPrevio(32.0);
        servicio.registrar(lectura(36.0), AHORA);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.CRITICA && a.getSeveridad() == Severidad.CRITICA));
    }

    @Test
    void permanecerEnElMismoNivelNoRepiteLaAlerta() {
        conPrevio(31.0);
        servicio.registrar(lectura(32.0), AHORA);
        verify(alertas, never()).save(any());
    }

    @Test
    void volverANormalGeneraNormalizacion() {
        conPrevio(31.0);
        servicio.registrar(lectura(28.0), AHORA);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.NORMALIZACION && a.getSeveridad() == Severidad.INFO));
    }

    @Test
    void lecturaFueraDeRangoSeGuardaMarcadaComoAnomalaConAlerta() {
        conPrevio(25.0);
        LecturaResponse r = servicio.registrar(lectura(120.0), AHORA);
        assertThat(r.isAnomala()).isTrue();
        assertThat(r.getMotivoAnomalia()).isEqualTo(ReglasIot.FUERA_DE_RANGO);
        verify(lecturas).save(any(LecturaIot.class));
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.ANOMALIA));
    }

    @Test
    void primeraLecturaSinHistorialNoGeneraAlertaSiEsNormal() {
        sinPrevio();
        servicio.registrar(lectura(25.0), AHORA);
        verify(alertas, never()).save(any());
    }

    @Test
    void lecturaTrasDesconexionGeneraReconexion() {
        conPrevio(25.0);
        sensor.setConexion(ConexionIot.DESCONECTADO);
        servicio.registrar(lectura(25.5), AHORA);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.RECONEXION));
        assertThat(sensor.getConexion()).isEqualTo(ConexionIot.CONECTADO);
    }

    // ---------------- validación
    @Test
    void dispositivoDesconocidoLanzaNoEncontrado() {
        when(dispositivos.findByCodigo("otro")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.registrar(new LecturaRequest("otro", AHORA, "temperatura", "°C", 20.0), AHORA))
            .isInstanceOf(DispositivoNoEncontradoException.class);
    }

    @Test
    void variableOUnidadDistintaSeRechaza() {
        assertThatThrownBy(() -> servicio.registrar(new LecturaRequest("sensor-lab-01", AHORA, "humedad", "°C", 20.0), AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        assertThatThrownBy(() -> servicio.registrar(new LecturaRequest("sensor-lab-01", AHORA, "temperatura", "°F", 20.0), AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        verify(lecturas, never()).save(any());
    }

    @Test
    void fechaEnElFuturoOMuyAntiguaSeRechaza() {
        assertThatThrownBy(() -> servicio.registrar(
            new LecturaRequest("sensor-lab-01", AHORA.plusSeconds(300), "temperatura", "°C", 20.0), AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        assertThatThrownBy(() -> servicio.registrar(
            new LecturaRequest("sensor-lab-01", AHORA.minusSeconds(90000), "temperatura", "°C", 20.0), AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
    }

    @Test
    void sinFechaUsaLaHoraDeRecepcion() {
        sinPrevio();
        LecturaResponse r = servicio.registrar(new LecturaRequest("sensor-lab-01", null, "temperatura", "°C", 20.0), AHORA);
        assertThat(r.getFechaHora()).isEqualTo(AHORA);
    }

    // ---------------- desconexión
    @Test
    void marcaDesconectadoTrasElTiempoLimite() {
        sensor.setConexion(ConexionIot.CONECTADO);
        sensor.setUltimaLectura(AHORA.minusSeconds(20));
        when(dispositivos.findAll()).thenReturn(Collections.singletonList(sensor));
        assertThat(servicio.verificarDesconexion(AHORA)).isEqualTo(1);
        assertThat(sensor.getConexion()).isEqualTo(ConexionIot.DESCONECTADO);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.DESCONEXION && a.getSeveridad() == Severidad.CRITICA));
    }

    @Test
    void noMarcaDesconectadoDentroDelLimiteNiDosVeces() {
        sensor.setConexion(ConexionIot.CONECTADO);
        sensor.setUltimaLectura(AHORA.minusSeconds(10));
        when(dispositivos.findAll()).thenReturn(Collections.singletonList(sensor));
        assertThat(servicio.verificarDesconexion(AHORA)).isZero();

        sensor.setConexion(ConexionIot.DESCONECTADO);
        sensor.setUltimaLectura(AHORA.minusSeconds(100));
        assertThat(servicio.verificarDesconexion(AHORA)).isZero();
        verify(alertas, never()).save(any());
    }

    // ---------------- consultas
    @Test
    void rangoInvertidoOLimiteInvalidoSeRechaza() {
        assertThatThrownBy(() -> servicio.lecturas(AHORA, AHORA.minusSeconds(60), null, AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        assertThatThrownBy(() -> servicio.lecturas(null, null, 0, AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        assertThatThrownBy(() -> servicio.lecturas(null, null, 5000, AHORA))
            .isInstanceOf(DatoIotInvalidoException.class);
        assertThatThrownBy(() -> servicio.alertas(0)).isInstanceOf(DatoIotInvalidoException.class);
    }

    @Test
    void estadoSinLecturasEsSinDatos() {
        sinPrevio();
        EstadoIotResponse e = servicio.estado(AHORA);
        assertThat(e.getNivel()).isEqualTo(NivelIot.SIN_DATOS);
        assertThat(e.getValorActual()).isNull();
    }

    @Test
    void estadoReflejaElNivelDeLaUltimaLecturaValida() {
        conPrevio(36.0);
        assertThat(servicio.estado(AHORA).getNivel()).isEqualTo(NivelIot.CRITICO);
    }
}
