#!/usr/bin/env bash
# P07 — Evidencia individual de contribución a partir de Git.
# Uso:  ./scripts/evidencia-commits.sh | tee docs/evidencia-commits.txt
set -u
cd "$(dirname "$0")/.."
git rev-parse --git-dir >/dev/null 2>&1 || { echo "No es un repositorio Git."; exit 1; }
echo "== Contribución por autor ($(date -u +%FT%TZ)) =="
git shortlog -sne --all --no-merges
echo
echo "== Archivos y líneas por autor =="
git log --all --no-merges --format='AUTOR:%an <%ae>' --numstat | awk '
  /^AUTOR:/ { autor=$0; next }
  NF==3 && $1 ~ /^[0-9]+$/ { add[autor]+=$1; del[autor]+=$2; arch[autor" "$3]=1 }
  END { for (a in add) { n=0; for (k in arch) if (index(k,a" ")==1) n++; printf "%s  +%d -%d  archivos:%d\n", a, add[a], del[a], n } }'
echo
echo "== Últimos 40 commits =="
git log --all --no-merges -40 --format='%h %ad %an  %s' --date=short
