#!/usr/bin/env bash
# Prueba extremo a extremo de la fuente IoT (P06) contra la API real con PostgreSQL detrás.
# Uso:   docker compose stop simulador        # para que el simulador no mezcle lecturas con la prueba
#        ./scripts/smoke-iot.sh [BASE_URL]    # por defecto http://localhost:8080
#        RAPIDO=1 ./scripts/smoke-iot.sh      # omite la espera de ~25 s de la prueba de desconexión
#        docker compose start simulador       # al terminar
# Guarda la evidencia:  ./scripts/smoke-iot.sh | tee docs/evidencia-smoke-iot.txt
# Requiere bash, curl y date de GNU (Linux, WSL o Git Bash).
set -u
cd "$(dirname "$0")/.."
# P07: credenciales desde .env (generado con scripts/generar-env.sh) o variables de entorno.
[ -f .env ] && { set -a; . ./.env; set +a; }
BASE="${1:-http://localhost:8080}"
FRONT="${FRONTEND:-http://localhost:4200}"
KEY="${IOT_API_KEY:-}"
USUARIO="${APP_INTEGRANTE_USUARIO:-integrante}"; CLAVE_USUARIO="${APP_INTEGRANTE_PASSWORD:-}"
[ -n "$KEY" ] && [ -n "$CLAVE_USUARIO" ] || { echo "Faltan IOT_API_KEY y APP_INTEGRANTE_PASSWORD (define .env con ./scripts/generar-env.sh)"; exit 2; }
DEV="sensor-lab-01"
OK=0; FAIL=0
CUERPO=/tmp/smoke_iot_body

ahora()  { date -u +%Y-%m-%dT%H:%M:%SZ; }
ahora_mas()  { date -u -d "$1" +%Y-%m-%dT%H:%M:%SZ; }

# resultado <descripción> <condición: 0 = cumple> [detalle]
resultado() {
  if [ "$2" -eq 0 ]; then OK=$((OK+1)); printf 'PASS  %s\n' "$1"
  else FAIL=$((FAIL+1)); printf 'FAIL  %s %s\n' "$1" "${3:-}"; fi
}

# http <método> <ruta> [cuerpo] [clave]   -> deja el código en $CODIGO y el cuerpo en $CUERPO
http() {
  local metodo="$1" ruta="$2" cuerpo="${3:-}" clave="${4-$KEY}"
  local args=(-s -o "$CUERPO" -w '%{http_code}' -X "$metodo" "$BASE$ruta")
  [ -n "$cuerpo" ] && args+=(-H 'Content-Type: application/json' -d "$cuerpo")
  if [ "$metodo" = "POST" ] && [ "$ruta" = "/api/iot/lecturas" ]; then
    [ -n "$clave" ] && args+=(-H "X-API-Key: $clave")          # el dispositivo se autentica con su clave
  elif [ "$ruta" != "/api/health" ]; then
    args+=(-u "$USUARIO:$CLAVE_USUARIO")                        # P07: consultas con usuario (rol INTEGRANTE basta)
  fi
  CODIGO="$(curl "${args[@]}")"
}

# lectura <valor> [fechaHora]
lectura() {
  local fecha="${2:-$(ahora)}"
  printf '{"dispositivo":"%s","fechaHora":"%s","variable":"temperatura","unidad":"°C","valor":%s}' "$DEV" "$fecha" "$1"
}

# espera <código> <descripción> <método> <ruta> [cuerpo] [clave]
espera() {
  local esperado="$1" desc="$2"; shift 2
  http "$@"
  if [ "$CODIGO" = "$esperado" ]; then resultado "$desc (HTTP $CODIGO)" 0
  else resultado "$desc" 1 "esperado $esperado, obtenido $CODIGO: $(head -c 160 "$CUERPO")"; fi
  sed 's/^/        /' "$CUERPO" | head -c 300; echo
}

contiene() { # <descripción> <texto>
  grep -q "$2" "$CUERPO"; resultado "$1" $? "(no aparece '$2' en: $(head -c 160 "$CUERPO"))"
}

valor_actual() { curl -s -u "$USUARIO:$CLAVE_USUARIO" "$BASE/api/iot/estado" | grep -o '"valorActual":[-0-9.]*' | cut -d: -f2; }

