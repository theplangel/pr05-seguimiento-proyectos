package mx.uv.proyectos;

public class ProyectoNoEncontradoException extends RuntimeException {
    public ProyectoNoEncontradoException(Long id) {
        super("No existe el proyecto con id " + id);
    }
}
