#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-004, RBOT-OPS-007, REQ-DEPLOYMENT-020

site="${1:?нужен файл сайта}"
context="${2:-http}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
case "$context" in
  http) body="include $site;" ;;
  server) body="server { listen 127.0.0.1:65000; include $site; }" ;;
  *) echo "Контекст: http или server" >&2; exit 2 ;;
esac
cat > "$work/nginx.conf" <<CONF
pid $work/nginx.pid;
error_log stderr;
events {}
http {
  include /etc/nginx/mime.types;
  $body
}
CONF
nginx -t -q -p "$work" -c "$work/nginx.conf"