# Lleva la última lectura válida a un objetivo en pasos de <= 6 (el salto máximo permitido es 8).
ir_a() {
  local objetivo="$1" actual
  actual="$(valor_actual)"
  while [ -n "$actual" ] && awk -v a="$actual" -v o="$objetivo" 'BEGIN{d=o-a; if(d<0)d=-d; exit !(d>6)}'; do
    actual="$(awk -v a="$actual" -v o="$objetivo" 'BEGIN{d=o-a; s=(d>0)?6:-6; printf "%.1f", a+s}')"
    http POST /api/iot/lecturas "$(lectura "$actual")"
    [ "$CODIGO" = "201" ] || { resultado "Aproximación gradual a $objetivo" 1 "HTTP $CODIGO"; return; }
  done
  http POST /api/iot/lecturas "$(lectura "$objetivo")"
}

echo "== Smoke IoT contra $BASE  ($(date -u +%FT%TZ)) =="
echo "   Recuerda detener el simulador:  docker compose stop simulador"

echo "--- Positivas: recepción y tratamiento"
espera 200 "Salud de la API" GET /api/health
espera 200 "Estado del dispositivo" GET /api/iot/estado
contiene "El estado identifica al dispositivo" "$DEV"
ir_a 24.0
resultado "Lectura base de 24.0 °C aceptada (HTTP $CODIGO)" $([ "$CODIGO" = 201 ] && echo 0 || echo 1)
espera 201 "Lectura normal de 24.5 °C" POST /api/iot/lecturas "$(lectura 24.5)"
contiene "La lectura normal NO es anómala" '"anomala":false'
espera 201 "Cruce del umbral de advertencia (31 °C)" POST /api/iot/lecturas "$(lectura 31.0)"
espera 200 "Alertas tras cruzar 30 °C" GET "/api/iot/alertas?limite=20"
contiene "Existe una alerta de ADVERTENCIA" '"tipo":"ADVERTENCIA"'
espera 201 "Cruce del umbral crítico (36 °C)" POST /api/iot/lecturas "$(lectura 36.0)"
espera 200 "Estado con 36 °C" GET /api/iot/estado
contiene "El nivel actual es CRITICO" '"nivel":"CRITICO"'
espera 200 "Alertas tras cruzar 35 °C" GET "/api/iot/alertas?limite=20"
contiene "Existe una alerta CRITICA" '"tipo":"CRITICA"'

echo "--- Positivas: datos anómalos (se guardan marcados y no cambian el nivel)"
espera 201 "Lectura fuera de rango (120 °C)" POST /api/iot/lecturas "$(lectura 120.0)"
contiene "Marcada FUERA_DE_RANGO" 'FUERA_DE_RANGO'
espera 201 "Salto brusco (45 °C tras 36 °C)" POST /api/iot/lecturas "$(lectura 45.0)"
contiene "Marcada SALTO_BRUSCO" 'SALTO_BRUSCO'
espera 200 "Estado tras las anomalías" GET /api/iot/estado
contiene "El valor actual sigue siendo el último válido (36)" '"valorActual":36'
espera 201 "Vuelta a valores normales (29 °C)" POST /api/iot/lecturas "$(lectura 29.0)"
espera 200 "Alertas tras normalizar" GET "/api/iot/alertas?limite=20"
contiene "Existe NORMALIZACION" 'NORMALIZACION'
contiene "Existe ANOMALIA" '"tipo":"ANOMALIA"'

echo "--- Positivas: historial y análisis"
espera 200 "Historial de la última hora" GET /api/iot/lecturas
contiene "El historial incluye la lectura anómala" 'FUERA_DE_RANGO'
DESDE="$(ahora_mas '-10 minutes')"; HASTA="$(ahora_mas '+1 minute')"
espera 200 "Historial con rango explícito" GET "/api/iot/lecturas?desde=$DESDE&hasta=$HASTA&limite=50"
espera 200 "Resumen del rango" GET "/api/iot/resumen?desde=$DESDE&hasta=$HASTA"
contiene "El resumen cuenta anomalías" '"anomalas"'

echo "--- Positivas: desconexión y reconexión"
if [ "${RAPIDO:-0}" = "1" ]; then
  echo "SKIP  Prueba de desconexión (RAPIDO=1)"
