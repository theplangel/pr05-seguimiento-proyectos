package mx.uv.proyectos.seguridad;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carga el usuario desde PostgreSQL. El hash BCrypt nunca sale de este servicio hacia la API. */
@Service
public class UsuarioDetallesService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public UsuarioDetallesService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario u = usuarios.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(u.getUsername())
            .password(u.getPasswordHash())
            .roles(u.getRol().name())
            .disabled(!u.isActivo())
            .build();
    }
}
