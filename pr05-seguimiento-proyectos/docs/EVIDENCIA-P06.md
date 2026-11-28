# Evidencia P06 — Web 4.0, IoT, dashboard y datos

Recorrido: **simulador → API (Spring Boot) → PostgreSQL → dashboard Angular**, con reglas de análisis y alertas.

> **Desde P07** la API exige sesión: el dashboard pide iniciar sesión, `smoke-iot.sh` lee las credenciales de `.env` (`./scripts/generar-env.sh`) y las contraseñas por defecto ya no existen.

## A. Verificado durante el desarrollo

| Qué | Cómo | Resultado |
|---|---|---|
| Frontend: servicio IoT, gráfica, dashboard, pestañas | `cd proyectos-frontend && npm ci && npx jest --ci` | 6 suites, **43 pruebas pasan** (incluye las 3 suites nuevas: contrato HTTP, geometría de la gráfica y dashboard con estados normal / advertencia / crítico / desconectado / sin datos / error de API / refresco cada 5 s / cambio de rango) |
| Frontend compila para producción | `npx ng build --configuration production` | Compila (bundle inicial ≈ 240 kB) |
| Migraciones SQL | Análisis sintáctico con el parser de PostgreSQL (`pglast`) de `V1`, `V2` y `V3` | Válidas |
| Contrato OpenAPI | Carga YAML + comprobación de que todos los `$ref` existen | 8 rutas, 11 esquemas, sin referencias rotas |
| Simulador | Generación de su ciclo `demo` contra las reglas | Recorre normal → advertencia → crítico → normalización → fuera de rango → salto brusco → silencio → reconexión; ningún paso «normal» supera el salto máximo |
| Script `smoke-iot.sh` | `bash -n` | Sintaxis válida |

## B. Por ejecutar en tu equipo (no se pudo en el entorno de desarrollo)

El entorno donde se escribió el backend no tiene acceso a Maven Central ni a Docker, así que **el backend Java no se compiló ni se probó allí**. Ejecuta esto y anexa los resultados:

| # | Comando | Qué debe verse |
|---|---|---|
| 1 | `cd proyectos-backend && mvn verify` | Pruebas nuevas en `mx.uv.proyectos.iot` (`ReglasIotTest`, `IotServiceTest`, `IotControllerTest`) más las de P05, todas en verde |
| 2 | `docker compose up --build` | 4 contenedores: `db`, `backend` (healthy), `frontend` y `simulador` |
| 3 | Abrir `http://localhost:4200/#iot` | Valor actual, nivel, conexión, gráfica que crece cada 5 s y alertas aparecen solas |
| 4 | `docker compose logs -f simulador` | Una línea cada 5 s con `HTTP 201`; las anómalas aparecen con `ANÓMALA(...)` |
| 5 | Esperar ≈ 5 min (ciclo `demo`) | El dashboard pasa por advertencia (ámbar), crítico (rojo), ✕ de anomalías, desconexión y reconexión |
| 6 | `docker compose stop simulador` y esperar 20 s | Banner «Dispositivo desconectado» y alerta `DESCONEXION` |
| 7 | `docker compose stop simulador && ./scripts/smoke-iot.sh \| tee docs/evidencia-smoke-iot.txt` | `== Resultado: N correctas, 0 fallidas ==` (positivas, anomalías, desconexión y negativas 400/401/403/404/405) |
| 8 | `docker compose exec db psql -U proyectos -d proyectos -c "SELECT dispositivo_id, fecha_hora, variable, unidad, valor, anomala FROM lectura_iot ORDER BY fecha_hora DESC LIMIT 10;"` | Historial persistido con dispositivo, fecha-hora, variable, unidad y valor |
| 9 | Reiniciar: `docker compose restart backend` y recargar el dashboard | El historial sigue ahí (persistencia en PostgreSQL) |
| 10 | `curl -i -X POST localhost:4200/api/iot/lecturas -H 'Content-Type: application/json' -d '{}'` | `403` (la ingesta no se expone por el frontend) |

## C. Capturas sugeridas para el informe

1. Dashboard en estado normal.
2. Dashboard con advertencia o crítico (banner visible).
3. Gráfica con una ✕ de anomalía y un corte por desconexión.
4. Banner de desconexión y tabla de alertas con `DESCONEXION` y `RECONEXION`.
5. Salida de `smoke-iot.sh` y consulta SQL del historial.

## D. Trazabilidad con la rúbrica R06

| Criterio | Dónde está |
|---|---|
| Integración IoT (3) | `simulador/simulador.py` → `ApiKeyFilter` → `IotService` (`registrar`) · `docs/arquitectura-p06.md` |
| Dashboard (3) | `proyectos-frontend/src/app/iot/` · pestaña «Dashboard IoT» |
| Historial PostgreSQL (2) | `V3__iot.sql` (`lectura_iot` + índice temporal) · `GET /api/iot/lecturas?desde=&hasta=` |
| Eventos / lectura periódica (2) | Ingesta REST + polling de 5 s · decisiones D2–D4 en `docs/decisiones-p06.md` |
| Análisis y alertas (2) | `ReglasIot`, `alerta_iot`, banners y tabla de alertas · §5–§7 de `docs/arquitectura-p06.md` |

## E. Ejecución realizada el 2026-11-19

| Comprobación | Resultado |
|---|---|
| `npx jest --ci` | VERIFICADO: 7 suites y 53 pruebas aprobadas |
| `mvn test` | VERIFICADO: 65 pruebas aprobadas, `BUILD SUCCESS` |
| Angular en desarrollo | VERIFICADO: ruta `/iot` disponible como `http://localhost:4200/#iot` y navegación hash |
| Dashboard móvil | VERIFICADO visualmente: viewport 390×844; tarjetas, banners y tablas adaptables |
| Estados UI normal, advertencia, crítico, anomalía y desconexión | VERIFICADO con datos controlados interceptados por navegador; capturas en `docs/captura-1-normal.png`, `captura-2-critico.png`, `captura-3-anomalia.png` y `captura-4-desconexion.png` |
| Docker Compose / PostgreSQL extremo a extremo | NO_VERIFICADO en esta máquina: Docker Desktop no tiene daemon activo ni instalación ejecutable disponible |

Captura móvil adicional: `docs/captura-4-desconexion-mobile.png`.

La captura con datos controlados demuestra el render y los estados de la UI; no sustituye ni declara persistencia real. La persistencia PostgreSQL se conserva en `V3__iot.sql`, `lectura_iot` y `alerta_iot`, y debe ejecutarse con `docker compose up --build` cuando Docker esté disponible.
