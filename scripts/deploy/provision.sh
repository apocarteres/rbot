#!/usr/bin/env bash
set -euo pipefail

# MVP-01, MVP-13, REQ-DEPLOYMENT-004, REQ-DEPLOYMENT-008, REQ-CONFIG

ADMIN_EMAIL="${1:?нужна почта первого администратора}"
ACME_EMAIL="${2:-}"
HOSTS=(admin.yanapaderina.com bot.yanapaderina.com)
ENV_FILE=/etc/rbot/rbot.env

log() {
  printf '[provision] %s\n' "$*"
}

secret() {
  head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 40
}

[ "$(id -u)" -eq 0 ] || { echo "Нужен root" >&2; exit 2; }
for tool in nginx psql redis-server certbot mise git rsync curl; do
  command -v "$tool" > /dev/null || { echo "Нет $tool: хост готовится provision-скриптом fileio" >&2; exit 2; }
done

log "пользователь и каталоги"
id rbot > /dev/null 2>&1 || useradd --system --home-dir /var/lib/rbot --shell /usr/sbin/nologin rbot
install -d -o root -g rbot -m 0750 /etc/rbot
install -d -o rbot -g rbot -m 0700 /var/lib/rbot /var/lib/rbot/cds
install -d -o root -g root -m 0755 /opt/rbot /opt/rbot/releases /opt/rbot/jdk /opt/rbot/instances /opt/rbot/source
install -d -o root -g root -m 0755 /var/www/rbot /var/www/rbot/admin /var/www/rbot/bot /var/www/letsencrypt
install -d -o postgres -g postgres -m 0700 /var/backups/rbot

log "файл окружения"
if [ ! -f "$ENV_FILE" ]; then
  redis_password="$(grep -h '^requirepass ' /etc/redis/redis.conf /etc/redis/*.conf 2> /dev/null | tail -n 1 | awk '{print $2}')"
  umask 027
  cat > "$ENV_FILE" <<ENV
RBOT_DB_URL=jdbc:postgresql://127.0.0.1:5432/rbot
RBOT_DB_USERNAME=rbot
RBOT_DB_PASSWORD=$(secret)
RBOT_REDIS_HOST=127.0.0.1
RBOT_REDIS_PORT=6379
RBOT_REDIS_PASSWORD=${redis_password}
RBOT_REDIS_DATABASE=1
RBOT_PUBLIC_URL=https://bot.yanapaderina.com
RBOT_HEAP=256m
PLATFORM_AUTH_ADMIN_EMAIL=${ADMIN_EMAIL}
PLATFORM_AUTH_ADMIN_PASSWORD=$(secret)
ENV
  chown root:rbot "$ENV_FILE"
  chmod 0640 "$ENV_FILE"
  log "создан $ENV_FILE с новыми секретами"
fi
set -a
. "$ENV_FILE"
set +a

log "postgres"
if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_roles WHERE rolname = 'rbot'\"" | grep -q 1; then
  su postgres -c "psql -q -c \"CREATE ROLE rbot LOGIN\""
fi
printf "ALTER ROLE rbot PASSWORD '%s';\n" "$RBOT_DB_PASSWORD" | su postgres -c "psql -q"
if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname = 'rbot'\"" | grep -q 1; then
  su postgres -c "createdb -O rbot rbot"
fi
su postgres -c "psql -q -d rbot -c 'CREATE EXTENSION IF NOT EXISTS btree_gist'"

log "сертификаты"
missing=()
for host in "${HOSTS[@]}"; do
  [ -f "/etc/letsencrypt/live/$host/fullchain.pem" ] || missing+=("$host")
done
if [ "${#missing[@]}" -gt 0 ]; then
  cat > /etc/nginx/sites-available/rbot-acme <<NGINX
server {
  listen 80;
  listen [::]:80;
  server_name ${missing[*]};
  location ^~ /.well-known/acme-challenge/ { root /var/www/letsencrypt; }
  location / { return 404; }
}
NGINX
  ln -sf /etc/nginx/sites-available/rbot-acme /etc/nginx/sites-enabled/rbot-acme
  nginx -t -q
  systemctl reload nginx
  if [ -n "$ACME_EMAIL" ]; then
    account=(--email "$ACME_EMAIL")
  else
    account=(--register-unsafely-without-email)
  fi
  for host in "${missing[@]}"; do
    certbot certonly --webroot -w /var/www/letsencrypt -d "$host" "${account[@]}" --agree-tos --non-interactive
  done
  rm -f /etc/nginx/sites-enabled/rbot-acme /etc/nginx/sites-available/rbot-acme
  nginx -t -q
  systemctl reload nginx
fi

if [ ! -f /etc/nginx/rbot-backend.conf ]; then
  printf 'upstream rbot_backend {\n  server 127.0.0.1:8091;\n}\n' > /etc/nginx/rbot-backend.conf
fi

log "кэш архивов ядра"
version="$(tr -d '[:space:]' < "$(dirname "$0")/../.platform-version" 2> /dev/null || true)"
version="${version#v}"
if [ -n "$version" ] && [ -d "/root/.cache/fileio-platform/$version" ] && [ ! -d "/root/.cache/rbot-platform/$version" ]; then
  install -d /root/.cache/rbot-platform
  cp -r "/root/.cache/fileio-platform/$version" "/root/.cache/rbot-platform/$version"
fi

log "готово: хост подготовлен, дальше mise run deploy"
