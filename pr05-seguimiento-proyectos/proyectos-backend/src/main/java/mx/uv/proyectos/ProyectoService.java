package mx.uv.proyectos;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectoService {
    private final ProyectoRepository repositorio;

    public ProyectoService(ProyectoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Proyecto> listar(Estado estado) {
        return estado == null ? repositorio.findAllByOrderByIdAsc() : repositorio.findByEstadoOrderByIdAsc(estado);
    }

    @Transactional(readOnly = true)
    public Proyecto obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ProyectoNoEncontradoException(id));
    }

    @Transactional
    public Proyecto crear(ProyectoRequest r) {
        if (repositorio.existsByTituloIgnoreCase(r.getTitulo())) {
            throw new TransicionInvalidaException("Ya existe un proyecto con el título '" + r.getTitulo() + "'");
        }
        return repositorio.save(new Proyecto(r.getTitulo(), r.getDescripcion(), r.getIntegrantes(), r.getAsesor()));
    }

    @Transactional
    public Proyecto cambiarEstado(Long id, Estado nuevo) {
        Proyecto proyecto = obtener(id);
        Estado actual = proyecto.getEstado();
        if (!actual.puedePasarA(nuevo)) {
            throw new TransicionInvalidaException(
                "Transición no permitida: " + actual + " -> " + nuevo + ". Permitidas: " + actual.siguientes());
        }
        proyecto.cambiarEstado(nuevo);
        return repositorio.save(proyecto);
    }
}
