#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-004, RBOT-OPS-006, REQ-DEPLOYMENT-001, REQ-DEPLOYMENT-002, REQ-DEPLOYMENT-003, REQ-DEPLOYMENT-014, REQ-RELEASE-016

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
SOURCE=/opt/rbot/source
components="${1:-}"
untagged="${2:-}"

log() {
  printf '[production] %s\n' "$*"
}

commit="$(git -C "$ROOT_DIR" rev-parse "${RBOT_PROD_COMMIT:-HEAD}^{commit}")"
tag="$(git -C "$ROOT_DIR" tag --points-at "$commit" | head -n 1)"
if [ -z "$tag" ] && [ -z "$untagged" ]; then
  echo "Коммит ${commit:0:12} не помечен тегом выпуска. Разворачивается тег; исключение — --untagged-reason \"<причина>\"" >&2
  exit 2
fi
log "хост $HOST, коммит $commit${tag:+, тег $tag}"

ssh -o BatchMode=yes "$HOST" 'test -f /etc/rbot/rbot.env' \
  || { echo "На $HOST нет /etc/rbot/rbot.env: это не рабочая машина rbot или она не подготовлена (mise run provision)" >&2; exit 2; }

log "передаю коммит"
bundle="$(mktemp)"
trap 'rm -f "$bundle"' EXIT
git -C "$ROOT_DIR" update-ref refs/deploy/production "$commit"
git -C "$ROOT_DIR" bundle create "$bundle" refs/deploy/production --tags 2> /dev/null
scp -q -o BatchMode=yes "$bundle" "$HOST:/opt/rbot/incoming.bundle"

ssh -o BatchMode=yes "$HOST" bash -s -- "$commit" <<'REMOTE'
set -euo pipefail
commit="$1"
cd /opt/rbot/source
[ -d .git ] || git init -q
git fetch -q --force --tags /opt/rbot/incoming.bundle refs/deploy/production:refs/deploy/production
git checkout -q --force --detach "$commit"
git clean -q -fdx -e .platform/ -e node_modules/ -e frontend/node_modules/
[ "$(git rev-parse HEAD)" = "$commit" ] || { echo "На хосте не тот коммит" >&2; exit 1; }
rm -f /opt/rbot/incoming.bundle
REMOTE

log "раскат на хосте"
ssh -o BatchMode=yes "$HOST" "cd $SOURCE && scripts/deploy/host-release.sh '$commit' '$components' $(printf '%q' "$untagged")"
