package mx.uv.proyectos.iot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class IotConfig {
    @Bean
    public ReglasIot reglasIot(@Value("${iot.umbral.advertencia:30}") double advertencia,
                               @Value("${iot.umbral.critico:35}") double critico,
                               @Value("${iot.rango.min:-10}") double rangoMin,
                               @Value("${iot.rango.max:80}") double rangoMax,
                               @Value("${iot.salto.max:8}") double saltoMax) {
        return new ReglasIot(advertencia, critico, rangoMin, rangoMax, saltoMax);
    }
}
