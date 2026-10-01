#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-009, REQ-BACKUPS-007

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
set -a
. /etc/rbot/rbot.env
set +a
STORE=(ssh -o BatchMode=yes -o IdentitiesOnly=yes -i /etc/rbot/backup_ed25519 -o UserKnownHostsFile=/etc/rbot/backup_known_hosts -o StrictHostKeyChecking=yes "$RBOT_BACKUP_TARGET")
CHECK_DB=rbot_restore_check

name="$("${STORE[@]}" latest)"
[ -n "$name" ] || { echo "в bones нет копий" >&2; exit 1; }
work="$(mktemp -d)"
trap 'rm -rf "$work"; su postgres -c "dropdb --if-exists $CHECK_DB" > /dev/null 2>&1 || true' EXIT
chmod 0755 "$work"
"${STORE[@]}" "get $name" > "$work/$name"
chmod 0644 "$work/$name"
su postgres -c "dropdb --if-exists $CHECK_DB && createdb $CHECK_DB && pg_restore --no-owner -d $CHECK_DB '$work/$name'"
accounts="$(su postgres -c "psql -d $CHECK_DB -tAc 'SELECT count(*) FROM platform_account'")"
changesets="$(su postgres -c "psql -d $CHECK_DB -tAc 'SELECT count(*) FROM databasechangelog'")"
cd "$ROOT_DIR"
mise exec -- scripts/platform/conventions.sh backups --restored --note "$name из bones восстановлена в базу $CHECK_DB на $(hostname): учётных записей $accounts, наборов изменений $changesets"
printf '[restore-check] %s: учётных записей %s, наборов изменений %s\n' "$name" "$accounts" "$changesets"