else
  echo "        esperando 25 s sin enviar datos (límite de desconexión: 15 s)..."
  sleep 25
  espera 200 "Estado sin lecturas" GET /api/iot/estado
  contiene "La conexión pasa a DESCONECTADO" '"conexion":"DESCONECTADO"'
  espera 200 "Alertas tras el silencio" GET "/api/iot/alertas?limite=20"
  contiene "Existe una alerta DESCONEXION" 'DESCONEXION'
  espera 201 "El dispositivo vuelve a enviar (29.5 °C)" POST /api/iot/lecturas "$(lectura 29.5)"
  espera 200 "Estado tras reconectar" GET /api/iot/estado
  contiene "La conexión vuelve a CONECTADO" '"conexion":"CONECTADO"'
  espera 200 "Alertas tras reconectar" GET "/api/iot/alertas?limite=20"
  contiene "Existe una alerta RECONEXION" 'RECONEXION'
fi

echo "--- Negativas: seguridad"
espera 401 "Ingesta sin API key" POST /api/iot/lecturas "$(lectura 25)" ""
espera 401 "Ingesta con API key incorrecta" POST /api/iot/lecturas "$(lectura 25)" "clave-incorrecta"
espera 401 "La autenticación precede a la validación (cuerpo vacío, sin clave)" POST /api/iot/lecturas '{}' ""
FCODE="$(curl -s -o /dev/null -m 5 -w '%{http_code}' -X POST -H 'Content-Type: application/json' -H "X-API-Key: $KEY" -d "$(lectura 25)" "$FRONT/api/iot/lecturas" 2>/dev/null || true)"
if [ "$FCODE" = "000" ] || [ -z "$FCODE" ]; then echo "SKIP  Ingesta por el puerto público del frontend (no accesible en $FRONT)"
else resultado "La ingesta NO se expone por el frontend (HTTP $FCODE)" $([ "$FCODE" = "403" ] && echo 0 || echo 1); fi

CODIGO_SIN_AUTH="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/iot/estado")"
resultado "Consulta del estado sin credenciales (HTTP $CODIGO_SIN_AUTH)" $([ "$CODIGO_SIN_AUTH" = "401" ] && echo 0 || echo 1)

echo "--- Negativas: validación"
espera 400 "Cuerpo vacío con clave válida" POST /api/iot/lecturas '{}'
espera 400 "Valor no numérico" POST /api/iot/lecturas '{"dispositivo":"sensor-lab-01","variable":"temperatura","unidad":"°C","valor":"abc"}'
espera 400 "Campo desconocido" POST /api/iot/lecturas '{"dispositivo":"sensor-lab-01","variable":"temperatura","unidad":"°C","valor":20,"x":1}'
espera 400 "Unidad distinta a la del dispositivo" POST /api/iot/lecturas '{"dispositivo":"sensor-lab-01","variable":"temperatura","unidad":"°F","valor":20}'
espera 400 "Variable distinta a la del dispositivo" POST /api/iot/lecturas '{"dispositivo":"sensor-lab-01","variable":"humedad","unidad":"°C","valor":20}'
espera 400 "Fecha-hora en el futuro" POST /api/iot/lecturas "$(lectura 25 "$(ahora_mas '+10 minutes')")"
espera 400 "Fecha-hora con más de 24 h" POST /api/iot/lecturas "$(lectura 25 "$(ahora_mas '-3 days')")"
espera 404 "Dispositivo no registrado" POST /api/iot/lecturas '{"dispositivo":"otro","variable":"temperatura","unidad":"°C","valor":20}'
espera 400 "Parámetro 'desde' con formato inválido" GET "/api/iot/lecturas?desde=no-es-fecha"
espera 400 "Rango invertido (desde > hasta)" GET "/api/iot/lecturas?desde=$HASTA&hasta=$DESDE"
espera 400 "Límite fuera de rango" GET "/api/iot/lecturas?limite=0"
espera 400 "Límite de alertas fuera de rango" GET "/api/iot/alertas?limite=999"
espera 403 "Método no declarado se deniega (DELETE en /estado)" DELETE /api/iot/estado

echo "== Resultado: $OK correctas, $FAIL fallidas =="
[ "$FAIL" -eq 0 ]
