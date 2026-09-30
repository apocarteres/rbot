---
id: RBOT-OPS-013
type: ticket
status: backlog
scope: deployment, operations
authority: supporting
priority: P1
release: unassigned
related: RBOT-OPS-004, RBOT-OPS-005, RBOT-OPS-007, RBOT-OPS-008, RBOT-OPS-011, REQ-TICKETS
---

# Проверка рабочей среды после раската выпуска 2026.09.1

## Проблема

Часть критериев приёмки обязательств выпуска `2026.09.1` проверяется только на рабочей среде после раската тега (`REQ-TICKETS-021`): принятие базы Flyway журналом Liquibase, раскладка nginx, архив кусков клиента, сверка развёрнутого.

## Подтверждение

Критерии задач RBOT-OPS-004, RBOT-OPS-005, RBOT-OPS-007, RBOT-OPS-008, RBOT-OPS-011; на QA они проверены 2026-10-01.

## Последствия при сохранении текущего поведения

Без наблюдения на рабочей среде выполненность обязательств держится на QA, а рабочая среда отличается от него хостом, nginx и базой.

## Требуется

Раскат тега `2026.09.1` командой `scripts/deploy.sh --env production` и наблюдение.

## Критерии приёмки

- Раскат прошёл: манифест записан, `conventions deployed` по каждой составляющей, `conventions health` к экземпляру — UP.
- База рабочей среды принята: в `databasechangelog` четыре набора `platform-auth`, учётные записи на месте; `liquibase status` не называет невыполненных наборов, повторный `migrate` ничего не меняет.
- `conventions unknown` проходит на `https://admin.yanapaderina.com` и `https://bot.yanapaderina.com`.
- `conventions static-check` проходит на обоих сайтах; архив кусков пополнен файлами прежней сборки.
- Правка настройки nginx приехала раскатом без ручного копирования: установленный `/etc/nginx/sites-available/rbot` совпадает с `deploy/nginx/rbot.conf` (`conventions deployed`).
