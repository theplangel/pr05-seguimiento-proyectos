# PR05 — Seguimiento de proyectos estudiantiles · P05 (API REST + integración)

Integración **Angular 18 ↔ Spring Boot 2.7 (Java 11) ↔ PostgreSQL 15**, sin servicios externos obligatorios.

> Desarrollo de Sistemas Web (DSW-19559) · Actividad P05 · Rúbrica R05 (12 pts)
> Incrementos previos (P02 JSP/Servlets y P03 JSF) se conservan en `src/` y `pom.xml`; su README está en [`docs/historico/`](docs/historico/README-P02-P03.md).

## 1. Estructura

| Ruta | Contenido |
|---|---|
| `proyectos-backend/` | API REST Spring Boot (controlador, servicio, JPA, Flyway, OpenAPI) y sus pruebas JUnit |
| `proyectos-frontend/` | Aplicación Angular (consume la API) y sus pruebas Jest |
| `docker-compose.yml` | Levanta PostgreSQL + backend + frontend con un solo comando |
| `docs/openapi.yaml` | Contrato OpenAPI 3 (copia de `proyectos-backend/openapi.yaml`; también en `/v3/api-docs`) |
| `scripts/smoke-test.sh` | Prueba extremo a extremo contra la API real (positivas y negativas) |
| `docs/EVIDENCIA.md` | Evidencia de ejecución y trazabilidad con R05 |

## 2. Requisitos

- **Opción A (recomendada):** Docker + Docker Compose v2.
- **Opción B (local):** JDK 11, Maven 3.8+, Node 20+, PostgreSQL 15 (o similar).

## 3. Ejecución con Docker (un comando)

```bash
docker compose up --build
```

| Servicio | URL |
|---|---|
| Aplicación Angular | http://localhost:4200 |
| API | http://localhost:8080/api/proyectos |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI | http://localhost:8080/v3/api-docs |
| PostgreSQL (opcional) | `localhost:5433`, usuario/BD `proyectos` |

Flyway crea el esquema y carga 4 proyectos de prueba al primer arranque. Los datos persisten en el volumen `pgdata`.
Reinicio limpio: `docker compose down -v && docker compose up --build`.
La contraseña por defecto es solo para uso local; se puede cambiar con `DB_PASSWORD=... docker compose up`.

## 4. Ejecución local sin Docker

```bash
# 1) Base de datos
psql -U postgres -c "CREATE USER proyectos WITH PASSWORD 'proyectos_local_2026';"
psql -U postgres -c "CREATE DATABASE proyectos OWNER proyectos;"

# 2) Backend (Flyway migra al arrancar) -> http://localhost:8080
cd proyectos-backend && mvn spring-boot:run

# 3) Frontend -> http://localhost:4200  (ng serve usa proxy.conf.json hacia :8080)
cd proyectos-frontend && npm ci && npm start
```

Variables del backend: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `APP_CORS_ORIGINS`.

## 5. API REST

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| GET | `/api/health` | Salud del servicio | 200 |
| GET | `/api/proyectos[?estado=]` | Consulta (con filtro opcional) | 200, 400 |
| GET | `/api/proyectos/{id}` | Consulta por id | 200, 400, 404 |
| POST | `/api/proyectos` | Registra un proyecto (inicia `REGISTRADO`) | 201 + `Location`, 400, 409 |
| PATCH | `/api/proyectos/{id}/estado` | Cambio de estado | 200, 400, 404, 409 |

Estados: `REGISTRADO → EN_PROGRESO → EN_REVISION → ENTREGADO`; `EN_REVISION` puede volver a `EN_PROGRESO`; `ENTREGADO` es final.

**Validaciones:** título, descripción, integrantes y asesor obligatorios (no vacíos tras recortar espacios) con longitudes máximas 120/500/250/120; JSON mal formado, campos desconocidos y estados inválidos → 400; título duplicado y transición no permitida → 409; recurso inexistente → 404; método no permitido → 405.

Todos los errores usan el mismo cuerpo:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "mensaje": "Datos de entrada inválidos",
  "ruta": "/api/proyectos", "campos": { "titulo": "El título es obligatorio" } }
```

## 6. Pruebas

```bash
cd proyectos-backend  && mvn test        # JUnit 5: MockMvc (positivas/negativas), servicio y reglas de estado
cd proyectos-frontend && npm test        # Jest: servicio HTTP y flujo de usuario del componente
./scripts/smoke-test.sh                  # extremo a extremo contra el stack levantado (19 verificaciones)
```

`smoke-test.sh` recorre el ciclo completo (crear → 3 cambios de estado → releer) y los casos inválidos, y termina con código ≠ 0 si algo falla.
Guardar evidencia: `./scripts/smoke-test.sh | tee docs/evidencia-smoke.txt`.

## 7. Recorrido manual del flujo extremo a extremo

1. Abrir http://localhost:4200: el indicador superior debe decir **API: activa** y verse la lista de 4 proyectos.
2. Enviar el formulario vacío → errores en rojo, sin petición a la API.
3. Registrar un proyecto válido → banner verde y aparece en la tabla en estado `REGISTRADO`.
4. Repetir el mismo título → banner rojo con el 409 de la API.
5. Pulsar `→ EN_PROGRESO`, luego `→ EN_REVISION`, luego `→ ENTREGADO`; en `ENTREGADO` ya no hay acciones.
6. Filtrar por estado y recargar la página: los datos persisten (PostgreSQL).
7. Verificar en BD: `docker compose exec db psql -U proyectos -c "select id,titulo,estado from proyectos order by id;"`

## 8. Trazabilidad con la rúbrica R05

| Criterio | Pts | Dónde verificarlo |
|---|---|---|
| API Spring funcional | 3 | §5, Swagger UI, `ProyectoControllerTest`, `smoke-test.sh` |
| Integración Angular | 3 | `proyectos-frontend/src/app` (servicio + componente), §7, specs Jest |
| Persistencia PostgreSQL | 2 | `db/migration/V1,V2`, volumen `pgdata`, paso 7 del §7 |
| Errores y validación | 2 | `ApiExceptionHandler`, `ProyectoRequest`, casos negativos de tests y smoke |
| Pruebas y repositorio | 2 | §6, `docs/EVIDENCIA.md`, historial de commits |
