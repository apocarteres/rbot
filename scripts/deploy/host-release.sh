#!/usr/bin/env bash
set -euo pipefail

# MVP-01, REQ-DEPLOYMENT-002, REQ-DEPLOYMENT-004, REQ-DEPLOYMENT-005, REQ-DEPLOYMENT-006, REQ-DEPLOYMENT-007,
# REQ-DEPLOYMENT-011, REQ-DEPLOYMENT-016, REQ-DEPLOYMENT-018

commit="${1:?нужен коммит}"
only="${2:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
UPSTREAM=/etc/nginx/rbot-backend.conf
RELEASE=/opt/rbot/releases/$commit
INSTANCES=(8091 8092)
SITES=(https://admin.yanapaderina.com https://bot.yanapaderina.com)

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
if wanted frontend; then
  mise run frontend-install
  mise run frontend-build
fi

if wanted web; then
  log "nginx и systemd"
  install -D -m 0644 deploy/nginx/rbot-headers.conf /etc/nginx/snippets/rbot-headers.conf
  install -D -m 0644 deploy/nginx/rbot.conf /etc/nginx/sites-available/rbot
  ln -sf /etc/nginx/sites-available/rbot /etc/nginx/sites-enabled/rbot
  install -D -m 0644 deploy/systemd/rbot@.service /etc/systemd/system/rbot@.service
  systemd-analyze verify /etc/systemd/system/rbot@.service
  systemctl daemon-reload
fi

current="$(sed -n 's/.*server 127\.0\.0\.1:\([0-9]*\);.*/\1/p' "$UPSTREAM" | head -n 1)"
if wanted backend; then
  next="${INSTANCES[0]}"
  [ "$current" = "${INSTANCES[0]}" ] && systemctl is-active --quiet "rbot@${INSTANCES[0]}" && next="${INSTANCES[1]}"

  log "копия базы до установки"
  dump="/var/backups/rbot/pre-${commit:0:12}-$(date -u +%Y%m%dT%H%M%SZ).dump"
  su postgres -c "pg_dump -Fc rbot" > "$dump"
  find /var/backups/rbot -name 'pre-*.dump' -type f -mtime +7 -delete

  jdk="$(mise where java)"
  runtime="/opt/rbot/jdk/$(basename "$jdk")"
  [ -x "$runtime/bin/java" ] || rsync -a --delete "$jdk/" "$runtime/"
  install -d -m 0755 "$RELEASE"
  install -m 0644 target/rbot-1.0.0.jar "$RELEASE/rbot.jar"

  log "миграции"
  systemd-run --quiet --wait --pipe --collect --uid=rbot --gid=rbot \
    -p EnvironmentFile=/etc/rbot/rbot.env -p ReadWritePaths=/var/lib/rbot \
    "$runtime/bin/java" -jar "$RELEASE/rbot.jar" migrate

  cat > "/opt/rbot/instances/$next.env" <<ENV
RBOT_JAVA=$runtime/bin/java
RBOT_JAR=$RELEASE/rbot.jar
RBOT_JVM_ARGS="-Xms${RBOT_HEAP:-256m} -Xmx${RBOT_HEAP:-256m} -XX:+ExitOnOutOfMemoryError"
ENV
  log "экземпляр $next"
  systemctl enable "rbot@$next" > /dev/null 2>&1
  systemctl restart "rbot@$next"
  healthy=0
  for _ in $(seq 1 60); do
    if curl -fsS -o /dev/null "http://127.0.0.1:$next/actuator/health/readiness" 2> /dev/null; then
      healthy=1
      break
    fi
    systemctl is-active --quiet "rbot@$next" || break
    sleep 2
  done
  if [ "$healthy" -ne 1 ]; then
    journalctl -u "rbot@$next" -n 60 --no-pager >&2
    echo "Экземпляр $next не готов: запросы остаются на прежнем" >&2
    exit 1
  fi
  printf 'upstream rbot_backend {\n  server 127.0.0.1:%s;\n}\n' "$next" > "$UPSTREAM"
fi

if wanted frontend; then
  log "клиент"
  rsync -a --delete frontend/dist/admin/browser/ /var/www/rbot/admin/
  rsync -a --delete frontend/dist/bot/browser/ /var/www/rbot/bot/
fi

nginx -t -q
systemctl reload nginx
log "nginx перечитан"

if wanted backend && [ -n "$current" ] && [ "$current" != "$next" ]; then
  systemctl disable "rbot@$current" > /dev/null 2>&1 || true
  if systemctl is-active --quiet "rbot@$current"; then
    systemctl reset-failed "rbot-retire-$current.timer" "rbot-retire-$current.service" > /dev/null 2>&1 || true
    systemctl stop "rbot-retire-$current.timer" > /dev/null 2>&1 || true
    systemd-run --quiet --unit "rbot-retire-$current" --on-active=5min systemctl stop "rbot@$current"
    log "прежний экземпляр $current остановится через 5 минут"
  fi
fi

log "проверки снаружи"
for site in "${SITES[@]}"; do
  curl -fsS -o /dev/null "$site/" || { echo "$site не отвечает" >&2; exit 1; }
  curl -fsS "$site/api/auth/session" | grep -q '"account"' || { echo "$site/api/auth/session не отвечает" >&2; exit 1; }
done
printf '%s commit=%s only=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$commit" "${only:-all}" >> /opt/rbot/journal.log
log "раскат $commit завершён"
