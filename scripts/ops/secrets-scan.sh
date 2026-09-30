#!/usr/bin/env bash
set -euo pipefail

# RBOT-QUAL-001

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if ! command -v trufflehog > /dev/null 2>&1; then
  echo "trufflehog не найден. Версия закреплена в mise.toml: mise run secrets-scan" >&2
  exit 2
fi

echo "[secrets-scan] объём: история целиком и рабочее дерево"
trufflehog git "file://$ROOT_DIR" --results=verified --no-update --fail
exec trufflehog filesystem "$ROOT_DIR" \
  --exclude-paths "$ROOT_DIR/scripts/ops/secrets-scan-exclude.txt" \
  --results=verified,unknown --no-update --fail
