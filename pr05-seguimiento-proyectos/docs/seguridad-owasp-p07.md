# P07 — Lista de comprobación de seguridad (OWASP Top 10:2021)

Alcance: PR05 «Seguimiento de proyectos estudiantiles» (Angular 18 · Spring Boot 2.7/Java 11 · PostgreSQL · Nginx · Docker Compose · fuente IoT simulada).
Resultado: **Cumple** = control implementado y verificable · **Mitigado** = riesgo reducido con control compensatorio · **Aceptado** = riesgo residual documentado.
Hallazgos y correcciones: [`defectos-p07.md`](defectos-p07.md) · Casos de prueba: [`pruebas-p07.md`](pruebas-p07.md).

| # | Categoría | Riesgo en este proyecto | Control aplicado | Evidencia | Resultado |
|---|---|---|---|---|---|
| A01 | Control de acceso roto | Cualquiera podía crear proyectos o cambiar estados (la API no tenía autenticación) | Spring Security: roles `LIDER` (lee y escribe) e `INTEGRANTE` (solo lee); **todo lo no listado se deniega**; la ingesta IoT solo con API key; la UI oculta botones por rol, pero el servidor decide | `SecurityConfig`; `SeguridadRolesTest`; CN-01, CN-02 | Cumple |
| A02 | Fallas criptográficas | Contraseñas en texto claro o con valores por defecto; credencial real versionada | Contraseñas con **BCrypt**; sin valores por defecto; secretos solo por entorno (`.env` no versionado); credencial de P02 retirada del repositorio; la sesión del navegador vive solo en memoria | `UsuarioInicializador`, `generar-env.sh`, `auditoria-seguridad.sh`; DEF-01, DEF-03 | Cumple. *Residual: sin TLS en local (ver A05)* |
| A03 | Inyección | SQL por entradas; XSS en la interfaz | Spring Data (consultas parametrizadas), `@Valid` con tipos y longitudes, campos desconocidos rechazados (`fail-on-unknown-properties`); restricciones `CHECK` en BD; Angular escapa las interpolaciones y no se usa `innerHTML`; CSP `script-src 'self'` | `ProyectoRequest`, `LecturaRequest`, migraciones; CN de validación (400); CSP de `nginx.conf` | Cumple |
| A04 | Diseño inseguro | Transiciones de estado saltables; datos anómalos que falsean el análisis | Máquina de estados validada en servidor (409); anomalías marcadas y excluidas del nivel; límites de fecha y de consulta; autenticación **antes** de leer el cuerpo en la ingesta | `Estado`, `ReglasIot`, `IotService`; `docs/arquitectura-p06.md` | Cumple |
| A05 | Configuración de seguridad incorrecta | Puertos expuestos, Swagger abierto, cabeceras ausentes, trazas en errores | PostgreSQL **sin publicar**; backend solo en `127.0.0.1`; Swagger **desactivado** por defecto; Actuator solo `health`; Nginx con CSP, `nosniff`, `X-Frame-Options`, `Referrer-Policy`, `server_tokens off` y límite de cuerpo; contenedores sin root, `no-new-privileges` y `cap_drop`; errores sin trazas | `docker-compose.yml`, `nginx.conf`, `application.properties`; DEF-04, DEF-05, DEF-06; `e2e-p07.sh` | Cumple. *HSTS/TLS: se activa al poner un proxy HTTPS (guía de despliegue §7)* |
| A06 | Componentes vulnerables y desactualizados | Dependencias con avisos conocidos | `npm audit` automatizado en `auditoria-seguridad.sh`; versiones de Spring Boot gestionadas por el BOM; análisis Java opcional con OWASP Dependency-Check | `docs/evidencia-auditoria-p07.txt`; **DEF-07** | **Aceptado (parcial):** Angular 18 tiene avisos altos/moderados sin parche en su rama; no son explotables con las funciones que usa la app (sin SSR, i18n ni hidratación). Plan: migrar a la versión LTS vigente |
| A07 | Fallas de identificación y autenticación | Fuerza bruta contra el login; enumeración de usuarios | Límite de **5 fallos/60 s por usuario → 429** con `Retry-After`; mismo mensaje para usuario inexistente y contraseña incorrecta; contraseñas ≥ 12 caracteres; sin sesión persistente (sin `WWW-Authenticate`, sin cookies) | `LimitadorIntentosFilter`; `LimitadorIntentosFilterTest`; CN-03 | Cumple. *Residual: sin MFA ni bloqueo por IP* |
| A08 | Fallas de integridad de software y datos | Ingesta IoT falsificada; imágenes sin fijar | Ingesta autenticada con API key en tiempo constante, que **falla cerrada** si no hay clave; validación de variable/unidad/fecha; migraciones versionadas con Flyway (solo avanzan) | `ApiKeyFilter`; `IotControllerTest`; DEF-10 | Cumple. *Residual: imágenes por etiqueta, no por *digest** |
| A09 | Fallas de registro y monitoreo | Ataques sin rastro | Se registran autenticaciones fallidas y bloqueos (usuario e IP saneados contra inyección en logs); alertas IoT persistidas en `alerta_iot`; errores internos solo al log | `LimitadorIntentosFilter`, `ApiExceptionHandler`; `docker compose logs backend` | Cumple. *Residual: sin agregación centralizada de logs* |
| A10 | Falsificación de solicitudes del lado del servidor (SSRF) | El backend llamando a URLs controladas por el usuario | **No aplica:** el backend no realiza peticiones salientes basadas en entradas; el simulador usa una URL fija del entorno | Revisión de código | N/A |

## Validación de entradas y datos sensibles (rúbrica R07)

| Entrada | Control |
|---|---|
| `POST /api/proyectos` | `@NotBlank`, `@Size`, trim, campos desconocidos → 400 |
| `PATCH …/estado` | Enum cerrado; valor ausente o desconocido → 400; transición inválida → 409 |
| `POST /api/iot/lecturas` | API key; `@Valid`; variable y unidad = las del dispositivo; fecha ≤ +60 s y ≤ 24 h; valor numérico |
| Parámetros de consulta | Fechas ISO-8601, `limite` 1–2000 / 1–200, rango ≤ 7 días → 400 |
| Login (Angular) | Obligatorios, longitud máxima, `autocomplete`, campo `password` |

| Dato sensible | Tratamiento |
|---|---|
| Contraseñas de usuarios | BCrypt en BD; nunca se devuelven por la API; el formulario las borra tras usarlas |
| Contraseña de BD, API key, contraseñas iniciales | Solo en `.env` (permisos 600, no versionado); `docker-compose` falla si faltan; `.env.example` sin valores |
| Credencial en el navegador | Solo en memoria; no se guarda en `localStorage`, `sessionStorage` ni cookies |
| Credencial heredada de P02 | Retirada de `db.properties` (plantilla `db.properties.example`); **rotar** la contraseña antigua y, si procede, limpiar el historial de Git |

## Riesgos y criterios de aceptación del banco (C11 · PR05)

Los criterios de aceptación del banco se cumplen con las verificaciones de este documento. **Antes de entregar, copia aquí los riesgos y criterios literales de PR05 del C11** y marca cuál evidencia cubre cada uno (la tabla de arriba y `pruebas-p07.md` sirven de referencia).
