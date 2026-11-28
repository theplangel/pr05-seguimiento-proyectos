#!/usr/bin/env bash
# P07 — Revisiones estáticas de seguridad del repositorio (secretos, dependencias).
# Uso:  ./scripts/auditoria-seguridad.sh | tee docs/evidencia-auditoria.txt
# Requiere bash y grep; usa git y npm si están disponibles.
set -u
cd "$(dirname "$0")/.."
OK=0; FAIL=0
resultado() { if [ "$2" -eq 0 ]; then OK=$((OK+1)); printf 'PASS  %s\n' "$1"; else FAIL=$((FAIL+1)); printf 'FAIL  %s %s\n' "$1" "${3:-}"; fi; }
ARCHIVOS() { if git rev-parse --git-dir >/dev/null 2>&1; then git ls-files; else find . -type f -not -path '*/node_modules/*' -not -path '*/.git/*' -not -path '*/target/*' -not -path '*/dist/*' -not -path '*/.angular/*'; fi; }

echo "== Auditoría de seguridad ($(date -u +%FT%TZ)) =="
echo "--- Secretos"
if git rev-parse --git-dir >/dev/null 2>&1; then
  [ -z "$(git ls-files | grep -E '(^|/)\.env$')" ]; resultado ".env NO está versionado" $?
else echo "SKIP  .env versionado (no es un repositorio Git)"; fi
grep -q '^\.env$' .gitignore; resultado ".env está en .gitignore" $?
[ -f .env.example ] && ! grep -Eq '^[A-Z_]*(PASSWORD|KEY)=.+$' .env.example; resultado ".env.example no contiene valores secretos" $?
ARCHIVOS | grep -vE '(auditoria-seguridad\.sh|defectos-p07\.md|seguridad-owasp-p07\.md)$' \
  | xargs grep -lEI 'proyectos_local_2026|iot_local_2026|seguimiento_local_2026|970533' 2>/dev/null > /tmp/hallazgos_secretos || true
[ ! -s /tmp/hallazgos_secretos ]; resultado "Sin contraseñas por defecto ni credenciales conocidas en archivos del repositorio" $? "$(tr '\n' ' ' < /tmp/hallazgos_secretos)"
ARCHIVOS | grep -vE '(auditoria-seguridad\.sh|defectos-p07\.md)$' \
  | xargs grep -lEI '(password|passwd|secret|api[_-]?key)[[:space:]]*[:=][[:space:]]*["'"'"'][^"'"'"'$ ]{8,}["'"'"']' 2>/dev/null \
  | grep -vE '\.spec\.ts$|Test\.java$|\.md$|/test/' > /tmp/hallazgos_literales || true
[ ! -s /tmp/hallazgos_literales ]; resultado "Sin literales de contraseña o clave en el código de producción" $? "$(tr '\n' ' ' < /tmp/hallazgos_literales)"
if git rev-parse --git-dir >/dev/null 2>&1; then
  H="$(git log --all --oneline -S'970533' 2>/dev/null | head -3)"
  [ -z "$H" ]; resultado "El historial de Git no contiene la contraseña antigua de P02 (si falla: rotarla)" $? "$H"
fi

echo "--- Configuración"
grep -q 'ddl-auto=validate' proyectos-backend/src/main/resources/application.properties; resultado "Hibernate solo valida el esquema (no lo modifica)" $?
grep -q 'include-stacktrace=never' proyectos-backend/src/main/resources/application.properties; resultado "Sin trazas en las respuestas de error" $?
grep -q 'springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}' proyectos-backend/src/main/resources/application.properties; resultado "Swagger desactivado por defecto" $?
! grep -qE '^\s+- "?(5433|5432):' docker-compose.yml; resultado "docker-compose no publica el puerto de PostgreSQL" $?
grep -q '127.0.0.1:8080:8080' docker-compose.yml; resultado "El backend solo escucha en 127.0.0.1" $?
grep -q 'DB_PASSWORD:?' docker-compose.yml; resultado "docker-compose exige DB_PASSWORD (sin valor por defecto)" $?
grep -q 'Content-Security-Policy' proyectos-frontend/nginx.conf; resultado "Nginx envía Content-Security-Policy" $?
grep -q 'USER app' proyectos-backend/Dockerfile; resultado "El contenedor del backend corre sin root" $?

echo "--- Dependencias"
if command -v npm >/dev/null 2>&1; then
  (cd proyectos-frontend && npm audit --omit=dev --audit-level=critical >/tmp/npm_audit.txt 2>&1); R=$?
  resultado "npm audit (producción): sin vulnerabilidades CRÍTICAS" $R "(ver /tmp/npm_audit.txt)"
  RESUMEN="$(cd proyectos-frontend && npm audit --omit=dev 2>/dev/null | grep -E '^[0-9]+ vulnerabilities' || echo 'sin hallazgos')"
  echo "AVISO npm audit: $RESUMEN"
  echo "        Hallazgos altos/moderados de Angular 18: sin parche en la rama 18 (requiere salto de versión mayor)."
  echo "        Tratados como RIESGO ACEPTADO documentado: ver DEF-07 en docs/defectos-p07.md"
else echo "SKIP  npm audit (npm no disponible)"; fi
echo "        Java: mvn -q org.owasp:dependency-check-maven:check   (opcional; requiere descargar la base NVD)"

echo "== Resultado: $OK correctas, $FAIL fallidas =="
[ "$FAIL" -eq 0 ]
