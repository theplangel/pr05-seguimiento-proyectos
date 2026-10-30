package mx.uv.proyectos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Estados del seguimiento de un proyecto y transiciones permitidas.
 * REGISTRADO -> EN_PROGRESO -> EN_REVISION -> ENTREGADO (EN_REVISION puede regresar a EN_PROGRESO).
 */
public enum Estado {
    REGISTRADO, EN_PROGRESO, EN_REVISION, ENTREGADO;

    public List<Estado> siguientes() {
        switch (this) {
            case REGISTRADO:
                return Collections.singletonList(EN_PROGRESO);
            case EN_PROGRESO:
                return Collections.singletonList(EN_REVISION);
            case EN_REVISION:
                return Arrays.asList(EN_PROGRESO, ENTREGADO);
            default:
                return Collections.emptyList();
        }
    }

    public boolean puedePasarA(Estado destino) {
        return siguientes().contains(destino);
    }
}
