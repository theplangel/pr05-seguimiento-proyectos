package mx.uv.proyectos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    List<Proyecto> findAllByOrderByIdAsc();

    List<Proyecto> findByEstadoOrderByIdAsc(Estado estado);

    boolean existsByTituloIgnoreCase(String titulo);
}
