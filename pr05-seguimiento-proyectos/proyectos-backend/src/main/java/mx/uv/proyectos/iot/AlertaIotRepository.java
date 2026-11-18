package mx.uv.proyectos.iot;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaIotRepository extends JpaRepository<AlertaIot, Long> {
    List<AlertaIot> findAllByOrderByFechaHoraDescIdDesc(Pageable pagina);
}
