-- ============================================================================
-- PR05: Sistema de Seguimiento de Proyectos
-- Módulo M02 - Actividad P02: Web 1.0 con JSP, Tomcat y PostgreSQL
-- Script DDL y Datos de Prueba Iniciales (6 Entidades)
-- Experiencia Educativa: Desarrollo de Sistemas Web - Universidad Veracruzana
-- ============================================================================

-- 1. Eliminación limpia de tablas si existen (en orden inverso de dependencias)
DROP TABLE IF EXISTS evento_avance CASCADE;
DROP TABLE IF EXISTS evidencia CASCADE;
DROP TABLE IF EXISTS impedimento CASCADE;
DROP TABLE IF EXISTS tarea CASCADE;
DROP TABLE IF EXISTS integrante CASCADE;
DROP TABLE IF EXISTS proyecto CASCADE;

-- 2. Creación de Entidades (DDL)

-- Entidad 1: Proyecto
CREATE TABLE proyecto (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    fecha_inicio DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'PAUSADO', 'CERRADO'))
);

-- Entidad 2: Integrante (del equipo por proyecto)
CREATE TABLE integrante (
    id SERIAL PRIMARY KEY,
    proyecto_id INT NOT NULL REFERENCES proyecto(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    rol_equipo VARCHAR(50) NOT NULL
);

-- Entidad 3: Tarea
CREATE TABLE tarea (
    id SERIAL PRIMARY KEY,
    proyecto_id INT NOT NULL REFERENCES proyecto(id) ON DELETE CASCADE,
    titulo VARCHAR(200) NOT NULL,
    responsable_id INT NOT NULL REFERENCES integrante(id) ON DELETE RESTRICT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'BLOQUEADA', 'COMPLETADA')),
    fecha_limite DATE
);

-- Entidad 4: Impedimento (bloqueos asociados a una tarea)
CREATE TABLE impedimento (
    id SERIAL PRIMARY KEY,
    tarea_id INT NOT NULL REFERENCES tarea(id) ON DELETE CASCADE,
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'RESUELTO'))
);

-- Entidad 5: Evidencia (artefactos de comprobación)
CREATE TABLE evidencia (
    id SERIAL PRIMARY KEY,
    tarea_id INT NOT NULL REFERENCES tarea(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    url_o_ruta TEXT NOT NULL,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Entidad 6: EventoAvance (bitácora de progreso del proyecto)
CREATE TABLE evento_avance (
    id SERIAL PRIMARY KEY,
    proyecto_id INT NOT NULL REFERENCES proyecto(id) ON DELETE CASCADE,
    descripcion TEXT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Datos de Prueba Iniciales (DML Ficticio para Evaluación R02)

-- Inserción de Proyectos
INSERT INTO proyecto (id, nombre, descripcion, fecha_inicio, estado) VALUES
(1, 'PR05 - Sistema de Seguimiento de Proyectos', 'Primer incremento Web 1.0 delimitado en C11 con JSP, Servlets y PostgreSQL.', '2026-09-01', 'ACTIVO'),
(2, 'PR02 - Portal de Gestión Escolar', 'Módulo de inscripción de alumnos y seguimiento académico.', '2026-08-25', 'PAUSADO');

-- Reinicio de secuencia de proyecto
SELECT setval('proyecto_id_seq', (SELECT MAX(id) FROM proyecto));

-- Inserción de Integrantes (Roles del equipo UV)
INSERT INTO integrante (id, proyecto_id, nombre, rol_equipo) VALUES
(1, 1, 'Persona A (Vistas)', 'Persona A - Vistas JSP'),
(2, 1, 'Persona B (Controladores)', 'Persona B - Servlets'),
(3, 1, 'Persona C (Persistencia)', 'Persona C - PostgreSQL / DAO');

SELECT setval('integrante_id_seq', (SELECT MAX(id) FROM integrante));

-- Inserción de Tareas (con diferentes estados para probar máquina de estados)
INSERT INTO tarea (id, proyecto_id, titulo, responsable_id, estado, fecha_limite) VALUES
(1, 1, 'Construir vistas JSP con formularios y validación de errores', 1, 'EN_PROGRESO', '2026-09-11'),
(2, 1, 'Implementar controladores Servlets y mapeo HTTP', 2, 'EN_PROGRESO', '2026-09-11'),
(3, 1, 'Definir esquema SQL y operaciones JDBC parametrizadas', 3, 'COMPLETADA', '2026-09-08'),
(4, 1, 'Empaquetar WAR y verificar despliegue en Tomcat 9', 1, 'PENDIENTE', '2026-09-11'),
(5, 1, 'Configuración de pruebas y verificación reproducible', 2, 'BLOQUEADA', '2026-09-12');

SELECT setval('tarea_id_seq', (SELECT MAX(id) FROM tarea));

-- Inserción de Impedimentos
-- Tarea 5 tiene un impedimento ABIERTO (permite probar regla de no completar con impedimentos abiertos)
INSERT INTO impedimento (id, tarea_id, descripcion, estado) VALUES
(1, 5, 'Falta definir la versión final de Tomcat en el laboratorio', 'ABIERTO'),
(2, 3, 'Incompatibilidad de driver postgresql resuelta con dependencia maven', 'RESUELTO');

SELECT setval('impedimento_id_seq', (SELECT MAX(id) FROM impedimento));

-- Inserción de Evidencias
INSERT INTO evidencia (id, tarea_id, tipo, url_o_ruta, fecha) VALUES
(1, 3, 'codigo', 'sql/init.sql', '2026-09-08'),
(2, 3, 'documento', 'docs/modelo-relacional.png', '2026-09-08'),
(3, 1, 'enlace', 'http://localhost:8080/pr05-seguimiento-proyectos/proyectos', '2026-09-11');

SELECT setval('evidencia_id_seq', (SELECT MAX(id) FROM evidencia));

-- Inserción de Eventos de Avance
INSERT INTO evento_avance (proyecto_id, descripcion) VALUES
(1, 'Inicio del sprint M02 y delimitación del PR05 en F02.'),
(1, 'Persistencia en PostgreSQL concluida con 6 entidades.'),
(1, 'Vistas JSP completadas con soporte de validaciones positivas y negativas.');

