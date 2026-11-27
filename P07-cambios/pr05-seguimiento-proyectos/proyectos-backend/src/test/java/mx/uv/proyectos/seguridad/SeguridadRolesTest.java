package mx.uv.proyectos.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import mx.uv.proyectos.Estado;
import mx.uv.proyectos.HealthController;
import mx.uv.proyectos.Proyecto;
import mx.uv.proyectos.ProyectoController;
import mx.uv.proyectos.ProyectoRequest;
import mx.uv.proyectos.ProyectoService;

/** Autenticación y autorización por roles con la cadena de seguridad REAL (usuarios en memoria solo para la prueba). */
@WebMvcTest({ProyectoController.class, HealthController.class, AuthController.class})
@Import(SecurityConfig.class)
class SeguridadRolesTest {
    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String CLAVE_LIDER = "clave-lider-de-prueba";
    private static final String CLAVE_INTEGRANTE = "clave-integrante-de-prueba";
    private static final String NUEVO =
        "{\"titulo\":\"Nuevo\",\"descripcion\":\"Desc\",\"integrantes\":\"Ana\",\"asesor\":\"Dr. X\"}";

    @TestConfiguration
    static class UsuariosDePrueba {
        @Bean
        UserDetailsService usuariosDePrueba(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                User.withUsername("lider").password(encoder.encode(CLAVE_LIDER)).roles("LIDER").build(),
                User.withUsername("integrante").password(encoder.encode(CLAVE_INTEGRANTE)).roles("INTEGRANTE").build());
        }
    }

    @Autowired MockMvc mvc;
    @MockBean ProyectoService servicio;

    private Proyecto proyecto() {
        return new Proyecto("Nuevo", "Desc", "Ana", "Dr. X");
    }

    // ---------------- positivas
    @Test
    void liderAutenticadoConsultaProyectos() throws Exception {
        mvc.perform(get("/api/proyectos").with(httpBasic("lider", CLAVE_LIDER))).andExpect(status().isOk());
    }

    @Test
    void integranteAutenticadoConsultaProyectos() throws Exception {
        mvc.perform(get("/api/proyectos").with(httpBasic("integrante", CLAVE_INTEGRANTE))).andExpect(status().isOk());
    }

    @Test
    void liderCambiaElEstadoDeUnProyecto() throws Exception {
        Proyecto p = proyecto();
        p.cambiarEstado(Estado.EN_PROGRESO);
        when(servicio.cambiarEstado(eq(1L), eq(Estado.EN_PROGRESO))).thenReturn(p);
        mvc.perform(patch("/api/proyectos/1/estado").with(httpBasic("lider", CLAVE_LIDER))
                .contentType(JSON).content("{\"estado\":\"EN_PROGRESO\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void liderCreaUnProyecto() throws Exception {
        when(servicio.crear(any(ProyectoRequest.class))).thenReturn(proyecto());
        mvc.perform(post("/api/proyectos").with(httpBasic("lider", CLAVE_LIDER)).contentType(JSON).content(NUEVO))
            .andExpect(status().isCreated());
    }

    @Test
    void saludEsPublicaYLlevaCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void authYoDevuelveUsuarioYRol() throws Exception {
        mvc.perform(get("/api/auth/yo").with(httpBasic("integrante", CLAVE_INTEGRANTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario").value("integrante"))
            .andExpect(jsonPath("$.rol").value("INTEGRANTE"));
    }

    // ---------------- negativas
    @Test
    void sinCredencialesDevuelve401SinDialogoDelNavegador() throws Exception {
        mvc.perform(get("/api/proyectos"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void contrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(get("/api/proyectos").with(httpBasic("lider", "contrasena-equivocada")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInexistenteDevuelve401IgualQueContrasenaIncorrecta() throws Exception {
        mvc.perform(get("/api/proyectos").with(httpBasic("fantasma", "cualquier-cosa-123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensaje").value("Autenticación requerida o credenciales inválidas"));
    }

    @Test
    void integranteNoPuedeCrearProyectos() throws Exception {
        mvc.perform(post("/api/proyectos").with(httpBasic("integrante", CLAVE_INTEGRANTE)).contentType(JSON).content(NUEVO))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void integranteNoPuedeCambiarEstados() throws Exception {
        mvc.perform(patch("/api/proyectos/1/estado").with(httpBasic("integrante", CLAVE_INTEGRANTE))
                .contentType(JSON).content("{\"estado\":\"EN_PROGRESO\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNoPuedeEscribir() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(JSON).content(NUEVO)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/proyectos/1/estado").contentType(JSON).content("{\"estado\":\"EN_PROGRESO\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rutasNoDeclaradasSeDenieganPorDefecto() throws Exception {
        mvc.perform(delete("/api/proyectos/1").with(httpBasic("lider", CLAVE_LIDER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/secreto").with(httpBasic("lider", CLAVE_LIDER))).andExpect(status().isForbidden());
    }

    @Test
    void laFuerzaBrutaSeFrenaConUn429SinAfectarAOtrosUsuarios() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/proyectos").with(httpBasic("atacante", "intento-" + i))).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/proyectos").with(httpBasic("atacante", "intento-final")))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
        // un usuario legítimo no se ve afectado
        mvc.perform(get("/api/proyectos").with(httpBasic("lider", CLAVE_LIDER))).andExpect(status().isOk());
    }
}
