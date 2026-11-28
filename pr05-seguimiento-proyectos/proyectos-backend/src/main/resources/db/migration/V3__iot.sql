-- P06: una fuente IoT simulada (sensor de temperatura) con historial y alertas. Horas guardadas en UTC.
CREATE TABLE dispositivo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    variable VARCHAR(40) NOT NULL,
    unidad VARCHAR(15) NOT NULL,
    proyecto_id BIGINT REFERENCES proyectos(id),
    frecuencia_seg INTEGER NOT NULL CHECK (frecuencia_seg > 0),
    conexion VARCHAR(15) NOT NULL DEFAULT 'SIN_DATOS',
    ultima_lectura TIMESTAMP,
    CONSTRAINT conexion_valida CHECK (conexion IN ('SIN_DATOS','CONECTADO','DESCONECTADO'))
);

CREATE TABLE lectura_iot (
    id BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT NOT NULL REFERENCES dispositivo(id),
    fecha_hora TIMESTAMP NOT NULL,
    variable VARCHAR(40) NOT NULL,
    unidad VARCHAR(15) NOT NULL,
    valor DOUBLE PRECISION NOT NULL,
    anomala BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anomalia VARCHAR(40)
);
CREATE INDEX idx_lectura_dispositivo_fecha ON lectura_iot (dispositivo_id, fecha_hora DESC);

CREATE TABLE alerta_iot (
    id BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT NOT NULL REFERENCES dispositivo(id),
    lectura_id BIGINT REFERENCES lectura_iot(id),
    tipo VARCHAR(20) NOT NULL,
    severidad VARCHAR(15) NOT NULL,
    mensaje VARCHAR(300) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    CONSTRAINT alerta_tipo_valido CHECK (tipo IN ('ADVERTENCIA','CRITICA','ANOMALIA','DESCONEXION','RECONEXION','NORMALIZACION')),
    CONSTRAINT alerta_severidad_valida CHECK (severidad IN ('INFO','ADVERTENCIA','CRITICA'))
);
CREATE INDEX idx_alerta_fecha ON alerta_iot (fecha_hora DESC);

INSERT INTO dispositivo (codigo, nombre, variable, unidad, proyecto_id, frecuencia_seg)
VALUES ('sensor-lab-01', 'Sensor de temperatura del laboratorio', 'temperatura', '°C',
        (SELECT MIN(id) FROM proyectos), 5);
