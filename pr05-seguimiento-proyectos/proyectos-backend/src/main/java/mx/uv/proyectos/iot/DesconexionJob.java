package mx.uv.proyectos.iot;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisión periódica de desconexión (la ausencia de datos no genera ninguna petición que la detecte). */
@Component
public class DesconexionJob {
    private final IotService servicio;

    public DesconexionJob(IotService servicio) {
        this.servicio = servicio;
    }

    @Scheduled(fixedDelayString = "${iot.desconexion.revision-ms:5000}")
    public void revisar() {
        servicio.verificarDesconexion(Instant.now());
    }
}
