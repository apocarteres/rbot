#!/usr/bin/env bash
set -euo pipefail

# MVP-04, RBOT-FEAT-009, ADR-0002

set -a
. /etc/rbot/rbot.env
set +a
token="${RBOT_TELEGRAM_BOT_TOKEN:-}"
if [ -z "$token" ] || [ -z "${RBOT_TELEGRAM_WEBHOOK_SECRET:-}" ] || [ -z "${RBOT_TELEGRAM_PROXY:-}" ]; then
  echo "[telegram] бот не настроен: регистрация webhook пропущена"
  exit 0
fi
app="${RBOT_TELEGRAM_APP_URL:-https://bot.yanapaderina.com}"

call() {
  local method="$1" body="$2" answer
  for _ in $(seq 1 10); do
    if answer="$(printf 'url = "https://api.telegram.org/bot%s/%s"\n' "$token" "$method" \
      | curl -sS -m 15 -x "http://$RBOT_TELEGRAM_PROXY" -K - -H 'Content-Type: application/json' -d "$body" 2>&1)"; then
      if printf '%s' "$answer" | grep -q '"ok":true'; then
        echo "[telegram] $method: ok"
        return 0
      fi
      echo "[telegram] $method отказал: $(printf '%s' "$answer" | sed 's/.*"description":"\([^"]*\)".*/\1/')" >&2
      return 1
    fi
    sleep 3
  done
  echo "[telegram] $method: прокси не ответил" >&2
  return 1
}

call setWebhook "{\"url\":\"$app/api/tg/webhook\",\"secret_token\":\"$RBOT_TELEGRAM_WEBHOOK_SECRET\",\"allowed_updates\":[\"message\",\"callback_query\"]}"
call setChatMenuButton "{\"menu_button\":{\"type\":\"web_app\",\"text\":\"Записи\",\"web_app\":{\"url\":\"$app\"}}}"
call setMyCommands '{"commands":[{"command":"start","description":"Открыть запись"}]}'
