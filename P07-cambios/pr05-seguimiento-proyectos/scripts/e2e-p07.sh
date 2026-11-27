#!/usr/bin/env bash
# P07 — Pruebas funcionales y extremo a extremo de seguridad contra el sistema REAL (API + PostgreSQL + Nginx).
#   CP-01, CP-02  positivas      CN-01, CN-02, CN-03  negativas      E2E-01  flujo completo + controles de configuración
# Uso:  ./scripts/e2e-p07.sh [BASE_URL]           (por defecto http://localhost:8080; el frontend en http://localhost:4200)
#       docker compose stop simulador             (opcional: evita ruido de lecturas ajenas)
# Evidencia:  ./scripts/e2e-p07.sh | tee docs/evidencia-e2e-p07.txt
# Credenciales: se leen de .env (generado con ./scripts/generar-env.sh) o de variables de entorno. Requiere bash y curl.
set -u
cd "$(dirname "$0")/.."
[ -f .env ] && { set -a; . ./.env; set +a; }
BASE="${1:-http://localhost:8080}"
FRONT="${FRONTEND:-http://localhost:4200}"
LIDER="${APP_LIDER_USUARIO:-lider}:${APP_LIDER_PASSWORD:-}"
INTEG="${APP_INTEGRANTE_USUARIO:-integrante}:${APP_INTEGRANTE_PASSWORD:-}"
KEY="${IOT_API_KEY:-}"
{ [ -n "${APP_LIDER_PASSWORD:-}" ] && [ -n "${APP_INTEGRANTE_PASSWORD:-}" ] && [ -n "$KEY" ]; } \
  || { echo "Faltan credenciales: define .env con ./scripts/generar-env.sh"; exit 2; }
OK=0; FAIL=0; B=/tmp/e2e_p07_body; TS="$(date +%s)"

resultado() { # <descripción> <0 = cumple> [detalle]
  if [ "$2" -eq 0 ]; then OK=$((OK+1)); printf 'PASS  %s\n' "$1"; else FAIL=$((FAIL+1)); printf 'FAIL  %s %s\n' "$1" "${3:-}"; fi
}
llamar() { # <usuario:clave | -> <método> <ruta> [cuerpo]
  local cred="$1" m="$2" r="$3" c="${4:-}"
  local args=(-s -o "$B" -w '%{http_code}' -X "$m" "$BASE$r")
  [ "$cred" != "-" ] && args+=(-u "$cred")
  [ -n "$c" ] && args+=(-H 'Content-Type: application/json' -d "$c")
  CODIGO="$(curl "${args[@]}")"
}
caso() { # <id> <descripción> <código esperado> <usuario:clave | -> <método> <ruta> [cuerpo]
  local id="$1" desc="$2" esperado="$3"; shift 3
  llamar "$@"
  if [ "$CODIGO" = "$esperado" ]; then resultado "[$id] $desc (HTTP $CODIGO)" 0
  else resultado "[$id] $desc" 1 "esperado $esperado, obtenido $CODIGO: $(head -c 160 "$B")"; fi
}
contiene() { # <id> <texto> <descripción>
  grep -q "$2" "$B"; resultado "[$1] $3" $? "(no aparece '$2' en: $(head -c 160 "$B"))"
}

echo "== P07 E2E de seguridad contra $BASE  ($(date -u +%FT%TZ)) =="

echo "--- Pruebas positivas"
caso CP-01 "LIDER inicia sesión (valida credenciales)" 200 "$LIDER" GET /api/auth/yo
contiene CP-01 '"rol":"LIDER"' "La respuesta identifica el rol LIDER"
caso CP-01 "LIDER consulta los proyectos" 200 "$LIDER" GET /api/proyectos
NUEVO="{\"titulo\":\"E2E P07 $TS\",\"descripcion\":\"Prueba extremo a extremo\",\"integrantes\":\"Equipo\",\"asesor\":\"Dr. X\"}"
caso CP-02 "LIDER registra un proyecto" 201 "$LIDER" POST /api/proyectos "$NUEVO"
ID="$(grep -o '"id":[0-9]*' "$B" | head -1 | cut -d: -f2)"
caso CP-02 "LIDER avanza REGISTRADO -> EN_PROGRESO" 200 "$LIDER" PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_PROGRESO"}'
contiene CP-02 '"estado":"EN_PROGRESO"' "El estado quedó EN_PROGRESO"

echo "--- Pruebas negativas"
caso CN-01 "Consulta sin credenciales" 401 - GET /api/proyectos
caso CN-01 "Contraseña incorrecta" 401 "${LIDER%%:*}:contrasena-equivocada" GET /api/proyectos
caso CN-02 "INTEGRANTE intenta cambiar el estado" 403 "$INTEG" PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_REVISION"}'
caso CN-02 "INTEGRANTE intenta crear un proyecto" 403 "$INTEG" POST /api/proyectos "$NUEVO"
caso CN-02 "El estado NO cambió tras el intento prohibido" 200 "$INTEG" GET "/api/proyectos/$ID"
contiene CN-02 '"estado":"EN_PROGRESO"' "Sigue EN_PROGRESO"
ATACANTE="atacante-$TS"
for i in 1 2 3 4 5; do llamar "$ATACANTE:intento-$i" GET /api/proyectos; done
caso CN-03 "Fuerza bruta: el 6.º intento fallido se frena" 429 "$ATACANTE:intento-6" GET /api/proyectos
caso CN-03 "Un usuario legítimo no se ve afectado" 200 "$LIDER" GET /api/proyectos

