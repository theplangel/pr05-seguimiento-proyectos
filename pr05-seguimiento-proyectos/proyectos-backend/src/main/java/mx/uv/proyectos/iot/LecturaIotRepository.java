package mx.uv.proyectos.iot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaIotRepository extends JpaRepository<LecturaIot, Long> {
    /** Última lectura no anómala: referencia para el nivel actual y para detectar saltos bruscos. */
    Optional<LecturaIot> findFirstByDispositivoIdAndAnomalaFalseOrderByFechaHoraDescIdDesc(Long dispositivoId);

    /** Las más recientes del rango (el servicio las invierte para entregarlas en orden cronológico). */
    List<LecturaIot> findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraDesc(
        Long dispositivoId, Instant desde, Instant hasta, Pageable pagina);
}
