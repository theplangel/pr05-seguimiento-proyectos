# Evidencia P05

## A. Verificada al preparar el repositorio

**Frontend Angular (Jest)** — `cd proyectos-frontend && npm test`

```
Test Suites: 4 passed, 4 total
Tests:       31 passed, 31 total
```
Cubre contrato HTTP del servicio (GET/POST/PATCH, 400/404/409, sin conexión), el flujo de la lista y el formulario
(listar, registrar, validación en cliente, 400 por campo, 409, cambio de estado, bloqueo durante el PATCH, filtro,
estado final, error de carga + Reintentar), el detalle (`/proyectos/{id}`: carga, PATCH, 409, 404, id inválido,
sin conexión) y el indicador de salud. `ng build --configuration production`: sin errores (329 kB inicial, 89 kB gzip).
Ejecutado el 2026-10-30 en contenedor `node:20-alpine` (`npm ci && npm run test:ci && npx ng build`). **VERIFICADO.**

**Frontend a través de Nginx (stack Docker real)** — `docs/evidencia-frontend.txt` — **VERIFICADO** 2026-10-30.
`docker compose up --build` con BD limpia; rutas del SPA `/`, `/proyectos`, `/proyectos/1` → 200 (`index.html`);
proxy `/api` → backend; alta 201 + `Location`; 400 con `campos`, 409 duplicado, 404, 409 transición inválida;
tres PATCH 200 hasta `ENTREGADO`; estado releído por GET y comprobado con `psql`.

**PostgreSQL** — migraciones `V1` y `V2` ejecutadas sobre una instancia real: crean la tabla y cargan 4 proyectos;
las restricciones rechazan título vacío (`titulo_no_vacio`) y título duplicado (`proyectos_titulo_unico`).

## B. Pendiente de ejecutar en tu equipo (requiere Docker/Maven)

Estas corridas dependen de dependencias Maven y Docker; pega aquí la salida real:

1. `docker compose up --build` → captura de `docker compose ps` (3 servicios healthy).
2. `cd proyectos-backend && mvn test` → resumen `Tests run: …, Failures: 0`.
3. `./scripts/smoke-test.sh | tee docs/evidencia-smoke.txt` → `== Resultado: 19 correctas, 0 fallidas ==`.
4. Capturas de http://localhost:4200: lista inicial, error de validación, alta exitosa, error 409, cambio de estado,
   detalle `/proyectos/{id}`, 404 en `/proyectos/999` y "sin conexión" con el backend detenido (README §8). **PENDIENTE** (manual).
5. Captura de Swagger UI (http://localhost:8080/swagger-ui.html) y de `psql` mostrando el estado persistido.
