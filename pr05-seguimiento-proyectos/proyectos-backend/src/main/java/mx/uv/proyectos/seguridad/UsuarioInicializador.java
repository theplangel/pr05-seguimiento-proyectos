package mx.uv.proyectos.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea los usuarios iniciales (solo si no existen) con contraseñas que vienen del entorno.
 * No hay contraseñas por defecto: si falta una (o tiene menos de 12 caracteres) la aplicación NO arranca.
 */
@Component
public class UsuarioInicializador implements ApplicationRunner {
    static final int LONGITUD_MINIMA = 12;
    private static final Logger log = LoggerFactory.getLogger(UsuarioInicializador.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String liderNombre;
    private final String liderClave;
    private final String integranteNombre;
    private final String integranteClave;

    public UsuarioInicializador(UsuarioRepository usuarios, PasswordEncoder encoder,
                                @Value("${app.usuarios.lider.nombre:lider}") String liderNombre,
                                @Value("${app.usuarios.lider.password:}") String liderClave,
                                @Value("${app.usuarios.integrante.nombre:integrante}") String integranteNombre,
                                @Value("${app.usuarios.integrante.password:}") String integranteClave) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.liderNombre = liderNombre;
        this.liderClave = liderClave;
        this.integranteNombre = integranteNombre;
        this.integranteClave = integranteClave;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearSiNoExiste(liderNombre, liderClave, Rol.LIDER, "APP_LIDER_PASSWORD");
        crearSiNoExiste(integranteNombre, integranteClave, Rol.INTEGRANTE, "APP_INTEGRANTE_PASSWORD");
    }

    void crearSiNoExiste(String nombre, String clave, Rol rol, String variable) {
        if (usuarios.existsByUsername(nombre)) {
            return;
        }
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException("Defina " + variable + " con al menos " + LONGITUD_MINIMA
                + " caracteres para crear el usuario '" + nombre + "'");
        }
        usuarios.save(new Usuario(nombre, encoder.encode(clave), rol));
        log.info("Usuario inicial '{}' creado con rol {}", nombre, rol);
    }
}
