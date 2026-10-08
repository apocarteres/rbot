---
id: RBOT-OPS-025
type: ticket
status: done
scope: deployment, operations
authority: supporting
priority: P2
release: unassigned
related: RBOT-OPS-004
---

# Контекст Docker стенда QA — `qa`

## Проблема

Раскат на QA падал: `context "zavpn-mini": context not found`. Владелец переименовал контексты Docker: стенд на `mini` — `qa` (`ssh://admin@mini`), локальная Lima — `dev`.

## Подтверждение

`docker context ls` 2026-10-08: `default`, `dev`, `qa`; `zavpn-mini` нет. Подтверждение владельца в тот же день: «контекст переименован в qa а локальный в dev».

## Последствия при сохранении текущего поведения

`mise run deploy -- --env qa` не работает.

## Требуется

1. `scripts/deploy/environments.sh`: стенд `qa` — контекст `qa`.
2. `RUN-QA` (`docs/runbooks/qa.md`): контекст `qa` в описании и в команде очистки базы.

## Критерии приёмки

- `mise run deploy -- --env qa` поднимает стенд и проходит проверки.

## Ход работы

- 2026-10-08: контекст стенда — `qa`. Локальный запуск и интеграционные тесты не менялись: `rbot_docker_host` находит сокет Lima, на который указывает `dev`. Раскат на QA прошёл, сервис — UP.
