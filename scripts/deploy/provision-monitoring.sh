#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-020, RBOT-OPS-024: разовая подготовка наблюдения — пароль метрик, задание Prometheus, папка Grafana, Alloy и доступ туннеля к Loki.
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

log "задание rbot-backend отдельным файлом scrape/rbot.yml и правила rbot-*.yml в Prometheus на $METRICS"
ssh -o BatchMode=yes "$METRICS" bash -s -- "$PROMETHEUS_DIR" "$(base64 < "$ROOT_DIR/monitoring/prometheus/rbot-scrape.yml")" <<'REMOTE'
set -euo pipefail
dir="$1"
config="$dir/prometheus.yml"
backup="$config.bak-rbot-$(date -u +%Y%m%dT%H%M%SZ)"
cp -a "$config" "$backup"
install -d -o prometheus -g prometheus -m 0755 "$dir/scrape"
printf '%s' "$2" | base64 -d > "$dir/scrape/rbot.yml.new"
chown prometheus:prometheus "$dir/scrape/rbot.yml.new"
mv "$dir/scrape/rbot.yml.new" "$dir/scrape/rbot.yml"
python3 - "$config" <<'PY'
import sys
path = sys.argv[1]
lines = open(path).read().split("\n")
out, skip = [], False
for line in lines:
    if line.startswith("# RBOT-OPS-020: задание опроса rbot"):
        skip = True
        continue
    if skip and (line.startswith("  - job_name:") and "rbot-backend" not in line or (line and not line.startswith(" ") and not line.startswith("#"))):
        skip = False
    if not skip:
        out.append(line)
text = "\n".join(out).rstrip("\n") + "\n"
if '"scrape/rbot.yml"' not in text:
    if "\nscrape_config_files:\n" in text:
        text = text.replace("\nscrape_config_files:\n", '\nscrape_config_files:\n  - "scrape/rbot.yml"  # RBOT-OPS-024\n', 1)
    else:
        text = text.replace("\nscrape_configs:", '\n# RBOT-OPS-024: задания проектов отдельными файлами — правки prometheus.yml их не задевают\nscrape_config_files:\n  - "scrape/rbot.yml"\n\nscrape_configs:', 1)
open(path, "w").write(text)
PY
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

log "туннелю rbot-tunnel на $METRICS — Loki 127.0.0.1:3100 и обратный проброс метрик 127.0.0.1:19100"
ssh -o BatchMode=yes "$METRICS" bash -s <<'REMOTE'
set -euo pipefail
keys=/var/lib/rbot-tunnel/.ssh/authorized_keys
grep -q 'permitopen="127.0.0.1:3100"' "$keys" \
  || sed -i 's/permitopen="127.0.0.1:18888"/permitopen="127.0.0.1:18888",permitopen="127.0.0.1:3100"/' "$keys"
grep -q 'permitlisten="127.0.0.1:19100"' "$keys" \
  || sed -i 's/permitopen="127.0.0.1:3100"/permitopen="127.0.0.1:3100",permitlisten="127.0.0.1:19100"/' "$keys"
grep -q 'permitlisten="127.0.0.1:19100"' "$keys"
REMOTE

log "Alloy на $HOST: пакет с $METRICS (apt.grafana.com из РФ отвечает 403)"
if ! ssh -o BatchMode=yes "$HOST" 'command -v alloy > /dev/null'; then
  deb="$(ssh -o BatchMode=yes "$METRICS" 'cd /tmp && apt-get download -q alloy > /dev/null 2>&1 && ls -1t /tmp/alloy_*.deb | head -n 1')"
  ssh -o BatchMode=yes "$METRICS" "cat '$deb'" | ssh -o BatchMode=yes "$HOST" 'cat > /tmp/rbot-alloy.deb && DEBIAN_FRONTEND=noninteractive dpkg -i /tmp/rbot-alloy.deb > /dev/null && rm -f /tmp/rbot-alloy.deb'
  ssh -o BatchMode=yes "$METRICS" "rm -f '$deb'"
fi
ssh -o BatchMode=yes "$HOST" 'usermod -aG systemd-journal alloy && systemctl enable alloy > /dev/null 2>&1'
log "готово: конфиг Alloy, туннель журналов, правила и дашборд доставит раскат"
