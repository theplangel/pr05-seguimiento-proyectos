package mx.uv.proyectos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioDetallesServiceTest {
    @Mock UsuarioRepository usuarios;
    @InjectMocks UsuarioDetallesService servicio;

    @Test
    void cargaElUsuarioConSuRolYSuHash() {
        when(usuarios.findByUsername("lider")).thenReturn(Optional.of(new Usuario("lider", "$2a$hash", Rol.LIDER)));
        UserDetails u = servicio.loadUserByUsername("lider");
        assertThat(u.getPassword()).isEqualTo("$2a$hash");
        assertThat(u.getAuthorities()).extracting("authority").containsExactly("ROLE_LIDER");
        assertThat(u.isEnabled()).isTrue();
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIniciarSesion() {
        Usuario baja = new Usuario("ana", "$2a$hash", Rol.INTEGRANTE);
        baja.setActivo(false);
        when(usuarios.findByUsername("ana")).thenReturn(Optional.of(baja));
        assertThat(servicio.loadUserByUsername("ana").isEnabled()).isFalse();
    }

    @Test
    void unUsuarioInexistenteNoRevelaDetalles() {
        when(usuarios.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.loadUserByUsername("nadie"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Credenciales inválidas");
    }
}
