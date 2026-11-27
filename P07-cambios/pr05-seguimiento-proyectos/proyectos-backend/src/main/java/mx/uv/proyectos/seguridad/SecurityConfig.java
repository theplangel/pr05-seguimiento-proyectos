package mx.uv.proyectos.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Autenticación HTTP Basic sin estado (sin cookies, por eso CSRF no aplica) y autorización por roles.
 * Política: todo lo no listado se deniega.
 *   - Salud y ficha de Swagger (desactivado por defecto): públicos.
 *   - POST /api/iot/lecturas: lo autentica ApiKeyFilter (clave del dispositivo), no un usuario.
 *   - GET: LIDER o INTEGRANTE.   POST/PATCH de proyectos: solo LIDER.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${seguridad.login.max-fallos:5}") int maxFallos,
                                           @Value("${seguridad.login.ventana-segundos:60}") int ventanaSegundos)
        throws Exception {
        AuthenticationEntryPoint sinCredenciales = (req, res, ex) ->
            RespuestasSeguridad.escribir(req, res, 401, "Unauthorized", "Autenticación requerida o credenciales inválidas");
        AccessDeniedHandler sinPermiso = (req, res, ex) ->
            RespuestasSeguridad.escribir(req, res, 403, "Forbidden", "No tiene permiso para esta operación");

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.authenticationEntryPoint(sinCredenciales)) // sin WWW-Authenticate: el navegador no abre su diálogo
            .exceptionHandling(e -> e.authenticationEntryPoint(sinCredenciales).accessDeniedHandler(sinPermiso))
            .headers(h -> {
                h.frameOptions(f -> f.deny());
                h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER));
            })
            .authorizeRequests(a -> a
                .antMatchers(HttpMethod.GET, "/api/health", "/actuator/health", "/actuator/health/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/iot/lecturas").permitAll()
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/proyectos").hasRole("LIDER")
                .antMatchers(HttpMethod.PATCH, "/api/proyectos/*/estado").hasRole("LIDER")
                .antMatchers(HttpMethod.GET, "/api/proyectos", "/api/proyectos/**", "/api/iot/**", "/api/auth/yo")
                    .hasAnyRole("LIDER", "INTEGRANTE")
                .anyRequest().denyAll())
            .addFilterBefore(new LimitadorIntentosFilter(maxFallos, ventanaSegundos * 1000L), BasicAuthenticationFilter.class);
        return http.build();
    }
}
