package mx.uv.proyectos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimitadorIntentosFilterTest {
    private final long[] ahora = {0L};
    private final LimitadorIntentosFilter filtro = new LimitadorIntentosFilter(3, 60_000, () -> ahora[0]);

    /** Simula una petición con credenciales Basic cuyo resultado (status) decide el "backend". */
    private int peticion(String usuario, int statusDelBackend) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/proyectos");
        req.addHeader("Authorization", "Basic "
            + Base64.getEncoder().encodeToString((usuario + ":x").getBytes(StandardCharsets.UTF_8)));
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, (rq, rs) -> ((HttpServletResponse) rs).setStatus(statusDelBackend));
        return res.getStatus();
    }

    @Test
    void bloqueaTrasElMaximoDeFallos() throws Exception {
        assertThat(peticion("ana", 401)).isEqualTo(401);
        assertThat(peticion("ana", 401)).isEqualTo(401);
        assertThat(peticion("ana", 401)).isEqualTo(401);
        assertThat(peticion("ana", 200)).isEqualTo(429); // ni siquiera llega al backend
    }

    @Test
    void unAccesoCorrectoReiniciaElContador() throws Exception {
        peticion("ana", 401);
        peticion("ana", 401);
        assertThat(peticion("ana", 200)).isEqualTo(200);
        peticion("ana", 401);
        peticion("ana", 401);
        assertThat(peticion("ana", 200)).isEqualTo(200); // solo 2 fallos desde el reinicio
    }

    @Test
    void elBloqueoExpiraAlPasarLaVentana() throws Exception {
        for (int i = 0; i < 3; i++) {
            peticion("ana", 401);
        }
        assertThat(peticion("ana", 200)).isEqualTo(429);
        ahora[0] += 61_000;
        assertThat(peticion("ana", 200)).isEqualTo(200);
    }

    @Test
    void losUsuariosSonIndependientesYNoDistinguenMayusculas() throws Exception {
        for (int i = 0; i < 3; i++) {
            peticion("Ana", 401);
        }
        assertThat(peticion("ANA", 200)).isEqualTo(429);
        assertThat(peticion("beto", 200)).isEqualTo(200);
    }

    @Test
    void sinCabeceraBasicNoSeCuentaNiSeBloquea() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(new MockHttpServletRequest("GET", "/api/proyectos"), res,
                (rq, rs) -> ((HttpServletResponse) rs).setStatus(401));
            assertThat(res.getStatus()).isEqualTo(401);
        }
    }

    @Test
    void laRespuestaDeBloqueoIncluyeRetryAfterYJson() throws Exception {
        for (int i = 0; i < 3; i++) {
            peticion("ana", 401);
        }
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/proyectos");
        req.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("ana:x".getBytes(StandardCharsets.UTF_8)));
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, (rq, rs) -> { });
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
        assertThat(res.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void laLimpiezaParaLogsQuitaSaltosDeLineaYLimitaLaLongitud() {
        assertThat(LimitadorIntentosFilter.limpiar("a\nb\r\nc")).doesNotContain("\n").doesNotContain("\r");
        assertThat(LimitadorIntentosFilter.limpiar("x".repeat(100))).hasSize(40);
        assertThat(LimitadorIntentosFilter.limpiar(null)).isEmpty();
    }
}
