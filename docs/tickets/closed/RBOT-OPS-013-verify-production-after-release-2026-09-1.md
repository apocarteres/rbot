---
id: RBOT-OPS-013
type: ticket
status: done
scope: deployment, operations
authority: supporting
priority: P1
release: RELEASE-2026-10-1
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

## Ход работы

- 2026-10-01: тег `2026.09.1` (`357ff53`) развёрнут командой `RBOT_PROD_COMMIT=2026.09.1 scripts/deploy.sh --env production`. Прежний журнал раскатов в собственном формате MVP-01 сохранён как `/opt/rbot/journal-before-cycle.log`, новый ведёт `conventions manifest`: `env=production commit=357ff539… tag=2026.09.1`.
- `conventions deployed`: «Развёрнуто собранное» по шести составляющим — `web-headers`, `web-site`, `unit-backend`, `backend`, `frontend-admin`, `frontend-bot`; настройка nginx приехала раскатом без ручного копирования. `conventions health` к `127.0.0.1:8091` — UP; прежний экземпляр 8092 остановлен таймером.
- База: `migrate` принял базу Flyway — в `databasechangelog` четыре набора `platform-auth`, учётная запись администратора на месте. Повторный `migrate`: `Run: 0, Previously run: 4` — невыполненных наборов нет, `update` ничего не меняет.
- `conventions unknown` на `https://admin.yanapaderina.com` и `https://bot.yanapaderina.com` — «Неизвестный адрес отвечает как неизвестный».
- `conventions static-check` на обоих сайтах — «Статика клиента разложена по правилам»; с `--previous /main-FSDMSN4X.js` (admin) и `--previous /chunk-FAJBF6QU.js` (bot) — куски прежней сборки отдаются из архива `/var/www/rbot/archive/<приложение>`. Предупреждение о `no-referrer` относится к `http`; сайты работают по `https`.
