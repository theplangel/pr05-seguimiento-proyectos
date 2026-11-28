package mx.uv.proyectos.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {
    @GetMapping("/yo")
    @Operation(summary = "Valida las credenciales Basic y devuelve usuario y rol")
    public Map<String, String> yo(Authentication autenticacion) {
        String rol = autenticacion.getAuthorities().stream().findFirst()
            .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse("");
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("usuario", autenticacion.getName());
        cuerpo.put("rol", rol);
        return cuerpo;
    }
}
