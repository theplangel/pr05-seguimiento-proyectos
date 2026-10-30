#!/usr/bin/env bash
# Prueba de humo extremo a extremo contra la API real (con PostgreSQL detrás).
# Uso:  ./scripts/smoke-test.sh [BASE_URL]      (por defecto http://localhost:8080)
# Con el stack en Docker también funciona vía frontend: ./scripts/smoke-test.sh http://localhost:4200
# Guarda la salida como evidencia:  ./scripts/smoke-test.sh | tee docs/evidencia-smoke.txt
set -u
BASE="${1:-http://localhost:8080}"
OK=0; FAIL=0
SUFIJO="$(date +%s)"

# check <descripción> <código_esperado> <método> <ruta> [cuerpo_json]
check() {
  local desc="$1" esperado="$2" metodo="$3" ruta="$4" cuerpo="${5:-}"
  local args=(-s -o /tmp/smoke_body -w '%{http_code}' -X "$metodo" "$BASE$ruta")
  [ -n "$cuerpo" ] && args+=(-H 'Content-Type: application/json' -d "$cuerpo")
  local real; real="$(curl "${args[@]}")"
  if [ "$real" = "$esperado" ]; then OK=$((OK+1)); printf 'PASS  %-62s HTTP %s\n' "$desc" "$real"
  else FAIL=$((FAIL+1)); printf 'FAIL  %-62s esperado %s, obtenido %s\n' "$desc" "$esperado" "$real"; fi
  sed 's/^/        /' /tmp/smoke_body | head -c 400; echo
}

echo "== Smoke test contra $BASE  ($(date -u +%FT%TZ)) =="
echo "--- Positivas"
check "Salud"                                   200 GET   /api/health
check "Listar proyectos (datos de prueba)"      200 GET   /api/proyectos
check "Filtrar por estado=ENTREGADO"            200 GET   "/api/proyectos?estado=ENTREGADO"
check "Consultar proyecto id=1"                 200 GET   /api/proyectos/1
NUEVO="{\"titulo\":\"Smoke $SUFIJO\",\"descripcion\":\"Prueba E2E\",\"integrantes\":\"Equipo\",\"asesor\":\"Dr. X\"}"
check "Crear proyecto (queda REGISTRADO)"       201 POST  /api/proyectos "$NUEVO"
ID="$(grep -o '"id":[0-9]*' /tmp/smoke_body | head -1 | cut -d: -f2)"
check "REGISTRADO -> EN_PROGRESO"               200 PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_PROGRESO"}'
check "EN_PROGRESO -> EN_REVISION"              200 PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_REVISION"}'
check "EN_REVISION -> ENTREGADO"                200 PATCH "/api/proyectos/$ID/estado" '{"estado":"ENTREGADO"}'
check "Persistencia: el proyecto sigue ENTREGADO" 200 GET "/api/proyectos/$ID"
echo "--- Negativas"
check "Crear con campos vacíos"                 400 POST  /api/proyectos '{"titulo":" ","descripcion":"","integrantes":"A","asesor":"B"}'
check "Crear con JSON mal formado"              400 POST  /api/proyectos '{titulo:'
check "Crear con campo desconocido"             400 POST  /api/proyectos '{"titulo":"t","descripcion":"d","integrantes":"i","asesor":"a","x":1}'
check "Crear con título duplicado"              409 POST  /api/proyectos "$NUEVO"
check "Consultar id inexistente"                404 GET   /api/proyectos/999999
check "Id no numérico"                          400 GET   /api/proyectos/abc
check "Filtro de estado desconocido"            400 GET   "/api/proyectos?estado=TERMINADO"
check "Estado desconocido en PATCH"             400 PATCH "/api/proyectos/$ID/estado" '{"estado":"TERMINADO"}'
check "Transición inválida (ENTREGADO es final)" 409 PATCH "/api/proyectos/$ID/estado" '{"estado":"EN_PROGRESO"}'
check "PATCH sobre proyecto inexistente"        404 PATCH /api/proyectos/999999/estado '{"estado":"EN_PROGRESO"}'
check "Método no permitido (DELETE)"            405 DELETE /api/proyectos/1

echo "== Resultado: $OK correctas, $FAIL fallidas =="
[ "$FAIL" -eq 0 ]
