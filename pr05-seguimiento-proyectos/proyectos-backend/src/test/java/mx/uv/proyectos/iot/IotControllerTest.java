package mx.uv.proyectos.iot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import mx.uv.proyectos.seguridad.SecurityConfig;

@WebMvcTest(IotController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "INTEGRANTE")
@TestPropertySource(properties = "iot.api-key=clave-solo-para-pruebas")
class IotControllerTest {
    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String CUERPO =
        "{\"dispositivo\":\"sensor-lab-01\",\"fechaHora\":\"2026-11-10T18:00:00Z\","
            + "\"variable\":\"temperatura\",\"unidad\":\"°C\",\"valor\":25.4}";

    @Autowired MockMvc mvc;
    @MockBean IotService servicio;
    @Value("${iot.api-key}") String clave;

    private LecturaResponse respuesta(boolean anomala) {
        return new LecturaResponse(new LecturaIot(1L, Instant.parse("2026-11-10T18:00:00Z"), "temperatura", "°C",
            25.4, anomala, anomala ? "FUERA_DE_RANGO" : null), "sensor-lab-01");
    }

    // ---------- autenticación
    @Test
    void ingestaSinApiKeyDevuelve401() throws Exception {
        mvc.perform(post("/api/iot/lecturas").contentType(JSON).content(CUERPO))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void ingestaConApiKeyIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", "incorrecta").contentType(JSON).content(CUERPO))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void autenticacionPrecedeALaValidacionDelCuerpo() throws Exception {
        mvc.perform(post("/api/iot/lecturas").contentType(JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    // ---------- positivas
    @Test
    void ingestaValidaDevuelve201() throws Exception {
        when(servicio.registrar(any(LecturaRequest.class), any(Instant.class))).thenReturn(respuesta(false));
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON).content(CUERPO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dispositivo").value("sensor-lab-01"))
            .andExpect(jsonPath("$.anomala").value(false));
    }

    @Test
    void ingestaAnomalaSeAceptaYSeMarca() throws Exception {
        when(servicio.registrar(any(LecturaRequest.class), any(Instant.class))).thenReturn(respuesta(true));
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON).content(CUERPO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.motivoAnomalia").value("FUERA_DE_RANGO"));
    }

    @Test
    void historialDevuelve200() throws Exception {
        when(servicio.lecturas(any(), any(), any(), any(Instant.class))).thenReturn(Collections.singletonList(respuesta(false)));
        mvc.perform(get("/api/iot/lecturas").param("desde", "2026-11-10T17:00:00.000Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].valor").value(25.4));
    }

    @Test
    void alertasDevuelve200() throws Exception {
        when(servicio.alertas(any())).thenReturn(Collections.emptyList());
        mvc.perform(get("/api/iot/alertas")).andExpect(status().isOk());
    }

    // ---------- negativas
    @Test
    void cuerpoSinCamposDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.dispositivo").exists())
            .andExpect(jsonPath("$.campos.valor").exists());
    }

    @Test
    void valorNoNumericoDevuelve400() throws Exception {
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON)
                .content("{\"dispositivo\":\"s\",\"variable\":\"temperatura\",\"unidad\":\"°C\",\"valor\":\"abc\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void fechaMalFormadaDevuelve400() throws Exception {
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON)
                .content("{\"dispositivo\":\"s\",\"fechaHora\":\"ayer\",\"variable\":\"temperatura\",\"unidad\":\"°C\",\"valor\":1}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void dispositivoNoRegistradoDevuelve404() throws Exception {
        when(servicio.registrar(any(LecturaRequest.class), any(Instant.class)))
            .thenThrow(new DispositivoNoEncontradoException("no registrado"));
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON).content(CUERPO))
            .andExpect(status().isNotFound());
    }

    @Test
    void datoRechazadoPorElServicioDevuelve400() throws Exception {
        when(servicio.registrar(any(LecturaRequest.class), any(Instant.class)))
            .thenThrow(new DatoIotInvalidoException("La unidad debe ser '°C'"));
        mvc.perform(post("/api/iot/lecturas").header("X-API-Key", clave).contentType(JSON).content(CUERPO))
            .andExpect(status().isBadRequest());
    }

    @Test
    void parametroDeFechaInvalidoDevuelve400() throws Exception {
        mvc.perform(get("/api/iot/lecturas").param("desde", "no-es-fecha")).andExpect(status().isBadRequest());
    }

    @Test
    void rangoInvalidoRechazadoPorElServicioDevuelve400() throws Exception {
        when(servicio.lecturas(any(), any(), any(), any(Instant.class))).thenThrow(new DatoIotInvalidoException("rango"));
        mvc.perform(get("/api/iot/lecturas")).andExpect(status().isBadRequest());
    }

    @Test
    void metodoNoDeclaradoEnEstadoSeDeniega() throws Exception {
        mvc.perform(post("/api/iot/estado").header("X-API-Key", clave)).andExpect(status().isForbidden());
    }
}
