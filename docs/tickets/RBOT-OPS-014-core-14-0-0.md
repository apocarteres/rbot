---
id: RBOT-OPS-014
type: ticket
status: in_progress
scope: dependencies, build, persistence
authority: supporting
priority: P1
release: RELEASE-2026-10-1
related: REQ-ADOPTION, RBOT-OPS-011
---

# Ядро 14.0.0

## Проблема

Вышло ядро `v14.0.0` с несовместимым изменением: `platform-persistence` приносит `spring-boot-starter-liquibase` (platform/CORE-ARC-025), автонастройка Liquibase включается у каждой службы с модулями ядра и `DataSource`.

## Подтверждение

Тег `v14.0.0` в https://github.com/apocarteres/platform, документ выпуска ядра `RELEASE-14-0-0`, раздел «Миграция».

## Последствия при сохранении текущего поведения

Проект отстаёт на мажорную версию ядра; обновление откладывается и копится.

## Требуется

1. `.platform-version`, `platform-service-parent`, пакеты npm — `14.0.0` (`REQ-ADOPTION-017`).
2. Миграция по документу выпуска: проект уже на Liquibase — своя зависимость `spring-boot-starter-liquibase` снимается, стартер приходит с ядром. Переходы по-прежнему выполняет запуск `migrate`, у веб-службы `spring.liquibase.enabled=false`.
3. Блок правил `AGENTS.md` — версии `14.0.0`.

## Критерии приёмки

- `mise run verify` проходит на `14.0.0`.
- Веб-служба не выполняет переходов при старте, `migrate` выполняет.
