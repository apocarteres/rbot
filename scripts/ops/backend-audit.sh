#!/usr/bin/env bash
set -euo pipefail

# RBOT-QUAL-001

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if ! command -v grype > /dev/null 2>&1; then
  echo "grype не найден. Версия закреплена в mise.toml: mise run backend-audit" >&2
  exit 2
fi

shopt -s nullglob
ARTIFACTS=(target/*.jar)
shopt -u nullglob

if [[ ${#ARTIFACTS[@]} -ne 1 ]]; then
  echo "В target должен быть ровно один артефакт, найдено ${#ARTIFACTS[@]}: ./mvnw clean && mise run backend-build" >&2
  exit 2
fi

echo "[backend-audit] проверяется ${ARTIFACTS[0]}"
exec grype "${ARTIFACTS[0]}" --fail-on high --output table
