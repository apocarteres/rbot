#!/usr/bin/env bash
set -euo pipefail

# MVP-01, REQ-DEPLOYMENT-001, REQ-DEPLOYMENT-002, REQ-DEPLOYMENT-003

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
only=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --only) only="${2:?нужен перечень: backend,frontend,web}"; shift 2 ;;
    *) echo "Неизвестный ключ: $1" >&2; exit 2 ;;
  esac
done

log() {
  printf '[production] %s\n' "$*"
}

[ -z "$(git -C "$ROOT_DIR" status --porcelain)" ] || { echo "Рабочее дерево не чистое: разворачивается коммит" >&2; exit 2; }
commit="$(git -C "$ROOT_DIR" rev-parse "${RBOT_PROD_COMMIT:-HEAD}^{commit}")"
log "хост $HOST, коммит $commit"

ssh -o BatchMode=yes "$HOST" 'test -f /etc/rbot/rbot.env' \
  || { echo "На $HOST нет /etc/rbot/rbot.env: сначала mise run provision" >&2; exit 2; }

log "передаю коммит"
bundle="$(mktemp)"
trap 'rm -f "$bundle"' EXIT
git -C "$ROOT_DIR" update-ref refs/deploy/production "$commit"
git -C "$ROOT_DIR" bundle create "$bundle" refs/deploy/production 2> /dev/null
scp -q -o BatchMode=yes "$bundle" "$HOST:/opt/rbot/incoming.bundle"

ssh -o BatchMode=yes "$HOST" bash -s -- "$commit" <<'REMOTE'
set -euo pipefail
commit="$1"
cd /opt/rbot/source
[ -d .git ] || git init -q
git fetch -q --force /opt/rbot/incoming.bundle refs/deploy/production:refs/deploy/production
git checkout -q --force --detach "$commit"
git clean -q -fdx -e .platform/ -e node_modules/ -e frontend/node_modules/
[ "$(git rev-parse HEAD)" = "$commit" ] || { echo "На хосте не тот коммит" >&2; exit 1; }
rm -f /opt/rbot/incoming.bundle
REMOTE

log "раскат на хосте"
ssh -o BatchMode=yes "$HOST" "cd /opt/rbot/source && scripts/deploy/host-release.sh '$commit' '$only'"
