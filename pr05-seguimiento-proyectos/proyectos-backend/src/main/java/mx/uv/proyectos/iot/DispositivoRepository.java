package mx.uv.proyectos.iot;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    Optional<Dispositivo> findByCodigo(String codigo);

    Optional<Dispositivo> findFirstByOrderByIdAsc();
}
