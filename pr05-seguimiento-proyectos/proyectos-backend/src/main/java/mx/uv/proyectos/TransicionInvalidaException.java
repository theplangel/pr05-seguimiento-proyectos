package mx.uv.proyectos;

/** Se lanza cuando el cambio de estado o el título entran en conflicto con el estado actual (HTTP 409). */
public class TransicionInvalidaException extends RuntimeException {
    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
