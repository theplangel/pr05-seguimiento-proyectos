package mx.uv.proyectos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Salud")
public class HealthController {
    @GetMapping
    @Operation(summary = "Indica que el servicio está activo")
    public Map<String, Object> salud() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", "UP");
        cuerpo.put("servicio", "proyectos-api");
        cuerpo.put("timestamp", Instant.now().toString());
        return cuerpo;
    }
}
