package mx.uv.proyectos.iot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica la ingesta del dispositivo con la cabecera X-API-Key ANTES de leer o validar el cuerpo.
 * Solo protege POST /api/iot/lecturas; la consulta del dashboard es de solo lectura.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    public static final String CABECERA = "X-API-Key";
    public static final String RUTA_INGESTA = "/api/iot/lecturas";

    private final byte[] esperada;

    public ApiKeyFilter(@Value("${iot.api-key:}") String apiKey) {
        this.esperada = apiKey.getBytes(StandardCharsets.UTF_8);
        if (this.esperada.length == 0) {
            logger.warn("IOT_API_KEY no está definida: la ingesta IoT rechazará todas las lecturas");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && RUTA_INGESTA.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String recibida = request.getHeader(CABECERA);
        // Falla cerrado: sin clave configurada (IOT_API_KEY vacía) NADIE puede ingerir datos.
        // Comparación en tiempo constante para no filtrar información por temporización.
        if (esperada.length == 0 || recibida == null
            || !MessageDigest.isEqual(recibida.getBytes(StandardCharsets.UTF_8), esperada)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\","
                + "\"mensaje\":\"API key ausente o inválida\",\"ruta\":\"" + RUTA_INGESTA + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
