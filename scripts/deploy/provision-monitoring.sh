#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-020: разовая подготовка наблюдения — пароль метрик, задание Prometheus, папка Grafana, Alloy и доступ туннеля к Loki.
# Правила оповещений, дашборд и конфиг Alloy доставляет раскат (scripts/deploy/production.sh, host-release.sh).

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
METRICS="${RBOT_METRICS_HOST:-root@metrics}"
PROMETHEUS_DIR=/opt/prometheus-3.9.1.linux-amd64
GRAFANA_PROVISIONING=/opt/grafana-12.3.2/conf/provisioning

log() {
  printf '[monitoring] %s\n' "$*"
}

log "пароль метрик и htpasswd nginx на $HOST"
ssh -o BatchMode=yes "$HOST" bash -s <<'REMOTE'
set -euo pipefail
password=/etc/rbot/metrics.password
if [ ! -s "$password" ]; then
  (umask 077 && openssl rand -hex 24 > "$password")
fi
chmod 0600 "$password"
staged="$(mktemp)"
printf 'prometheus:%s\n' "$(openssl passwd -apr1 -in "$password")" > "$staged"
install -o root -g www-data -m 0640 "$staged" /etc/nginx/rbot-metrics.htpasswd
rm -f "$staged"
REMOTE

log "пароль метрик → $METRICS (трубой ssh, без вывода)"
ssh -o BatchMode=yes "$HOST" 'cat /etc/rbot/metrics.password' \
  | ssh -o BatchMode=yes "$METRICS" 'umask 077 && cat > /etc/prometheus/rbot-metrics.password.new && chown root:prometheus /etc/prometheus/rbot-metrics.password.new && chmod 0640 /etc/prometheus/rbot-metrics.password.new && mv /etc/prometheus/rbot-metrics.password.new /etc/prometheus/rbot-metrics.password'

log "задание rbot-backend и правила rbot-*.yml в Prometheus на $METRICS"
ssh -o BatchMode=yes "$METRICS" bash -s -- "$PROMETHEUS_DIR" "$(base64 < "$ROOT_DIR/monitoring/prometheus/rbot-scrape.yml")" <<'REMOTE'
set -euo pipefail
dir="$1"
config="$dir/prometheus.yml"
backup="$config.bak-rbot-$(date -u +%Y%m%dT%H%M%SZ)"
cp -a "$config" "$backup"
if ! grep -q 'job_name: "rbot-backend"' "$config"; then
  printf '%s' "$2" | base64 -d >> "$config"
fi
if ! grep -q 'rules/rbot-\*.yml' "$config"; then
  sed -i '/^rule_files:/a\  - "rules/rbot-*.yml"  # RBOT-OPS-020' "$config"
fi
if ! "$dir/promtool" check config "$config" > /dev/null; then
  cp -a "$backup" "$config"
  echo "prometheus.yml не прошёл проверку: восстановлен прежний" >&2
  exit 1
fi
systemctl kill -s HUP prometheus
REMOTE

log "папка rbot в Grafana на $METRICS"
ssh -o BatchMode=yes "$METRICS" bash -s -- "$GRAFANA_PROVISIONING" "$(base64 < "$ROOT_DIR/monitoring/grafana/rbot.yml")" <<'REMOTE'
set -euo pipefail
dir="$1"
staged="$(mktemp)"
printf '%s' "$2" | base64 -d > "$staged"
install -d -o grafana -g grafana -m 0755 "$dir/dashboards/rbot"
if ! cmp -s "$staged" "$dir/dashboards/rbot.yml"; then
  install -o grafana -g grafana -m 0644 "$staged" "$dir/dashboards/rbot.yml"
  systemctl restart grafana
fi
rm -f "$staged"
REMOTE

log "туннелю rbot-tunnel на $METRICS — доступ к Loki 127.0.0.1:3100"
ssh -o BatchMode=yes "$METRICS" bash -s <<'REMOTE'
set -euo pipefail
keys=/var/lib/rbot-tunnel/.ssh/authorized_keys
grep -q 'permitopen="127.0.0.1:3100"' "$keys" \
  || sed -i 's/permitopen="127.0.0.1:18888"/permitopen="127.0.0.1:18888",permitopen="127.0.0.1:3100"/' "$keys"
grep -q 'permitopen="127.0.0.1:3100"' "$keys"
REMOTE

log "Alloy на $HOST"
ssh -o BatchMode=yes "$HOST" bash -s <<'REMOTE'
set -euo pipefail
if ! command -v alloy > /dev/null; then
  install -d -m 0755 /etc/apt/keyrings
  [ -s /etc/apt/keyrings/grafana.asc ] || curl -fsSL https://apt.grafana.com/gpg.key -o /etc/apt/keyrings/grafana.asc
  echo "deb [signed-by=/etc/apt/keyrings/grafana.asc] https://apt.grafana.com stable main" > /etc/apt/sources.list.d/grafana.list
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq alloy > /dev/null
fi
usermod -aG systemd-journal alloy
systemctl enable alloy > /dev/null 2>&1
REMOTE
log "готово: конфиг Alloy, туннель журналов, правила и дашборд доставит раскат"
