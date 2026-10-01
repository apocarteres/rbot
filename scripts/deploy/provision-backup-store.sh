#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-009, REQ-BACKUPS-003

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
STORE="${RBOT_BACKUP_STORE_ADMIN:-root@bones}"
STORE_ADDRESS="${RBOT_BACKUP_STORE_ADDRESS:-176.12.65.175}"
KEY=/etc/rbot/backup_ed25519

log() {
  printf '[backup-store] %s\n' "$*"
}

log "ключ копий на $HOST"
public="$(ssh -o BatchMode=yes "$HOST" "
  set -eu
  [ -f $KEY ] || ssh-keygen -q -t ed25519 -N '' -C rbot-backup@\$(hostname) -f $KEY
  chmod 0600 $KEY
  cat $KEY.pub
")"

log "получатель на $STORE"
scp -q -o BatchMode=yes "$ROOT_DIR/deploy/backup/rbot-backup-store" "$STORE:/tmp/rbot-backup-store"
ssh -o BatchMode=yes "$STORE" bash -s -- "$(printf "%q" "$public")" <<'REMOTE'
set -euo pipefail
public="$1"
id rbot-backup > /dev/null 2>&1 || useradd --system --home-dir /srv/backups/rbot --shell /bin/sh rbot-backup
install -d -o rbot-backup -g rbot-backup -m 0700 /srv/backups/rbot /srv/backups/rbot/.ssh
install -m 0755 /tmp/rbot-backup-store /usr/local/bin/rbot-backup-store
rm -f /tmp/rbot-backup-store
printf 'restrict,command="/usr/local/bin/rbot-backup-store" %s\n' "$public" > /srv/backups/rbot/.ssh/authorized_keys
chown rbot-backup:rbot-backup /srv/backups/rbot/.ssh/authorized_keys
chmod 0600 /srv/backups/rbot/.ssh/authorized_keys
REMOTE

log "адрес хранилища и отпечаток хоста на $HOST"
ssh -o BatchMode=yes "$HOST" bash -s -- "$STORE_ADDRESS" <<'REMOTE'
set -euo pipefail
address="$1"
ssh-keyscan -t ed25519 "$address" 2> /dev/null > /etc/rbot/backup_known_hosts
[ -s /etc/rbot/backup_known_hosts ] || { echo "отпечаток $address не получен" >&2; exit 1; }
chmod 0644 /etc/rbot/backup_known_hosts
grep -q '^RBOT_BACKUP_TARGET=' /etc/rbot/rbot.env || printf 'RBOT_BACKUP_TARGET=rbot-backup@%s\n' "$address" >> /etc/rbot/rbot.env
REMOTE
log "готово: хранилище bones принимает копии только командами put, get, latest, list"
