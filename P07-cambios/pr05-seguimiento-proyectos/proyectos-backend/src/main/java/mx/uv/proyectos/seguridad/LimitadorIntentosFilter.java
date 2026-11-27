package mx.uv.proyectos.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Freno a la fuerza bruta: tras N respuestas 401 para el mismo usuario dentro de una ventana de tiempo,
 * responde 429 sin siquiera evaluar la contraseña. Va ANTES de BasicAuthenticationFilter y no es un bean
 * (se instancia en SecurityConfig) para que no se registre dos veces en la cadena de servlets.
 */
public class LimitadorIntentosFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(LimitadorIntentosFilter.class);
    private static final int MAX_CLAVES = 5000;

    private final int maxFallos;
    private final long ventanaMs;
    private final LongSupplier reloj;
    private final Map<String, Deque<Long>> fallos = new ConcurrentHashMap<>();

    public LimitadorIntentosFilter(int maxFallos, long ventanaMs) {
        this(maxFallos, ventanaMs, System::currentTimeMillis);
    }

    public LimitadorIntentosFilter(int maxFallos, long ventanaMs, LongSupplier reloj) {
        this.maxFallos = maxFallos;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth == null || !auth.regionMatches(true, 0, "Basic ", 0, 6);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String clave = claveDe(request);
        if (bloqueado(clave)) {
            log.warn("Acceso bloqueado temporalmente por intentos fallidos: usuario='{}' ip='{}'",
                limpiar(clave), limpiar(request.getRemoteAddr()));
            response.setHeader("Retry-After", String.valueOf(Math.max(1, ventanaMs / 1000)));
            RespuestasSeguridad.escribir(request, response, 429, "Too Many Requests",
                "Demasiados intentos fallidos. Espere un momento e intente de nuevo");
            return;
        }
        chain.doFilter(request, response);
        int status = response.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            registrarFallo(clave);
            log.warn("Autenticación fallida: usuario='{}' ip='{}'", limpiar(clave), limpiar(request.getRemoteAddr()));
        } else if (status < 400) {
            fallos.remove(clave);
        }
    }

    boolean bloqueado(String clave) {
        Deque<Long> d = fallos.get(clave);
        if (d == null) {
            return false;
        }
        synchronized (d) {
            purgar(d);
            return d.size() >= maxFallos;
        }
    }

    private void registrarFallo(String clave) {
        if (fallos.size() > MAX_CLAVES) { // evita crecimiento sin límite con nombres inventados
            fallos.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    purgar(e.getValue());
                    return e.getValue().isEmpty();
                }
            });
        }
        Deque<Long> d = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) {
            purgar(d);
            d.addLast(reloj.getAsLong());
        }
    }

    private void purgar(Deque<Long> d) {
        long limite = reloj.getAsLong() - ventanaMs;
        while (!d.isEmpty() && d.peekFirst() < limite) {
            d.pollFirst();
        }
    }

    private static String claveDe(HttpServletRequest request) {
        try {
            String cabecera = request.getHeader("Authorization");
            String decodificado = new String(Base64.getDecoder().decode(cabecera.substring(6).trim()),
                StandardCharsets.UTF_8);
            int i = decodificado.indexOf(':');
            String usuario = (i < 0 ? decodificado : decodificado.substring(0, i)).trim().toLowerCase();
            return usuario.isEmpty() ? "ip:" + request.getRemoteAddr() : "u:" + usuario;
        } catch (IllegalArgumentException e) {
            return "ip:" + request.getRemoteAddr();
        }
    }

    /** Evita inyección en logs (saltos de línea, caracteres de control) y limita la longitud. */
    static String limpiar(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("[^\\p{Print}]", "_");
        return t.length() > 40 ? t.substring(0, 40) : t;
    }
}
