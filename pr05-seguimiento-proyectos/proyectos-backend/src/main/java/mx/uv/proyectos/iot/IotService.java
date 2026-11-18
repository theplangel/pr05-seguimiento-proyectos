package mx.uv.proyectos.iot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IotService {
    static final int LIMITE_LECTURAS_DEFECTO = 500;
    static final int LIMITE_LECTURAS_MAX = 2000;
    static final int LIMITE_RESUMEN = 20000;
    static final int LIMITE_ALERTAS_DEFECTO = 50;
    static final int LIMITE_ALERTAS_MAX = 200;
    private static final Duration ANTIGUEDAD_MAX = Duration.ofHours(24);
    private static final Duration FUTURO_MAX = Duration.ofSeconds(60);
    private static final Duration RANGO_MAX = Duration.ofDays(7);

    private final DispositivoRepository dispositivos;
    private final LecturaIotRepository lecturas;
    private final AlertaIotRepository alertas;
    private final ReglasIot reglas;
    private final int desconexionSegundos;

    public IotService(DispositivoRepository dispositivos, LecturaIotRepository lecturas, AlertaIotRepository alertas,
                      ReglasIot reglas, @Value("${iot.desconexion.segundos:15}") int desconexionSegundos) {
        this.dispositivos = dispositivos;
        this.lecturas = lecturas;
        this.alertas = alertas;
        this.reglas = reglas;
        this.desconexionSegundos = desconexionSegundos;
    }

    // ------------------------------------------------------------------ ingesta
    @Transactional
    public LecturaResponse registrar(LecturaRequest r, Instant ahora) {
        Dispositivo d = dispositivos.findByCodigo(r.getDispositivo())
            .orElseThrow(() -> new DispositivoNoEncontradoException(
                "El dispositivo '" + r.getDispositivo() + "' no está registrado"));
        if (!d.getVariable().equalsIgnoreCase(r.getVariable())) {
            throw new DatoIotInvalidoException("La variable debe ser '" + d.getVariable() + "' para este dispositivo");
        }
        if (!d.getUnidad().equals(r.getUnidad())) {
            throw new DatoIotInvalidoException("La unidad debe ser '" + d.getUnidad() + "' para este dispositivo");
        }
        Instant fechaHora = r.getFechaHora() == null ? ahora : r.getFechaHora();
        if (fechaHora.isAfter(ahora.plus(FUTURO_MAX))) {
            throw new DatoIotInvalidoException("La fecha-hora está en el futuro");
        }
        if (fechaHora.isBefore(ahora.minus(ANTIGUEDAD_MAX))) {
            throw new DatoIotInvalidoException("La fecha-hora tiene más de 24 horas de antigüedad");
        }

        Optional<LecturaIot> previa = lecturas.findFirstByDispositivoIdAndAnomalaFalseOrderByFechaHoraDescIdDesc(d.getId());
        Double valorPrevio = previa.map(LecturaIot::getValor).orElse(null);
        ReglasIot.Evaluacion ev = reglas.evaluar(valorPrevio, r.getValor());

        LecturaIot guardada = lecturas.save(new LecturaIot(
            d.getId(), fechaHora, d.getVariable(), d.getUnidad(), r.getValor(), ev.isAnomala(), ev.getMotivo()));

        boolean reconexion = d.getConexion() == ConexionIot.DESCONECTADO;
        d.setConexion(ConexionIot.CONECTADO);
        d.setUltimaLectura(ahora);
        dispositivos.save(d);

        String u = d.getUnidad();
        if (reconexion) {
            alerta(d, guardada, TipoAlerta.RECONEXION, Severidad.INFO,
                "El dispositivo volvió a enviar lecturas", ahora);
        }
        if (ev.isAnomala()) {
            alerta(d, guardada, TipoAlerta.ANOMALIA, Severidad.ADVERTENCIA,
                "Lectura anómala (" + ev.getMotivo() + "): " + r.getValor() + " " + u + ". Se conserva marcada y no cuenta para el nivel.",
                ahora);
        } else {
            NivelIot previo = valorPrevio == null ? NivelIot.OK : reglas.nivelPara(valorPrevio);
            if (ev.getNivel() != previo) {
                alertaPorNivel(d, guardada, ev.getNivel(), r.getValor(), ahora);
            }
        }
        return new LecturaResponse(guardada, d.getCodigo());
    }

    private void alertaPorNivel(Dispositivo d, LecturaIot l, NivelIot nivel, double valor, Instant ahora) {
        String u = d.getUnidad();
        switch (nivel) {
            case CRITICO:
                alerta(d, l, TipoAlerta.CRITICA, Severidad.CRITICA,
                    "Nivel CRÍTICO: " + valor + " " + u + " (umbral " + reglas.getUmbralCritico() + " " + u + ")", ahora);
                break;
            case ADVERTENCIA:
                alerta(d, l, TipoAlerta.ADVERTENCIA, Severidad.ADVERTENCIA,
                    "Nivel de ADVERTENCIA: " + valor + " " + u + " (umbral " + reglas.getUmbralAdvertencia() + " " + u + ")", ahora);
                break;
            default:
                alerta(d, l, TipoAlerta.NORMALIZACION, Severidad.INFO,
                    "Valores normales de nuevo: " + valor + " " + u, ahora);
        }
    }

    private void alerta(Dispositivo d, LecturaIot l, TipoAlerta tipo, Severidad sev, String mensaje, Instant ahora) {
        alertas.save(new AlertaIot(d.getId(), l == null ? null : l.getId(), tipo, sev, mensaje, ahora));
    }

    // ------------------------------------------------------------------ desconexión
    /** Lo invoca el planificador: marca DESCONECTADO a quien lleva demasiado tiempo sin enviar. */
    @Transactional
    public int verificarDesconexion(Instant ahora) {
        int cambios = 0;
        for (Dispositivo d : dispositivos.findAll()) {
            Instant ultima = d.getUltimaLectura();
            if (d.getConexion() == ConexionIot.CONECTADO && ultima != null
                && ultima.isBefore(ahora.minusSeconds(desconexionSegundos))) {
                d.setConexion(ConexionIot.DESCONECTADO);
                dispositivos.save(d);
                long segundos = Duration.between(ultima, ahora).getSeconds();
                alerta(d, null, TipoAlerta.DESCONEXION, Severidad.CRITICA,
                    "Sin lecturas desde hace " + segundos + " s (límite " + desconexionSegundos + " s)", ahora);
                cambios++;
            }
        }
        return cambios;
    }

    // ------------------------------------------------------------------ consultas
    @Transactional(readOnly = true)
    public EstadoIotResponse estado(Instant ahora) {
        Dispositivo d = dispositivoUnico();
        Optional<LecturaIot> ultima = lecturas.findFirstByDispositivoIdAndAnomalaFalseOrderByFechaHoraDescIdDesc(d.getId());
        NivelIot nivel = ultima.map(l -> reglas.nivelPara(l.getValor())).orElse(NivelIot.SIN_DATOS);
        return new EstadoIotResponse(d, nivel, ultima.orElse(null), reglas, desconexionSegundos, ahora);
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> lecturas(Instant desde, Instant hasta, Integer limite, Instant ahora) {
        Instant fin = hasta == null ? ahora : hasta;
        Instant ini = desde == null ? fin.minus(Duration.ofHours(1)) : desde;
        validarRango(ini, fin);
        int tope = limite == null ? LIMITE_LECTURAS_DEFECTO : limite;
        if (tope < 1 || tope > LIMITE_LECTURAS_MAX) {
            throw new DatoIotInvalidoException("El límite debe estar entre 1 y " + LIMITE_LECTURAS_MAX);
        }
        Dispositivo d = dispositivoUnico();
        List<LecturaIot> recientes = new ArrayList<>(lecturas.findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraDesc(
            d.getId(), ini, fin, PageRequest.of(0, tope)));
        Collections.reverse(recientes); // orden cronológico para graficar
        return recientes.stream().map(l -> new LecturaResponse(l, d.getCodigo())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumenIot resumen(Instant desde, Instant hasta, Instant ahora) {
        Instant fin = hasta == null ? ahora : hasta;
        Instant ini = desde == null ? fin.minus(Duration.ofHours(1)) : desde;
        validarRango(ini, fin);
        Dispositivo d = dispositivoUnico();
        List<LecturaIot> todas = lecturas.findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraDesc(
            d.getId(), ini, fin, PageRequest.of(0, LIMITE_RESUMEN));
        List<Double> validas = todas.stream().filter(l -> !l.isAnomala()).map(LecturaIot::getValor)
            .collect(Collectors.toList());
        long anomalas = todas.size() - validas.size();
        Double min = validas.stream().min(Double::compare).orElse(null);
        Double max = validas.stream().max(Double::compare).orElse(null);
        Double prom = validas.isEmpty() ? null
            : Math.round(validas.stream().mapToDouble(Double::doubleValue).average().getAsDouble() * 100.0) / 100.0;
        return new ResumenIot(ini, fin, todas.size(), anomalas, min, max, prom);
    }

    @Transactional(readOnly = true)
    public List<AlertaIot> alertas(Integer limite) {
        int tope = limite == null ? LIMITE_ALERTAS_DEFECTO : limite;
        if (tope < 1 || tope > LIMITE_ALERTAS_MAX) {
            throw new DatoIotInvalidoException("El límite debe estar entre 1 y " + LIMITE_ALERTAS_MAX);
        }
        return alertas.findAllByOrderByFechaHoraDescIdDesc(PageRequest.of(0, tope));
    }

    // ------------------------------------------------------------------ auxiliares
    private void validarRango(Instant ini, Instant fin) {
        if (ini.isAfter(fin)) {
            throw new DatoIotInvalidoException("'desde' no puede ser posterior a 'hasta'");
        }
        if (Duration.between(ini, fin).compareTo(RANGO_MAX) > 0) {
            throw new DatoIotInvalidoException("El rango máximo de consulta es de 7 días");
        }
    }

    private Dispositivo dispositivoUnico() {
        return dispositivos.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new DispositivoNoEncontradoException("No hay dispositivos registrados"));
    }
}
