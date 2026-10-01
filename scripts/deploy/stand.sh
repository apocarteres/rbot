#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-004, RBOT-OPS-007, REQ-DEPLOYMENT-004, REQ-DEPLOYMENT-006, REQ-DEPLOYMENT-016, REQ-DEPLOYMENT-018

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/deploy/environments.sh"
conventions="$ROOT_DIR/scripts/platform/conventions.sh"

environment="${1:?нужна среда}"
components="${2:-}"
rbot_stand "$environment"

log() {
  printf '[stand:%s] %s\n' "$environment" "$*"
}

export DOCKER_CONTEXT="$RBOT_STAND_CONTEXT"
export RBOT_QA_PORT="$RBOT_STAND_PORT"
docker version > /dev/null

services=()
IFS=',' read -r -a wanted <<< "${components:-backend,frontend-admin,frontend-bot}"
for component in "${wanted[@]}"; do
  case "$component" in
    backend)
      log "сборка сервера"
      rm -rf "$ROOT_DIR/target/classes" "$ROOT_DIR"/target/rbot-*.jar
      (cd "$ROOT_DIR" && mise run backend-build)
      services+=(migrate backend)
      ;;
    frontend-admin|frontend-bot)
      log "сборка клиента"
      (cd "$ROOT_DIR" && mise run frontend-build)
      services+=(frontend)
      ;;
    web-stand)
      services+=(frontend)
      ;;
    web-site|web-headers|unit-backend|unit-backup|timer-backup)
      ;;
    *)
      printf 'составляющей %s у стенда нет: backend, frontend-admin, frontend-bot, web-stand\n' "$component" >&2
      exit 2
      ;;
  esac
done

read -r -a services <<< "$(printf '%s\n' "${services[@]}" | sort -u | tr '\n' ' ')"
log "демон: $DOCKER_CONTEXT, проект $RBOT_STAND_PROJECT"
(cd "$ROOT_DIR" && docker compose -p "$RBOT_STAND_PROJECT" -f docker-compose.qa.yml up -d --build --wait postgres redis "${services[@]}")

log "ожидание проброса порта $RBOT_STAND_PORT"
for _ in $(seq 1 24); do
  curl -fsS -o /dev/null --max-time 5 "$RBOT_STAND_ADMIN/" 2> /dev/null && break
  sleep 5
done
log "проверки"
"$conventions" health --url "$RBOT_STAND_ADMIN/actuator/health"
for site in "$RBOT_STAND_ADMIN" "$RBOT_STAND_BOT"; do
  "$conventions" unknown --url "$site"
done
log "стенд: $RBOT_STAND_ADMIN, $RBOT_STAND_BOT"
