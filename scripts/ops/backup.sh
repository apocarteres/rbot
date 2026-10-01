#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-009, REQ-BACKUPS-002, REQ-BACKUPS-004

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
set -a
. /etc/rbot/rbot.env
set +a
: "${RBOT_BACKUP_TARGET:?нет RBOT_BACKUP_TARGET в /etc/rbot/rbot.env: mise run provision-backup-store}"
STORE=(ssh -o BatchMode=yes -o IdentitiesOnly=yes -i /etc/rbot/backup_ed25519 -o UserKnownHostsFile=/etc/rbot/backup_known_hosts -o StrictHostKeyChecking=yes "$RBOT_BACKUP_TARGET")

name="rbot-$(date -u +%Y%m%dT%H%M%SZ).dump"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
chmod 0700 "$work"
chown postgres "$work"
su postgres -c "pg_dump -Fc rbot" > "$work/$name"
[ -s "$work/$name" ] || { echo "копия пуста" >&2; exit 1; }
"${STORE[@]}" "put $name" < "$work/$name"
cd "$ROOT_DIR"
mise exec -- scripts/platform/conventions.sh backups --receipt database --file "$work/$name"
printf '[backup] %s отправлена в bones, хранится 5 последних\n' "$name"
