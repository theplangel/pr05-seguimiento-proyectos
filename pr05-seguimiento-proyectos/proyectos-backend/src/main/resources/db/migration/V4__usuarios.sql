-- P07: usuarios y roles. NO se siembran contraseñas: el backend crea los usuarios iniciales al arrancar
-- a partir de variables de entorno (APP_LIDER_PASSWORD / APP_INTEGRANTE_PASSWORD) y las guarda con BCrypt.
CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT rol_valido CHECK (rol IN ('LIDER', 'INTEGRANTE')),
    CONSTRAINT username_no_vacio CHECK (length(trim(username)) > 0)
);