echo "--- E2E-01: flujo completo con roles, IoT y configuración segura"
caso E2E-01 "INTEGRANTE ve el proyecto creado por el LIDER" 200 "$INTEG" GET /api/proyectos
contiene E2E-01 "E2E P07 $TS" "El listado incluye el proyecto nuevo"
caso E2E-01 "LIDER avanza EN_PROGRESO -> EN_REVISION" 200 "$LIDER" PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_REVISION"}'
caso E2E-01 "Transición inválida rechazada (EN_REVISION -> REGISTRADO)" 400 "$LIDER" PATCH "/api/proyectos/$ID/estado" '{"estado":"REGISTRADO"}'
LECTURA="{\"dispositivo\":\"sensor-lab-01\",\"variable\":\"temperatura\",\"unidad\":\"°C\",\"valor\":24.5}"
CODIGO="$(curl -s -o "$B" -w '%{http_code}' -X POST "$BASE/api/iot/lecturas" -H 'Content-Type: application/json' -H "X-API-Key: $KEY" -d "$LECTURA")"
[ "$CODIGO" = "201" ] || [ "$CODIGO" = "400" ]   # 400 solo si la prueba de salto/rango rechaza por política
resultado "[E2E-01] El dispositivo envía una lectura con su API key (HTTP $CODIGO)" $([ "$CODIGO" = "201" ] && echo 0 || echo 1) "$(head -c 160 "$B")"
CODIGO="$(curl -s -o "$B" -w '%{http_code}' -X POST "$BASE/api/iot/lecturas" -H 'Content-Type: application/json' -d "$LECTURA")"
resultado "[E2E-01] Ingesta sin API key rechazada (HTTP $CODIGO)" $([ "$CODIGO" = "401" ] && echo 0 || echo 1)
caso E2E-01 "INTEGRANTE consulta el estado IoT" 200 "$INTEG" GET /api/iot/estado
contiene E2E-01 'sensor-lab-01' "El estado IoT identifica al dispositivo"
caso E2E-01 "Estado IoT sin credenciales" 401 - GET /api/iot/estado
caso E2E-01 "Salud pública sin credenciales" 200 - GET /api/health
CODIGO="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/swagger-ui.html")"
resultado "[E2E-01] Swagger UI desactivado por defecto (HTTP $CODIGO)" $([ "$CODIGO" = "404" ] || [ "$CODIGO" = "401" ] || [ "$CODIGO" = "403" ] && echo 0 || echo 1)
caso E2E-01 "Actuator no expone 'env' (solo health)" 403 "$LIDER" GET /actuator/env
caso E2E-01 "Los errores no revelan detalles internos" 400 "$LIDER" POST /api/proyectos '{titulo:'
if grep -qiE 'exception|at mx\.uv|stacktrace|org\.springframework' "$B"; then resultado "[E2E-01] El cuerpo de error NO contiene trazas" 1 "$(head -c 200 "$B")"
else resultado "[E2E-01] El cuerpo de error NO contiene trazas" 0; fi

echo "--- E2E-01: configuración segura del frontend y la red"
CAB="$(curl -sI -m 5 "$FRONT/" 2>/dev/null || true)"
if [ -z "$CAB" ]; then echo "SKIP  Cabeceras del frontend (no accesible en $FRONT)"
else
  for h in "X-Content-Type-Options: nosniff" "X-Frame-Options: DENY" "Content-Security-Policy:" "Referrer-Policy:"; do
    echo "$CAB" | grep -qi "^$h"; resultado "[E2E-01] Frontend envía la cabecera '$h'" $?
  done
  echo "$CAB" | grep -qi '^Server: .*[0-9]\.[0-9]'; [ $? -ne 0 ]; resultado "[E2E-01] El frontend no revela la versión del servidor" $?
  CODIGO="$(curl -s -o /dev/null -w '%{http_code}' -m 5 -X POST -H 'Content-Type: application/json' -H "X-API-Key: $KEY" -d "$LECTURA" "$FRONT/api/iot/lecturas")"
  resultado "[E2E-01] La ingesta IoT NO se expone por el puerto público (HTTP $CODIGO)" $([ "$CODIGO" = "403" ] && echo 0 || echo 1)
fi
if (exec 3<>/dev/tcp/127.0.0.1/5433) 2>/dev/null; then resultado "[E2E-01] PostgreSQL no está publicado en el host (:5433)" 1 "el puerto 5433 acepta conexiones"
else resultado "[E2E-01] PostgreSQL no está publicado en el host (:5433)" 0; fi

echo "== Resultado: $OK correctas, $FAIL fallidas =="
[ "$FAIL" -eq 0 ]
