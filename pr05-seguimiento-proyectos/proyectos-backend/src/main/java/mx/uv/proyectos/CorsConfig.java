package mx.uv.proyectos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS solo para ejecución local sin proxy (ng serve en :4200 llamando a :8080).
 * Con Docker Compose el frontend usa el proxy de nginx y no depende de esto.
 * Orígenes configurables con APP_CORS_ORIGINS (separados por coma).
 */
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${app.cors.origins:http://localhost:4200}") String[] origenes) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(origenes)
                    .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                    .allowedHeaders("Content-Type");
            }
        };
    }
}
