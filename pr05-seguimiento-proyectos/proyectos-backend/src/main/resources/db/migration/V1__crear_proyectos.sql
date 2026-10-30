CREATE TABLE proyectos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    integrantes VARCHAR(250) NOT NULL,
    asesor VARCHAR(120) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADO',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT proyectos_titulo_unico UNIQUE (titulo),
    CONSTRAINT titulo_no_vacio CHECK (length(trim(titulo)) > 0),
    CONSTRAINT descripcion_no_vacia CHECK (length(trim(descripcion)) > 0),
    CONSTRAINT integrantes_no_vacio CHECK (length(trim(integrantes)) > 0),
    CONSTRAINT asesor_no_vacio CHECK (length(trim(asesor)) > 0),
    CONSTRAINT estado_valido CHECK (estado IN ('REGISTRADO','EN_PROGRESO','EN_REVISION','ENTREGADO'))
);
