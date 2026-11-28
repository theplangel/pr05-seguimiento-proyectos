package mx.uv.proyectos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioInicializadorTest {
    private static final String CLAVE_A = "clave-de-prueba-larga-1";
    private static final String CLAVE_B = "clave-de-prueba-larga-2";

    @Mock UsuarioRepository usuarios;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private UsuarioInicializador con(String claveLider, String claveIntegrante) {
        return new UsuarioInicializador(usuarios, encoder, "lider", claveLider, "integrante", claveIntegrante);
    }

    @Test
    void creaLosUsuariosConHashBcryptYSuRol() {
        when(usuarios.existsByUsername(anyString())).thenReturn(false);
        con(CLAVE_A, CLAVE_B).run(null);
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(2)).save(captor.capture());
        List<Usuario> creados = captor.getAllValues();
        assertThat(creados.get(0).getRol()).isEqualTo(Rol.LIDER);
        assertThat(creados.get(1).getRol()).isEqualTo(Rol.INTEGRANTE);
        assertThat(creados.get(0).getPasswordHash()).isNotEqualTo(CLAVE_A).startsWith("$2");
        assertThat(encoder.matches(CLAVE_A, creados.get(0).getPasswordHash())).isTrue();
        assertThat(encoder.matches(CLAVE_B, creados.get(1).getPasswordHash())).isTrue();
    }

    @Test
    void noRecreaNiExigeClaveSiLosUsuariosYaExisten() {
        when(usuarios.existsByUsername(anyString())).thenReturn(true);
        con("", "").run(null);
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaUnaClaveCorta() {
        when(usuarios.existsByUsername(anyString())).thenReturn(false);
        assertThatThrownBy(() -> con("corta", CLAVE_B).run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_LIDER_PASSWORD");
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaUnaClaveVaciaONula() {
        when(usuarios.existsByUsername(anyString())).thenReturn(false);
        assertThatThrownBy(() -> con(CLAVE_A, "").run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APP_INTEGRANTE_PASSWORD");
        assertThatThrownBy(() -> con(null, CLAVE_B).run(null)).isInstanceOf(IllegalStateException.class);
    }
}
