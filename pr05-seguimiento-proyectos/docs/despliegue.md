# Guía de despliegue y configuración (sin credenciales reales)

Sistema: **PostgreSQL + Spring Boot (Java 11) + Angular/Nginx + simulador IoT**, orquestado con Docker Compose.
Esta guía no contiene credenciales: todos los secretos se generan localmente.

## 1. Requisitos

| Herramienta | Versión mínima | Para qué |
|---|---|---|
| Docker Engine + Compose v2 | 24 / 2.20 | Despliegue (único requisito para ejecutar) |
| Git | 2.30 | Obtener el código |
| bash y curl | cualquiera | Scripts de generación de secretos y pruebas (Linux, macOS, WSL o Git Bash) |
| JDK 11 y Maven 3.8 · Node 20 | opcional | Solo para compilar y probar fuera de Docker |

Puertos: **4200** (aplicación) y **8080** (API, solo en `127.0.0.1`). PostgreSQL no se publica.

## 2. Obtener el código

```bash
git clone <URL-del-repositorio>
cd pr05-seguimiento-proyectos
```

## 3. Configurar los secretos

```bash
./scripts/generar-env.sh
```

- Crea `.env` (permisos 600) con contraseñas aleatorias para PostgreSQL, la API key del dispositivo y los usuarios `lider` e `integrante`, y **imprime las credenciales una sola vez**.
- `.env` no se versiona. Para configurarlo a mano, copia `.env.example` y rellena todos los valores (las contraseñas de usuario, de ≥ 12 caracteres).
- Sin `.env` completo, `docker compose` se detiene con un mensaje indicando la variable que falta.

## 4. Desplegar

```bash
docker compose up --build -d
docker compose ps          # db, backend, frontend y simulador en ejecución; backend y db «healthy»
```

El primer arranque aplica las migraciones de Flyway (`V1`–`V4`) y crea los dos usuarios con contraseñas cifradas (BCrypt).

## 5. Verificar

| Paso | Comando o acción | Resultado esperado |
|---|---|---|
| 1 | `curl -s http://localhost:8080/api/health` | `{"status":"UP",…}` |
| 2 | `curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/proyectos` | `401` (la API exige sesión) |
| 3 | Abrir `http://localhost:4200` e iniciar sesión con las credenciales de `.env` | Lista de proyectos; el `integrante` solo puede consultar |
| 4 | Pestaña «Dashboard IoT» (`http://localhost:4200/#iot`) | Valores que se actualizan cada 5 s |
| 5 | `./scripts/e2e-p07.sh` | `== Resultado: N correctas, 0 fallidas ==` |
| 6 | `./scripts/auditoria-seguridad.sh` | `0 fallidas` |

## 6. Operación

| Tarea | Comando |
|---|---|
| Ver registros | `docker compose logs -f backend` (autenticaciones fallidas y bloqueos incluidos) |
| Consultar la BD | `docker compose exec db psql -U proyectos -d proyectos` |
| Copia de seguridad | `docker compose exec -T db pg_dump -U proyectos proyectos > respaldo-$(date +%F).sql` |
| Restaurar | `docker compose exec -T db psql -U proyectos -d proyectos < respaldo.sql` |
| Cambiar la contraseña de un usuario | Cambiar `APP_…_PASSWORD` en `.env`, ejecutar `docker compose exec db psql -U proyectos -d proyectos -c "DELETE FROM usuario WHERE username='integrante';"` y `docker compose up -d backend` (se recrea con la nueva contraseña) |
| Rotar la API key del dispositivo | Cambiar `IOT_API_KEY` en `.env` y `docker compose up -d backend simulador` |
| Detener / reanudar | `docker compose stop` / `docker compose start` |
| Reinicio completo **borrando datos** | `docker compose down -v` (solo en desarrollo) |
| Habilitar Swagger (solo desarrollo) | `SWAGGER_ENABLED=true` en `.env` y `docker compose up -d backend` → `http://localhost:8080/swagger-ui.html` |

> Si cambias `DB_PASSWORD` después del primer arranque, el volumen de PostgreSQL conserva la contraseña anterior: cámbiala también dentro de la BD (`ALTER USER`) o recrea el volumen en desarrollo.

## 7. Producción (recomendaciones)

1. Colocar un **proxy inverso con HTTPS** (Caddy, Traefik o Nginx con certificado) delante del puerto 4200 y **no** exponer 4200 ni 8080 directamente; las credenciales Basic viajan en cada petición y requieren TLS.
2. Activar `Strict-Transport-Security` (línea comentada en `proyectos-frontend/nginx.conf`).
3. Guardar los secretos en un gestor (Docker secrets, Vault, variables del orquestador) y no en archivos del servidor.
4. Fijar las imágenes por *digest* y programar `auditoria-seguridad.sh` en la integración continua.
5. Respaldar la BD de forma periódica y probar la restauración.

## 8. Reversión

```bash
git log --oneline -n 10                 # elegir la versión estable anterior
git checkout <commit-o-etiqueta>
docker compose up -d --build
```

Las migraciones de Flyway solo avanzan. Si una migración nueva ya se aplicó, restaura el respaldo previo (§6) antes de volver a una versión anterior del código.

## 9. Lista de comprobación antes de entregar o desplegar

- [ ] `.env` existe, **no** está en `git status` y no se compartió por chat ni por correo.
- [ ] `./scripts/auditoria-seguridad.sh` → 0 fallidas.
- [ ] `./scripts/e2e-p07.sh` → 0 fallidas.
- [ ] `mvn verify` y `npx jest --ci` en verde.
- [ ] La contraseña antigua de P02 se rotó (DEF-01).
