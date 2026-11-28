#!/usr/bin/env bash
# Genera un .env con secretos aleatorios (no reutilices los de otro entorno).
# Uso:  ./scripts/generar-env.sh            # crea .env si no existe
#       ./scripts/generar-env.sh --forzar   # sobrescribe (los usuarios ya creados en la BD conservan su contraseña antigua)
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ] && [ "${1:-}" != "--forzar" ]; then
  echo "Ya existe .env; no se modifica (usa --forzar para regenerarlo)."; exit 0
fi
secreto() { head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c "${1:-24}"; }
LIDER_PASS="$(secreto 20)"; INTEGRANTE_PASS="$(secreto 20)"
umask 077
cat > .env <<ENV
DB_PASSWORD=$(secreto 24)
IOT_API_KEY=$(secreto 32)
APP_LIDER_USUARIO=lider
APP_LIDER_PASSWORD=$LIDER_PASS
APP_INTEGRANTE_USUARIO=integrante
APP_INTEGRANTE_PASSWORD=$INTEGRANTE_PASS
ESCENARIO=demo
SWAGGER_ENABLED=false
ENV
echo ".env creado (permisos 600)."
echo "Credenciales de la aplicación (guárdalas; también están en .env):"
echo "  lider       / $LIDER_PASS"
echo "  integrante  / $INTEGRANTE_PASS"
