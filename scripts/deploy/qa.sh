#!/usr/bin/env bash
set -euo pipefail

# MVP-01, RUN-QA, REQ-DEPLOYMENT-004, REQ-DEPLOYMENT-006

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export DOCKER_CONTEXT="${RBOT_QA_CONTEXT:-zavpn-mini}"
export RBOT_QA_PORT="${RBOT_QA_PORT:-4206}"
SITES=("http://qa.admin.yanapaderina.test:$RBOT_QA_PORT" "http://qa.bot.yanapaderina.test:$RBOT_QA_PORT")

log() {
  printf '[qa] %s\n' "$*"
}

cd "$ROOT_DIR"
docker version > /dev/null
log "сборка сервера и клиента"
mise run backend-build
mise run frontend-build
log "демон: $DOCKER_CONTEXT"
docker compose -f docker-compose.qa.yml up -d --build --wait postgres redis migrate backend frontend

log "проверки"
for site in "${SITES[@]}"; do
  curl -fsS -o /dev/null "$site/" || { echo "$site не отвечает" >&2; exit 1; }
  curl -fsS "$site/api/auth/session" | grep -q '"account"' || { echo "$site/api/auth/session не отвечает" >&2; exit 1; }
  log "$site"
done
