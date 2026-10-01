---
id: RBOT-OPS-016
type: ticket
status: backlog
scope: deployment, operations
authority: supporting
priority: P1
release: unassigned
related: RBOT-OPS-009, MVP-02, REQ-TICKETS
---

# Проверка рабочей среды после раската выпуска 2026.10.1

## Проблема

Критерии обязательства `backups` и перехода расписания проверяются только на рабочей среде после раската тега (`REQ-TICKETS-021`).

## Подтверждение

Критерии RBOT-OPS-009 и MVP-02.

## Последствия при сохранении текущего поведения

Копии и переход `rbot:001-schedule` проверены только на QA и в тестах.

## Требуется

Раскат тега `2026.10.1` и наблюдение.

## Критерии приёмки

- Раскат прошёл, `rbot-backup.timer` включён развёртыванием.
- Первая копия снята заданием и лежит в `bones`, квитанция записана.
- Восстановление проверено `backup-restore-check.sh`, запись `--restored` есть.
- `conventions backups --check` на рабочей среде проходит.
- База рабочей среды: набор `rbot:001-schedule` применён, `migrate` повторно ничего не меняет.
