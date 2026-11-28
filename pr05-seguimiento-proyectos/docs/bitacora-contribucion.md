# Evidencia individual de contribución — P07

La evidencia combina **commits identificables**, esta **bitácora** y una **defensa breve**. Cada integrante completa su sección.

## Cómo generar la evidencia de Git

```bash
./scripts/evidencia-commits.sh | tee docs/evidencia-commits.txt
git log --author="TU NOMBRE" --oneline        # tus commits
```

Convención de commits para que la contribución sea trazable: `P07(rol): qué cambió` — por ejemplo `P07(backend): roles con Spring Security y pruebas`.
Trabaja en ramas (`p07-<tu-rol>`) y, si es posible, entra a `main` por *pull request*.

## Reparto de trabajo (rol → entregables de P07)

| Rol | Entregables de P07 | Archivos principales |
|---|---|---|
| Líder / Backend | Autenticación, roles, límite de intentos, manejo de secretos del backend, pruebas Java | `seguridad/*`, `application.properties`, `ApiKeyFilter`, `SeguridadRolesTest` |
| BD / Persistencia | Migración de usuarios, secretos de PostgreSQL, puertos, `docker-compose`, rotación de la credencial de P02 | `V4__usuarios.sql`, `docker-compose.yml`, `.env.example`, `generar-env.sh`, `db.properties.example` |
| Frontend | Login, interceptor, UI por rol, cabeceras de Nginx y CSP, pruebas Jest | `auth/*`, `core/auth.*`, `app.component.ts`, `nginx.conf` |
| QA y documentación | Lista OWASP, casos de prueba, registro de defectos, guía de despliegue, scripts E2E y de auditoría | `docs/*-p07.md`, `despliegue.md`, `e2e-p07.sh`, `auditoria-seguridad.sh` |

## Bitácora por integrante

> Copia este bloque una vez por integrante. Registra solo lo que hiciste tú, con el commit que lo respalda.

### Integrante: ______________________  ·  Rol: ______________________

| Fecha | Tarea realizada | Commit o PR | Evidencia (archivo, captura, salida de comando) |
|---|---|---|---|
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

**Defectos que encontré o corregí (IDs de `defectos-p07.md`):** ________________

**Pruebas que escribí o ejecuté (IDs de `pruebas-p07.md`):** ________________

## Guion de defensa breve (3–5 minutos por persona)

1. ¿Qué parte construiste y en qué commits está?
2. Explica un control de seguridad que implementaste (qué riesgo OWASP cubre y por qué así).
3. Muestra una prueba tuya: caso, resultado y qué defecto habría detectado.
4. ¿Qué riesgo queda abierto y cómo se mitiga? (por ejemplo DEF-07 o la falta de TLS en local)
5. ¿Cómo se despliega el sistema desde cero sin credenciales en el repositorio?
