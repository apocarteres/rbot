#!/usr/bin/env bash
set -euo pipefail

# MVP-04, RBOT-FEAT-009, ADR-0001

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
PROXY="${RBOT_TELEGRAM_PROXY_ADMIN:-root@metrics}"
PROXY_ADDRESS="${RBOT_TELEGRAM_PROXY_ADDRESS:-65.108.55.171}"
BOT="${RBOT_TELEGRAM_BOT_USERNAME:-psy_receptionist_bot}"
KEY=/etc/rbot/telegram_proxy_ed25519

log() {
  printf '[telegram-proxy] %s\n' "$*"
}

log "ключ туннеля на $HOST"
public="$(ssh -o BatchMode=yes "$HOST" "
  set -eu
  [ -f $KEY ] || ssh-keygen -q -t ed25519 -N '' -C rbot-telegram@\$(hostname) -f $KEY
  chown rbot:rbot $KEY $KEY.pub
  chmod 0600 $KEY
  cat $KEY.pub
")"

log "прокси на $PROXY: только api.telegram.org:443, только с 127.0.0.1"
scp -q -o BatchMode=yes "$ROOT_DIR/deploy/telegram/tinyproxy.conf" "$PROXY:/tmp/rbot-tinyproxy.conf"
scp -q -o BatchMode=yes "$ROOT_DIR/deploy/telegram/tinyproxy-filter" "$PROXY:/tmp/rbot-tinyproxy-filter"
ssh -o BatchMode=yes "$PROXY" bash -s -- "$(printf "%q" "$public")" <<'REMOTE'
set -euo pipefail
public="$1"
command -v tinyproxy > /dev/null || DEBIAN_FRONTEND=noninteractive apt-get install -y -qq tinyproxy > /dev/null
install -m 0644 /tmp/rbot-tinyproxy.conf /etc/tinyproxy/tinyproxy.conf
install -m 0644 /tmp/rbot-tinyproxy-filter /etc/tinyproxy/rbot-filter
rm -f /tmp/rbot-tinyproxy.conf /tmp/rbot-tinyproxy-filter
systemctl enable --now tinyproxy > /dev/null 2>&1
systemctl restart tinyproxy
id rbot-tunnel > /dev/null 2>&1 || useradd --system --create-home --home-dir /var/lib/rbot-tunnel --shell /usr/sbin/nologin rbot-tunnel
install -d -o rbot-tunnel -g rbot-tunnel -m 0700 /var/lib/rbot-tunnel/.ssh
printf 'restrict,port-forwarding,permitopen="127.0.0.1:18888" %s\n' "$public" > /var/lib/rbot-tunnel/.ssh/authorized_keys
chown rbot-tunnel:rbot-tunnel /var/lib/rbot-tunnel/.ssh/authorized_keys
chmod 0600 /var/lib/rbot-tunnel/.ssh/authorized_keys
REMOTE

log "адрес прокси, отпечаток хоста и настройки бота на $HOST"
ssh -o BatchMode=yes "$HOST" bash -s -- "$PROXY_ADDRESS" "$BOT" <<'REMOTE'
set -euo pipefail
address="$1"
bot="$2"
ssh-keyscan -t ed25519 "$address" 2> /dev/null > /etc/rbot/telegram_proxy_known_hosts
[ -s /etc/rbot/telegram_proxy_known_hosts ] || { echo "отпечаток $address не получен" >&2; exit 1; }
chmod 0644 /etc/rbot/telegram_proxy_known_hosts
env=/etc/rbot/rbot.env
if grep -q '^TELEGRAM_BOT_TOKEN=' $env && ! grep -q '^RBOT_TELEGRAM_BOT_TOKEN=' $env; then
  sed -i 's/^TELEGRAM_BOT_TOKEN=/RBOT_TELEGRAM_BOT_TOKEN=/' $env
fi
grep -q '^RBOT_TELEGRAM_PROXY_TARGET=' $env || printf 'RBOT_TELEGRAM_PROXY_TARGET=rbot-tunnel@%s\n' "$address" >> $env
grep -q '^RBOT_TELEGRAM_PROXY=' $env || printf 'RBOT_TELEGRAM_PROXY=127.0.0.1:18888\n' >> $env
grep -q '^RBOT_TELEGRAM_BOT_USERNAME=' $env || printf 'RBOT_TELEGRAM_BOT_USERNAME=%s\n' "$bot" >> $env
grep -q '^RBOT_TELEGRAM_WEBHOOK_SECRET=' $env || printf 'RBOT_TELEGRAM_WEBHOOK_SECRET=%s\n' "$(openssl rand -hex 32)" >> $env
REMOTE
log "готово: туннель rbot-telegram-tunnel поднимет раскат; исходящие в Telegram идут через $PROXY_ADDRESS"
