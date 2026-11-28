#!/usr/bin/env python3
"""Simulador del sensor IoT (temperatura, °C) para P06. Solo biblioteca estándar.

Envía una lectura cada INTERVALO_SEG segundos a POST /api/iot/lecturas con la cabecera X-API-Key.

ESCENARIO:
  normal  -> temperatura estable ~24 °C con ruido (sin alertas).
  demo    -> ciclo reproducible que recorre TODAS las reglas del análisis:
             normal -> calentamiento (ADVERTENCIA) -> crítico (CRITICA) -> enfriamiento (NORMALIZACION)
             -> lectura fuera de rango (ANOMALIA) -> salto brusco (ANOMALIA) -> silencio (DESCONEXION)
             -> reconexión (RECONEXION) y vuelta a empezar.
"""
import json
import math
import os
import random
import signal
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

API_URL = os.environ.get("API_URL", "http://localhost:8080").rstrip("/")
API_KEY = os.environ.get("IOT_API_KEY", "")
DISPOSITIVO = os.environ.get("DISPOSITIVO", "sensor-lab-01")
VARIABLE = os.environ.get("VARIABLE", "temperatura")
UNIDAD = os.environ.get("UNIDAD", "°C")
INTERVALO = float(os.environ.get("INTERVALO_SEG", "5"))
ESCENARIO = os.environ.get("ESCENARIO", "demo").lower()
SEMILLA = os.environ.get("SEMILLA")

_detener = False


def _salir(*_):
    global _detener
    _detener = True


def ahora_iso():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def base(t):
    """Temperatura ambiente con una oscilación lenta y ruido leve (24 ± 0.7)."""
    return 24.0 + 0.5 * math.sin(t / 8.0) + random.uniform(-0.2, 0.2)


def ciclo_demo():
    """Genera (valor | None, etiqueta). None = silencio (el dispositivo no envía)."""
    t = 0
    while True:
        for _ in range(24):            # ~2 min normal
            yield base(t), "normal"; t += 1
        for i in range(6):             # sube de 26 a 32 (cruza 30 -> ADVERTENCIA)
            yield 26.0 + i * 1.2, "calentamiento"; t += 1
        for v in (36.0, 37.0, 38.0, 37.5, 36.5):   # >= 35 -> CRITICA
            yield v, "critico"; t += 1
        for i in range(6):             # baja hasta ~28 (NORMALIZACION)
            yield 35.0 - i * 1.4, "enfriamiento"; t += 1
        yield 120.0, "anomalia-fuera-de-rango"; t += 1
        yield base(t) + 3.0, "normal"; t += 1
        yield 45.0, "anomalia-salto-brusco"; t += 1
        yield base(t) + 3.0, "normal"; t += 1
        for _ in range(int(max(1, round(30 / INTERVALO)))):   # 30 s sin enviar -> DESCONEXION
            yield None, "desconectado"
        for i in range(12):            # vuelve (RECONEXION) y se estabiliza
            yield base(t) + max(0.0, 3.0 - i * 0.3), "reconexion"; t += 1


def ciclo_normal():
    t = 0
    while True:
        yield base(t), "normal"; t += 1


def enviar(valor):
    cuerpo = json.dumps({
        "dispositivo": DISPOSITIVO,
        "fechaHora": ahora_iso(),
        "variable": VARIABLE,
        "unidad": UNIDAD,
        "valor": round(valor, 2),
    }).encode("utf-8")
    req = urllib.request.Request(
        API_URL + "/api/iot/lecturas", data=cuerpo, method="POST",
        headers={"Content-Type": "application/json", "X-API-Key": API_KEY})
    with urllib.request.urlopen(req, timeout=5) as resp:
        return resp.status, json.loads(resp.read().decode("utf-8"))


def main():
    if not API_KEY:
        print("[sim] Falta IOT_API_KEY (defínela en .env); sin clave la API rechaza las lecturas", flush=True)
        return 2
    if SEMILLA:
        random.seed(int(SEMILLA))
    signal.signal(signal.SIGTERM, _salir)
    signal.signal(signal.SIGINT, _salir)
    generador = ciclo_demo() if ESCENARIO == "demo" else ciclo_normal()
    print(f"[sim] {DISPOSITIVO} -> {API_URL} cada {INTERVALO}s, escenario={ESCENARIO}", flush=True)
    for valor, etiqueta in generador:
        if _detener:
            break
        if valor is None:
            print(f"[sim] {ahora_iso()} silencio ({etiqueta})", flush=True)
        else:
            try:
                codigo, cuerpo = enviar(valor)
                marca = f" ANÓMALA({cuerpo.get('motivoAnomalia')})" if cuerpo.get("anomala") else ""
                print(f"[sim] {ahora_iso()} {valor:6.2f} {UNIDAD} [{etiqueta}] -> HTTP {codigo}{marca}", flush=True)
            except urllib.error.HTTPError as e:
                print(f"[sim] {ahora_iso()} {valor:6.2f} {UNIDAD} [{etiqueta}] -> HTTP {e.code} {e.read()[:120]!r}", flush=True)
            except (urllib.error.URLError, OSError) as e:
                print(f"[sim] {ahora_iso()} API no disponible ({e}); reintento", flush=True)
        # espera interrumpible
        fin = time.time() + INTERVALO
        while time.time() < fin and not _detener:
            time.sleep(0.2)
    print("[sim] detenido", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
