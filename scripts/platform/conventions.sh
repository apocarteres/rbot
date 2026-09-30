#!/usr/bin/env bash
set -euo pipefail

# REQ-QUALITY-005, MVP-01

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BIN="$ROOT_DIR/node_modules/.bin/conventions"

if [ ! -x "$BIN" ]; then
  cat >&2 <<'MSG'
Пакет правил ядра не установлен: node_modules/.bin/conventions отсутствует.
Установка: mise run platform-install
Подробности: docs/runbooks/local-run.md
MSG
  exit 2
fi

exec "$BIN" "$@"
