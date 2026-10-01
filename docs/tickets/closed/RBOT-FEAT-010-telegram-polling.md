---
id: RBOT-FEAT-010
type: ticket
status: done
scope: backend, telegram, deployment
authority: supporting
priority: P1
release: unassigned
related: RBOT-FEAT-009, RBOT-OPS-017, ADR-0001
---

# Обновления Telegram опросом и прокси по IPv4

## Проблема

После раската 2026.10.4 бот не отвечает: Telegram не доставляет webhook на рабочий хост (`getWebhookInfo`: «Connection timed out»), блокировка действует в обе стороны. Кроме того, 2 из 15 запросов через прокси зависали.

## Подтверждение

Проверено 2026-10-01: с `metrics` до `api.telegram.org` по IPv6 зависают 3 из 12 запросов, по IPv4 — 0 из 10; `tinyproxy` выбирал IPv6.

## Последствия при сохранении текущего поведения

Клиент не может привязать Telegram: `/start` до службы не доходит.

## Требуется

1. Служба забирает обновления сама: `getUpdates` с ожиданием 25 с через тот же прокси; опрашивает один экземпляр по замку `JobLock`; `offset` сдвигается за последнее обработанное.
2. Раскат снимает webhook (`deleteWebhook`) в режиме опроса; webhook остаётся запасным режимом.
3. `tinyproxy` соединяется с Telegram только по IPv4 (`Bind`).

## Критерии приёмки

- `UpdatePollingTest`: без замка опроса нет; держатель обрабатывает обновления и сдвигает `offset`.
- Рабочая среда: после раската `/start` в боте получает ответ (RBOT-OPS-017).

## Ход работы

- 2026-10-01: `UpdatePolling`, `BotGateway.updates`, замок `TelegramLocks` (переход ядра `platform-job-lock:001-lock`), `RBOT_TELEGRAM_POLLING` (по умолчанию включён), `deleteWebhook` в `scripts/ops/telegram-setup.sh`, `Bind 65.108.55.171` в `deploy/telegram/tinyproxy.conf`.
