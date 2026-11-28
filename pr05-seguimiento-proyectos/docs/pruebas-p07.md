# P07 — Pruebas funcionales: casos, resultados y evidencia

Mínimo exigido: **2 positivas, 2 negativas y 1 extremo a extremo** (más pruebas adicionales). Cada caso tiene una versión automatizada contra el sistema real (`scripts/e2e-p07.sh`) y pruebas de unidad/integración de capa web que corren sin Docker.

## 1. Casos exigidos

| ID | Tipo | Objetivo | Pasos | Resultado esperado | Automatizado en |
|---|---|---|---|---|---|
| **CP-01** | Positiva | Un LIDER inicia sesión y consulta | `GET /api/auth/yo` y `GET /api/proyectos` con credenciales de `lider` | 200; rol `LIDER`; lista de proyectos | `e2e-p07.sh` · `SeguridadRolesTest.liderAutenticadoConsultaProyectos` · `auth.service.spec` |
| **CP-02** | Positiva | El LIDER registra un proyecto y lo avanza | `POST /api/proyectos` y `PATCH /api/proyectos/{id}/estado` con `EN_PROGRESO` | 201 y después 200 con `estado = EN_PROGRESO` | `e2e-p07.sh` · `SeguridadRolesTest.liderCreaUnProyecto`, `liderCambiaElEstadoDeUnProyecto` |
| **CN-01** | Negativa | Sin credenciales o con contraseña errónea | `GET /api/proyectos` sin cabecera y con contraseña incorrecta | 401, sin `WWW-Authenticate` ni detalles internos | `e2e-p07.sh` · `SeguridadRolesTest.sinCredencialesDevuelve401…`, `contrasenaIncorrectaDevuelve401` |
| **CN-02** | Negativa | Un INTEGRANTE intenta escribir | `PATCH …/estado` y `POST /api/proyectos` como `integrante`; luego consulta el proyecto | 403 y el estado **no** cambia | `e2e-p07.sh` · `SeguridadRolesTest.integranteNoPuedeCambiarEstados`, `integranteNoPuedeCrearProyectos` · `proyectos.component.spec` (UI solo lectura) |
| **E2E-01** | Extremo a extremo | Flujo completo con roles, IoT y configuración | LIDER crea → INTEGRANTE lo ve → LIDER avanza → transición inválida (409) → el dispositivo envía una lectura con API key → INTEGRANTE consulta el estado IoT → ingesta sin clave (401) → Swagger, Actuator, cabeceras de Nginx, puerto de PostgreSQL | Cada paso con el código esperado | `scripts/e2e-p07.sh` |

## 2. Pruebas adicionales

| ID | Tipo | Qué comprueba | Dónde |
|---|---|---|---|
| CN-03 | Negativa | Fuerza bruta: el 6.º intento fallido se frena (429) sin afectar a otros usuarios | `e2e-p07.sh` · `SeguridadRolesTest.laFuerzaBrutaSeFrenaConUn429…` · `LimitadorIntentosFilterTest` |
| CN-04 | Negativa | Validación: JSON mal formado, campos desconocidos, valores fuera de rango, fechas inválidas, rangos invertidos (400) | `ProyectoControllerTest`, `IotControllerTest`, `smoke-iot.sh` |
| CN-05 | Negativa | Ingesta IoT sin clave o con clave incorrecta (401); dispositivo desconocido (404) | `IotControllerTest`, `smoke-iot.sh` |
| CP-03 | Positiva | Reglas IoT: umbrales, anomalías, desconexión y reconexión | `ReglasIotTest`, `IotServiceTest`, `smoke-iot.sh` |
| CP-04 | Positiva | Secretos: los usuarios se crean con BCrypt y se exige una clave de ≥ 12 caracteres | `UsuarioInicializadorTest`, `UsuarioDetallesServiceTest` |
| CP-05 | Positiva | Interfaz: login, cierre de sesión, interceptor (la credencial solo viaja a `/api/`), UI según rol | `auth.service.spec`, `auth.interceptor.spec`, `login.component.spec`, `app.component.spec` |
| — | Estáticas | Secretos, configuración y dependencias | `scripts/auditoria-seguridad.sh` |

## 3. Resultados de ejecución

| Qué | Cómo se ejecuta | Estado |
|---|---|---|
| Frontend (9 suites, **64 pruebas**) | `cd proyectos-frontend && npm ci && npx jest --ci` | ✅ **64 de 64 pasan** (ejecutado el 2026-11-27) |
| Compilación del frontend | `npx ng build --configuration production` | ✅ Build de producción correcto (ejecutado el 2026-11-27) |
| Auditoría estática | `./scripts/auditoria-seguridad.sh` | ❌ 14 correctas, 1 fallida: la credencial antigua de P02 permanece en el historial Git; pendiente rotación por el equipo (ver `evidencia-auditoria.txt`) |
| Backend Java (unidad + capa web, incluye `SeguridadRolesTest`) | `cd proyectos-backend && mvn verify` | ✅ **93 pruebas, 0 fallidas; BUILD SUCCESS** (ejecutado el 2026-11-27; ver `evidencia-mvn.txt`) |
| E2E contra el sistema real | `docker compose up --build -d` y `./scripts/e2e-p07.sh \| tee docs/evidencia-e2e-p07.txt` | ✅ **35 correctas, 0 fallidas** (ejecutado el 2026-11-27) |
| Humo de la API y del IoT | `./scripts/smoke-test.sh` y `docker compose stop simulador && ./scripts/smoke-iot.sh` | ✅ **API: 20 correctas; IoT: 56 correctas; 0 fallidas** (ejecutado el 2026-11-27) |

Las salidas ejecutadas se guardan en los archivos `evidencia-*.txt` de `docs/`.

## 4. Defectos hallados al probar

Ver [`defectos-p07.md`](defectos-p07.md): cada defecto indica la prueba que lo detecta y confirma su corrección.
