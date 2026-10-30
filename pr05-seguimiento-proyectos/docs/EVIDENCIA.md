# Evidencia P05

## A. Verificada al preparar el repositorio

**Frontend Angular (Jest)** — `cd proyectos-frontend && npm test`

```
Test Suites: 3 passed, 3 total
Tests:       21 passed, 21 total
```
Cubre contrato HTTP del servicio (GET/POST/PATCH, 400/404/409, sin conexión) y el flujo de usuario del componente
(listar, registrar, validación en cliente, error 409, cambio de estado, filtro, estado final). `ng build` en producción: sin errores.

**PostgreSQL** — migraciones `V1` y `V2` ejecutadas sobre una instancia real: crean la tabla y cargan 4 proyectos;
las restricciones rechazan título vacío (`titulo_no_vacio`) y título duplicado (`proyectos_titulo_unico`).

## B. Pendiente de ejecutar en tu equipo (requiere Docker/Maven)

Estas corridas dependen de dependencias Maven y Docker; pega aquí la salida real:

1. `docker compose up --build` → captura de `docker compose ps` (3 servicios healthy).
2. `cd proyectos-backend && mvn test` → resumen `Tests run: …, Failures: 0`.
3. `./scripts/smoke-test.sh | tee docs/evidencia-smoke.txt` → `== Resultado: 19 correctas, 0 fallidas ==`.
4. Capturas de http://localhost:4200: lista inicial, error de validación, alta exitosa, error 409, cambio de estado.
5. Captura de Swagger UI (http://localhost:8080/swagger-ui.html) y de `psql` mostrando el estado persistido.
