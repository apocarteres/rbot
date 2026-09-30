#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-004, RBOT-OPS-006, REQ-DEPLOYMENT-015, REQ-DEPLOYMENT-019

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
conventions="$ROOT_DIR/scripts/platform/conventions.sh"

if ! parsed="$("$conventions" deploy-args --root "$ROOT_DIR" -- "$@")"; then
  exit 2
fi

environment=""
components=""
untagged=""
while IFS='=' read -r key value; do
  case "$key" in
    env) environment="$value" ;;
    components) components="$value" ;;
    untagged-reason) untagged="$value" ;;
  esac
done <<< "$parsed"

if [ -z "$environment" ]; then
  printf '%s\n' "$parsed"
  exit 0
fi

case "$environment" in
  qa)
    exec "$ROOT_DIR/scripts/deploy/stand.sh" "$environment" "$components"
    ;;
  production)
    exec "$ROOT_DIR/scripts/deploy/production.sh" "$components" "$untagged"
    ;;
  *)
    printf 'среда %s объявлена, но вход её не разворачивает\n' "$environment" >&2
    exit 2
    ;;
esac
