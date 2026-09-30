---
id: RBOT-OPS-012
type: ticket
status: done
scope: dependencies, build
authority: supporting
priority: P1
release: RELEASE-2026-09-1
related: REQ-ADOPTION, RBOT-OPS-011
---

# Ядро 13.7.0

## Проблема

Проект закреплён на ядре `v13.5.0`. Вышли `v13.6.0` и `v13.7.0`: переходы базы ядра поставляются журналами Liquibase (platform/CORE-ARC-023, platform/CORE-ARC-024), появилось обязательство `liquibase-migrations`.

## Подтверждение

Теги `v13.6.0` и `v13.7.0` в https://github.com/apocarteres/platform; документы выпусков ядра `RELEASE-13-6-0`, `RELEASE-13-7-0`.

## Последствия при сохранении текущего поведения

Проект отстаёт от ядра, обязательства последних версий не видны проверке.

## Требуется

1. `.platform-version`, `platform-service-parent` и пакеты npm ядра — `13.7.0` (`REQ-ADOPTION-017`).
2. Блок правил `AGENTS.md` — версии `13.7.0`.
3. Разобраны обязательства, которые принесла версия.

## Критерии приёмки

- `conventions check` показывает обязательства `13.7.0`, включая `liquibase-migrations`.
- `mise run verify` проходит на `13.7.0`.

## Ход работы

- 2026-09-30: версия поднята, ядро собрано из тега локально, блок правил обновлён. Правило `migration-tool` называет Flyway в `pom.xml` — работа задачи RBOT-OPS-011. `verify` на `13.7.0` — после переезда на Liquibase.
- 2026-10-01: `mise run verify` на `13.7.0` проходит; обязательства `13.7.0` разобраны задачами выпуска `2026.09.1`, `liquibase-migrations` исполнено RBOT-OPS-011.
