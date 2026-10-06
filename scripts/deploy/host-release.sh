#!/usr/bin/env bash
set -euo pipefail

# MVP-01, RBOT-OPS-004, RBOT-OPS-007, RBOT-OPS-008, RBOT-OPS-011, REQ-DEPLOYMENT-002, REQ-DEPLOYMENT-004, REQ-DEPLOYMENT-005,
# REQ-DEPLOYMENT-006, REQ-DEPLOYMENT-007, REQ-DEPLOYMENT-011, REQ-DEPLOYMENT-016, REQ-DEPLOYMENT-018, REQ-DEPLOYMENT-020,
# REQ-DEPLOYMENT-022, REQ-DEPLOYMENT-024, REQ-DEPLOYMENT-025, REQ-DEPLOYMENT-029, RBOT-OPS-009, REQ-BACKUPS-005, REQ-BACKUPS-006, RBOT-FEAT-009

commit="${1:?нужен коммит}"
only="${2:-}"
untagged="${3:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
conventions() {
  mise exec -- "$ROOT_DIR/scripts/platform/conventions.sh" "$@"
}
UPSTREAM=/etc/nginx/rbot-backend.conf
RELEASE=/opt/rbot/releases/$commit
INSTANCES=(8091 8092)
declare -A SITES=([admin]=https://admin.yanapaderina.com [bot]=https://bot.yanapaderina.com)

log() {
  printf '[host] %s\n' "$*"
}

wanted() {
  [ -z "$only" ] || [[ ",$only," == *",$1,"* ]]
}

log "предпосылки"
[ "$(git rev-parse HEAD)" = "$commit" ] || { echo "Рабочее дерево не на $commit" >&2; exit 1; }
[ -f /etc/rbot/rbot.env ] || { echo "Нет /etc/rbot/rbot.env: mise run provision" >&2; exit 2; }
for host in admin.yanapaderina.com bot.yanapaderina.com; do
  [ -f "/etc/letsencrypt/live/$host/fullchain.pem" ] || { echo "Нет сертификата $host: mise run provision" >&2; exit 2; }
done
for tool in mise nginx pg_dump systemctl systemd-run systemd-analyze rsync; do
  command -v "$tool" > /dev/null || { echo "Нет $tool" >&2; exit 2; }
done
set -a
. /etc/rbot/rbot.env
set +a

log "сборка"
export MISE_YES=1
mise trust -q "$ROOT_DIR/mise.toml"
mise install -q
mise run platform-ensure
if wanted backend; then
  mise exec -- ./mvnw -B -ntp -q clean
  mise run backend-build
fi
if wanted frontend-admin || wanted frontend-bot; then
  mise run frontend-install
  mise run frontend-build
fi

current="$(sed -n 's/.*server 127\.0\.0\.1:\([0-9]*\);.*/\1/p' "$UPSTREAM" | head -n 1)"
next="$current"
if wanted backend; then
  next="${INSTANCES[0]}"
  [ "$current" = "${INSTANCES[0]}" ] && next="${INSTANCES[1]}"
fi

log "манифест: экземпляр $next"
install -d -m 0755 "$RELEASE"
manifest=(conventions manifest --env production --instance "$next" --file "$RELEASE/manifest.json" --journal /opt/rbot/journal.log)
[ -n "$only" ] && manifest+=(--only "$only")
[ -n "$untagged" ] && manifest+=(--untagged-reason "$untagged")
"${manifest[@]}"

install_component() {
  local name="$1" artifact="$2" target="$3" verify="$4" enable="$5"
  [ -n "$target" ] || return 0
  wanted "$name" || return 0
  if [ -n "$verify" ]; then
    local staged
    staged="$(mktemp --suffix="-$(basename "$target")")"
    cp "$artifact" "$staged"
    ${verify//\{\}/$staged}
    rm -f "$staged"
  fi
  install -D -m 0644 "$artifact" "$target"
  conventions deployed --artifact "$artifact" --installed "$target"
  if [ -n "$enable" ]; then
    systemctl daemon-reload
    systemctl enable --now "$(basename "$target")"
  fi
  log "установлено: $name → $target"
}

while IFS=$'\t' read -r name artifact target verify enable; do
  install_component "$name" "$artifact" "$target" "$verify" "$enable"
done < <(conventions components --full)
systemctl daemon-reload
ln -sf /etc/nginx/sites-available/rbot /etc/nginx/sites-enabled/rbot

if wanted backend; then
  order="$(conventions migrations --unreleased | sed -n 's/^order=//p')"
  log "копия базы до установки"
  dump="/var/backups/rbot/pre-${commit:0:12}-$(date -u +%Y%m%dT%H%M%SZ).dump"
  su postgres -c "pg_dump -Fc rbot" > "$dump"
  find /var/backups/rbot -name 'pre-*.dump' -type f -mtime +7 -delete

  jdk="$(mise where java)"
  runtime="/opt/rbot/jdk/$(basename "$jdk")"
  [ -x "$runtime/bin/java" ] || rsync -a --delete "$jdk/" "$runtime/"
  install -m 0644 target/rbot-1.0.0.jar "$RELEASE/rbot.jar"
  conventions deployed --artifact target/rbot-1.0.0.jar --installed "$RELEASE/rbot.jar"

  if [ "$order" = "downtime" ] && [ -n "$current" ]; then
    log "переход breaking: прежний экземпляр $current останавливается до переходов"
    systemctl stop "rbot@$current" || true
  fi
  log "переходы базы"
  systemd-run --quiet --wait --pipe --collect --uid=rbot --gid=rbot \
    -p EnvironmentFile=/etc/rbot/rbot.env -p ReadWritePaths=/var/lib/rbot \
    "$runtime/bin/java" -jar "$RELEASE/rbot.jar" migrate

  jvm="$(conventions jvm-args --env production --archive "/var/lib/rbot/cds/${commit:0:12}.jsa" | paste -sd' ' -)"
  cat > "/opt/rbot/instances/$next.env" <<ENV
RBOT_JAVA=$runtime/bin/java
RBOT_JAR=$RELEASE/rbot.jar
RBOT_JVM_ARGS="$jvm -Xms${RBOT_HEAP:-256m} -Xmx${RBOT_HEAP:-256m}"
ENV
  log "экземпляр $next"
  systemctl enable "rbot@$next" > /dev/null 2>&1
  systemctl restart "rbot@$next"
  for _ in $(seq 1 90); do
    curl -fsS -o /dev/null "http://127.0.0.1:$next/actuator/health" 2> /dev/null && break
    systemctl is-active --quiet "rbot@$next" || { journalctl -u "rbot@$next" -n 60 --no-pager >&2; exit 1; }
    sleep 2
  done
  conventions health --url "http://127.0.0.1:$next/actuator/health" --timeout 30

  printf 'upstream rbot_backend {\n  server 127.0.0.1:%s;\n}\n' "$next" > "$UPSTREAM"
fi

nginx -t -q
systemctl reload nginx
log "nginx: запросы идут на $next"

if wanted backend && [ -n "$current" ] && [ "$current" != "$next" ]; then
  systemctl disable "rbot@$current" > /dev/null 2>&1 || true
  if systemctl is-active --quiet "rbot@$current"; then
    systemctl reset-failed "rbot-retire-$current.timer" "rbot-retire-$current.service" > /dev/null 2>&1 || true
    systemctl stop "rbot-retire-$current.timer" > /dev/null 2>&1 || true
    systemd-run --quiet --unit "rbot-retire-$current" --on-active=10min systemctl stop "rbot@$current"
    log "прежний экземпляр $current остановится через 10 минут"
  fi
fi

declare -A previous=()
for app in admin bot; do
  wanted "frontend-$app" || continue
  served="/var/www/rbot/$app"
  archive="/var/www/rbot/archive/$app"
  install -d -m 0755 "$served" "$archive"
  log "клиент $app: прежние куски — в архив на 7 дней"
  gone="$(comm -23 \
    <(find "$served" -maxdepth 1 -type f -regextype posix-extended -regex '.*-[A-Za-z0-9_-]{8}\.(js|css)$' -printf '%f\n' | sort) \
    <(find "frontend/dist/$app/browser" -maxdepth 1 -type f -printf '%f\n' | sort) | head -n 1)"
  [ -n "$gone" ] && previous[$app]="/$gone"
  find "$served" -maxdepth 1 -type f -regextype posix-extended -regex '.*-[A-Za-z0-9_-]{8}\.(js|css)$' \
    -exec cp -n {} "$archive/" \;
  find "$archive" -type f -mtime +7 -delete
  rsync -a --delete "frontend/dist/$app/browser/" "$served/"
  conventions deployed --artifact "frontend/dist/$app/browser" --installed "$served"
done

log "проверки снаружи"
for app in admin bot; do
  conventions unknown --url "${SITES[$app]}"
  if wanted "frontend-$app"; then
    check=(conventions static-check --url "${SITES[$app]}" --component "frontend-$app")
    [ -n "${previous[$app]:-}" ] && check+=(--previous "${previous[$app]}")
    "${check[@]}"
  fi
done
log "Telegram: туннель и webhook"
systemctl restart rbot-telegram-tunnel.service > /dev/null 2>&1 || true
# RBOT-OPS-020
if wanted logs-pipeline && systemctl cat alloy.service > /dev/null 2>&1; then
  log "журналы: туннель к Loki и Alloy"
  systemctl restart rbot-logs-tunnel.service > /dev/null 2>&1 || true
  systemctl restart alloy.service
fi
"$ROOT_DIR/scripts/ops/telegram-setup.sh" || log "ОТКАЗ регистрации webhook Telegram: раскат установлен, бот без webhook"
log "копии данных"
if ! conventions backups --check; then
  log "ОТКАЗ проверки копий: раскат установлен, копии нужно чинить (REQ-BACKUPS-006)"
fi
log "раскат $commit завершён"
