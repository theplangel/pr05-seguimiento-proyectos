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
psql -U postgres -c "CREATE USER proyectos WITH PASSWORD '<tu-contraseña>';"
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

---

## P06 — Web 4.0: fuente IoT simulada, dashboard y datos

Amplía este mismo proyecto con **un sensor de temperatura simulado** (`sensor-lab-01`, °C) que envía una lectura cada 5 s.
Recorrido: **simulador → API Spring Boot → PostgreSQL → dashboard Angular**.

### Arranque

```bash
./scripts/generar-env.sh         # P07: crea .env con secretos aleatorios (no hay contraseñas por defecto)
docker compose up --build
# Dashboard:  http://localhost:4200/#iot   (inicia sesión con los usuarios que imprime generar-env.sh)
# Swagger UI: desactivado por defecto; en desarrollo: SWAGGER_ENABLED=true en .env
```

El servicio `simulador` arranca solo y recorre en ≈ 5 min **todas las reglas** (`ESCENARIO=demo`):
normal → advertencia → crítico → normalización → lectura fuera de rango → salto brusco → desconexión → reconexión.
Con `ESCENARIO=normal docker compose up` la temperatura se mantiene estable.
Para provocar una desconexión: `docker compose stop simulador` (y `start` para reconectar).

### API IoT

| Método y ruta | Descripción | Respuestas |
|---|---|---|
| `POST /api/iot/lecturas` | Ingesta del dispositivo (`X-API-Key`). Cuerpo: `dispositivo`, `fechaHora` (opc.), `variable`, `unidad`, `valor` | 201 · 400 · 401 · 404 |
| `GET /api/iot/estado` | Valor actual, nivel (`OK`/`ADVERTENCIA`/`CRITICO`) y conexión | 200 · 404 |
| `GET /api/iot/lecturas?desde=&hasta=&limite=` | Historial cronológico (por defecto, la última hora) | 200 · 400 |
| `GET /api/iot/resumen?desde=&hasta=` | Mínimo, máximo, promedio y anomalías | 200 · 400 |
| `GET /api/iot/alertas?limite=` | Alertas y eventos, las más nuevas primero | 200 · 400 |

Ejemplo de ingesta manual (desde la máquina anfitriona, contra el backend):

```bash
curl -i -X POST http://localhost:8080/api/iot/lecturas \
  -H 'Content-Type: application/json' -H "X-API-Key: $IOT_API_KEY" \
  -d '{"dispositivo":"sensor-lab-01","variable":"temperatura","unidad":"°C","valor":24.8}'
```

### Reglas de análisis

| Regla | Resultado |
|---|---|
| Valor < −10 °C o > 80 °C | Lectura **anómala** `FUERA_DE_RANGO` (se guarda marcada, no cuenta para el nivel) |
| Cambio > 8 °C respecto a la última lectura válida | Lectura **anómala** `SALTO_BRUSCO` |
| ≥ 30 °C / ≥ 35 °C | Nivel `ADVERTENCIA` / `CRITICO` (alerta solo al cambiar de nivel) |
| Más de 15 s sin lecturas | `DESCONECTADO` + alerta `DESCONEXION`; al volver, `RECONEXION` |

Detalle de frecuencia, anomalías, desconexión y seguridad: [`docs/arquitectura-p06.md`](docs/arquitectura-p06.md) ·
decisiones del equipo: [`docs/decisiones-p06.md`](docs/decisiones-p06.md) ·
evidencia y pasos de verificación: [`docs/EVIDENCIA-P06.md`](docs/EVIDENCIA-P06.md).

### Pruebas P06

```bash
cd proyectos-backend && mvn verify                       # ReglasIotTest, IotServiceTest, IotControllerTest + P05
cd proyectos-frontend && npm ci && npx jest --ci         # servicio IoT, gráfica y dashboard
docker compose stop simulador && ./scripts/smoke-iot.sh  # extremo a extremo contra la API real (RAPIDO=1 omite la espera de desconexión)
```

### Seguridad (resumen)

Clave de dispositivo `X-API-Key` validada antes de leer el cuerpo (401) · la ingesta **no** se expone por el puerto público del frontend (Nginx responde 403) ·
validación estricta y límites de consulta · errores sin detalles internos · contenedores sin root · la clave sale de `.env` (no versionado).

---

## P07 — Seguridad básica, pruebas y despliegue

Se aplicó seguridad al mismo PR05: **roles, secretos, validación, configuración segura**, pruebas y guía de despliegue.

### Arranque (desde cero, sin credenciales en el repositorio)

```bash
./scripts/generar-env.sh      # crea .env con secretos aleatorios e imprime los usuarios (lider / integrante)
docker compose up --build -d
# Aplicación: http://localhost:4200  →  inicia sesión
```

| Rol | Puede |
|---|---|
| `LIDER` | Consultar, crear proyectos y cambiar estados |
| `INTEGRANTE` | Solo consultar (proyectos y dashboard IoT) |
| Dispositivo IoT | Enviar lecturas con su `X-API-Key` (no es un usuario) |

Todo lo no declarado se **deniega**. Autenticación HTTP Basic sin estado (sin cookies); la credencial vive solo en memoria del navegador; freno a la fuerza bruta: 5 fallos en 60 s → `429`.

### Controles de configuración

PostgreSQL sin publicar · backend solo en `127.0.0.1:8080` · Swagger desactivado (`SWAGGER_ENABLED=true` solo en desarrollo) · cabeceras de seguridad y CSP en Nginx · contenedores sin root · errores sin trazas · sin contraseñas por defecto (el backend no arranca sin contraseñas de ≥ 12 caracteres).

### Pruebas y auditoría

```bash
cd proyectos-backend && mvn verify                  # incluye SeguridadRolesTest, LimitadorIntentosFilterTest, UsuarioInicializadorTest…
cd proyectos-frontend && npm ci && npx jest --ci    # 64 pruebas (login, interceptor, UI por rol, dashboard…)
./scripts/e2e-p07.sh | tee docs/evidencia-e2e-p07.txt          # CP-01/02, CN-01/02/03 y E2E-01 contra el sistema real
./scripts/auditoria-seguridad.sh | tee docs/evidencia-auditoria.txt   # secretos, configuración y dependencias
```

### Documentación P07

| Documento | Contenido |
|---|---|
| [`docs/seguridad-owasp-p07.md`](docs/seguridad-owasp-p07.md) | Lista OWASP Top 10:2021 aplicada, validación y datos sensibles |
| [`docs/defectos-p07.md`](docs/defectos-p07.md) | Registro de defectos y correcciones (DEF-01 a DEF-11) |
| [`docs/pruebas-p07.md`](docs/pruebas-p07.md) | Casos positivos, negativos y E2E con su estado de ejecución |
| [`docs/despliegue.md`](docs/despliegue.md) | Guía reproducible de despliegue, operación y reversión |
| [`docs/bitacora-contribucion.md`](docs/bitacora-contribucion.md) | Evidencia individual (commits, bitácora, defensa) |
| [`docs/evidencia-auditoria-p07.txt`](docs/evidencia-auditoria-p07.txt) | Auditoría antes y después de las correcciones |

