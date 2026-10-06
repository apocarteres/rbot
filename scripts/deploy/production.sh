#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-004, RBOT-OPS-006, RBOT-OPS-020, REQ-DEPLOYMENT-001, REQ-DEPLOYMENT-002, REQ-DEPLOYMENT-003, REQ-DEPLOYMENT-014, REQ-RELEASE-016

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
SOURCE=/opt/rbot/source
METRICS_HOST="${RBOT_METRICS_HOST:-root@metrics}"
PROMETHEUS_DIR=/opt/prometheus-3.9.1.linux-amd64
GRAFANA_DASHBOARDS=/opt/grafana-12.3.2/conf/provisioning/dashboards/rbot
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

# RBOT-OPS-020: правила оповещения и дашборд — на metrics, с проверкой promtool
if [ -z "$components" ] || [[ ",$components," == *",alert-rules,"* ]]; then
  log "правила оповещения → $METRICS_HOST"
  rules="$(git -C "$ROOT_DIR" show "$commit:monitoring/prometheus/rbot-alerts.yml" | base64)"
  tests="$(git -C "$ROOT_DIR" show "$commit:monitoring/prometheus/rbot-alerts.test.yml" | base64)"
  ssh -o BatchMode=yes "$METRICS_HOST" bash -s -- "$PROMETHEUS_DIR" "$rules" "$tests" <<'REMOTE'
set -euo pipefail
dir="$1"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
printf '%s' "$2" | base64 -d > "$work/rbot-alerts.yml"
printf '%s' "$3" | base64 -d > "$work/rbot-alerts.test.yml"
"$dir/promtool" check rules "$work/rbot-alerts.yml" > /dev/null
(cd "$work" && "$dir/promtool" test rules rbot-alerts.test.yml > /dev/null)
install -o prometheus -g prometheus -m 0644 "$work/rbot-alerts.yml" "$dir/rules/rbot-alerts.yml"
"$dir/promtool" check config "$dir/prometheus.yml" > /dev/null
systemctl kill -s HUP prometheus
REMOTE
  log "правила оповещения установлены"
fi

if [ -z "$components" ] || [[ ",$components," == *",dashboard,"* ]]; then
  log "дашборд → $METRICS_HOST"
  dashboard="$(git -C "$ROOT_DIR" show "$commit:monitoring/grafana/dashboards/rbot-service.json" | base64)"
  ssh -o BatchMode=yes "$METRICS_HOST" bash -s -- "$GRAFANA_DASHBOARDS" "$dashboard" <<'REMOTE'
set -euo pipefail
dir="$1"
work="$(mktemp)"
trap 'rm -f "$work"' EXIT
printf '%s' "$2" | base64 -d > "$work"
jq -e '.uid == "rbot-service" and (.panels | length > 0)' "$work" > /dev/null
install -d -o grafana -g grafana -m 0755 "$dir"
install -o grafana -g grafana -m 0644 "$work" "$dir/rbot-service.json"
REMOTE
  log "дашборд установлен"
fi
