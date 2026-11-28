package mx.uv.proyectos.iot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT", description = "Fuente IoT simulada: ingesta, estado, historial y alertas (P06)")
public class IotController {
    private final IotService servicio;

    public IotController(IotService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/lecturas")
    @Operation(summary = "Recibe una lectura del dispositivo (requiere cabecera X-API-Key)")
    @ApiResponse(responseCode = "201", description = "Lectura registrada (puede quedar marcada como anómala)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "API key ausente o inválida")
    @ApiResponse(responseCode = "404", description = "Dispositivo no registrado")
    public ResponseEntity<LecturaResponse> registrar(@Valid @RequestBody LecturaRequest request) {
        return ResponseEntity.status(201).body(servicio.registrar(request, Instant.now()));
    }

    @GetMapping("/estado")
    @Operation(summary = "Estado actual: valor, nivel (OK/ADVERTENCIA/CRITICO) y conexión")
    public EstadoIotResponse estado() {
        return servicio.estado(Instant.now());
    }

    @GetMapping("/lecturas")
    @Operation(summary = "Historial en orden cronológico (por defecto, la última hora)")
    @ApiResponse(responseCode = "400", description = "Rango o límite inválidos")
    public List<LecturaResponse> lecturas(
        @RequestParam(required = false) Instant desde,
        @RequestParam(required = false) Instant hasta,
        @RequestParam(required = false) Integer limite) {
        return servicio.lecturas(desde, hasta, limite, Instant.now());
    }

    @GetMapping("/resumen")
    @Operation(summary = "Mínimo, máximo, promedio y anomalías del rango")
    public ResumenIot resumen(
        @RequestParam(required = false) Instant desde,
        @RequestParam(required = false) Instant hasta) {
        return servicio.resumen(desde, hasta, Instant.now());
    }

    @GetMapping("/alertas")
    @Operation(summary = "Alertas y eventos recientes (más nuevas primero)")
    public List<AlertaIot> alertas(@RequestParam(required = false) Integer limite) {
        return servicio.alertas(limite);
    }
}
