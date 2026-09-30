#!/usr/bin/env bash
set -euo pipefail

# MVP-01

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${RBOT_PROD_HOST:-root@fileio.ru}"
ADMIN_EMAIL="${1:?нужна почта первого администратора: mise run provision -- <почта> [почта для Let Encrypt]}"
ACME_EMAIL="${2:-}"

ssh -o BatchMode=yes "$HOST" 'install -d -m 0755 /opt/rbot/provision'
scp -q -o BatchMode=yes "$ROOT_DIR/scripts/deploy/provision.sh" "$HOST:/opt/rbot/provision/provision.sh"
scp -q -o BatchMode=yes "$ROOT_DIR/.platform-version" "$HOST:/opt/rbot/.platform-version"
ssh -o BatchMode=yes "$HOST" "bash /opt/rbot/provision/provision.sh $(printf '%q' "$ADMIN_EMAIL") $(printf '%q' "$ACME_EMAIL")"
