package mx.uv.proyectos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ProyectoController.class, HealthController.class})
class ProyectoControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    ProyectoService servicio;

    private static final String VALIDO =
        "{\"titulo\":\"Nuevo\",\"descripcion\":\"Desc\",\"integrantes\":\"Ana\",\"asesor\":\"Dr. X\"}";

    private Proyecto proyecto() {
        return new Proyecto("Nuevo", "Desc", "Ana", "Dr. X");
    }

    // ---------- positivas ----------
    @Test
    void saludDevuelve200() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void listaDevuelve200() throws Exception {
        when(servicio.listar(null)).thenReturn(Collections.singletonList(proyecto()));
        mvc.perform(get("/api/proyectos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titulo").value("Nuevo"))
            .andExpect(jsonPath("$[0].estado").value("REGISTRADO"));
    }

    @Test
    void consultaPorIdDevuelve200() throws Exception {
        when(servicio.obtener(1L)).thenReturn(proyecto());
        mvc.perform(get("/api/proyectos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.asesor").value("Dr. X"));
    }

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        when(servicio.crear(any(ProyectoRequest.class))).thenReturn(proyecto());
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"));
    }

    @Test
    void cambioDeEstadoDevuelve200() throws Exception {
        Proyecto p = proyecto();
        p.cambiarEstado(Estado.EN_PROGRESO);
        when(servicio.cambiarEstado(eq(1L), eq(Estado.EN_PROGRESO))).thenReturn(p);
        mvc.perform(patch("/api/proyectos/1/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PROGRESO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_PROGRESO"));
    }

    // ---------- negativas ----------
    @Test
    void crearConCamposVaciosDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"  \",\"descripcion\":\"\",\"integrantes\":\"Ana\",\"asesor\":\"Dr. X\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.titulo").exists())
            .andExpect(jsonPath("$.campos.descripcion").exists());
    }

    @Test
    void crearConTituloLargoDevuelve400() throws Exception {
        String largo = new String(new char[121]).replace('\0', 'a');
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"" + largo + "\",\"descripcion\":\"d\",\"integrantes\":\"i\",\"asesor\":\"a\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.titulo").exists());
    }

    @Test
    void crearConJsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content("{titulo:"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void crearConCampoDesconocidoDevuelve400() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"t\",\"descripcion\":\"d\",\"integrantes\":\"i\",\"asesor\":\"a\",\"x\":1}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void crearConTituloDuplicadoDevuelve409() throws Exception {
        when(servicio.crear(any(ProyectoRequest.class))).thenThrow(new TransicionInvalidaException("Ya existe"));
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isConflict());
    }

    @Test
    void consultaInexistenteDevuelve404() throws Exception {
        when(servicio.obtener(99L)).thenThrow(new ProyectoNoEncontradoException(99L));
        mvc.perform(get("/api/proyectos/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mvc.perform(get("/api/proyectos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void filtroConEstadoDesconocidoDevuelve400() throws Exception {
        mvc.perform(get("/api/proyectos").param("estado", "TERMINADO")).andExpect(status().isBadRequest());
    }

    @Test
    void estadoDesconocidoDevuelve400() throws Exception {
        mvc.perform(patch("/api/proyectos/1/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"TERMINADO\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void estadoAusenteDevuelve400() throws Exception {
        mvc.perform(patch("/api/proyectos/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.estado").exists());
    }

    @Test
    void transicionInvalidaDevuelve409() throws Exception {
        when(servicio.cambiarEstado(eq(1L), eq(Estado.ENTREGADO)))
            .thenThrow(new TransicionInvalidaException("Transición no permitida"));
        mvc.perform(patch("/api/proyectos/1/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ENTREGADO\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void cambioDeEstadoDeProyectoInexistenteDevuelve404() throws Exception {
        when(servicio.cambiarEstado(eq(99L), any())).thenThrow(new ProyectoNoEncontradoException(99L));
        mvc.perform(patch("/api/proyectos/99/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_PROGRESO\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/proyectos/1"))
            .andExpect(status().isMethodNotAllowed());
    }
}
