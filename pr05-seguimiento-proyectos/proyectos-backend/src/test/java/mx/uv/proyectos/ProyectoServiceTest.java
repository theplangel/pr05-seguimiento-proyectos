package mx.uv.proyectos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceTest {
    @Mock
    ProyectoRepository repositorio;
    @InjectMocks
    ProyectoService servicio;

    private Proyecto proyecto() {
        return new Proyecto("Título", "Desc", "Ana", "Dr. X");
    }

    @Test
    void creaProyectoEnEstadoRegistrado() {
        when(repositorio.existsByTituloIgnoreCase("Título")).thenReturn(false);
        when(repositorio.save(any(Proyecto.class))).thenAnswer(i -> i.getArgument(0));
        Proyecto creado = servicio.crear(new ProyectoRequest("Título", "Desc", "Ana", "Dr. X"));
        assertThat(creado.getEstado()).isEqualTo(Estado.REGISTRADO);
    }

    @Test
    void rechazaTituloDuplicado() {
        when(repositorio.existsByTituloIgnoreCase("Título")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new ProyectoRequest("Título", "Desc", "Ana", "Dr. X")))
            .isInstanceOf(TransicionInvalidaException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void cambiaEstadoCuandoLaTransicionEsValida() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(proyecto()));
        when(repositorio.save(any(Proyecto.class))).thenAnswer(i -> i.getArgument(0));
        assertThat(servicio.cambiarEstado(1L, Estado.EN_PROGRESO).getEstado()).isEqualTo(Estado.EN_PROGRESO);
    }

    @Test
    void rechazaTransicionInvalida() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(proyecto()));
        assertThatThrownBy(() -> servicio.cambiarEstado(1L, Estado.ENTREGADO))
            .isInstanceOf(TransicionInvalidaException.class)
            .hasMessageContaining("REGISTRADO -> ENTREGADO");
        verify(repositorio, never()).save(any());
    }

    @Test
    void lanzaNoEncontradoSiNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(ProyectoNoEncontradoException.class);
    }

    @Test
    void listarSinFiltroUsaElOrdenPorId() {
        when(repositorio.findAllByOrderByIdAsc()).thenReturn(java.util.Collections.singletonList(proyecto()));
        assertThat(servicio.listar(null)).hasSize(1);
        verify(repositorio, never()).findByEstadoOrderByIdAsc(any());
    }

    @Test
    void listarConFiltroConsultaPorEstado() {
        when(repositorio.findByEstadoOrderByIdAsc(Estado.ENTREGADO)).thenReturn(java.util.Collections.emptyList());
        assertThat(servicio.listar(Estado.ENTREGADO)).isEmpty();
    }
}
