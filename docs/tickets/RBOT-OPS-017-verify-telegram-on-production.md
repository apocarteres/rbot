---
id: RBOT-OPS-017
type: ticket
status: backlog
scope: deployment, operations, telegram
authority: supporting
priority: P1
release: unassigned
related: RBOT-FEAT-009, REQ-TICKETS
---

# Проверка Telegram на рабочей среде

## Проблема

Привязку по приглашению, Mini App и исходящие сообщения через прокси проверить можно только на рабочей среде с настоящим ботом (`REQ-TICKETS-021`).

## Подтверждение

Критерии RBOT-FEAT-009.

## Последствия при сохранении текущего поведения

Telegram проверен только тестами с заглушкой Bot API.

## Требуется

Раскат выпуска с RBOT-FEAT-009 и сквозной сценарий владельца.

## Критерии приёмки

- Раскат снимает webhook и регистрирует кнопку меню: `deleteWebhook`, `setChatMenuButton` — ok; туннель `rbot-telegram-tunnel` активен; опрос держит замок `telegram-polling`.
- Владелец в кабинете приглашает себя, открывает ссылку, соглашается, бот отвечает «Готово».
- Mini App открывается из чата, запись проходит, психолог видит её в «Записях» с подписью клиента.
- Перенос психологом приходит клиенту сообщением бота.

## Ход работы

- 2026-10-01: 2026.10.4 — webhook зарегистрирован, но Telegram до хоста не достучался; исправлено RBOT-FEAT-010. 2026.10.5 — `deleteWebhook`, `setChatMenuButton`, `setMyCommands` — ok, `getWebhookInfo`: адрес пуст, очередь 0, замок `telegram-polling` продлевается экземпляром 8092. Ждёт сквозного сценария владельца.
