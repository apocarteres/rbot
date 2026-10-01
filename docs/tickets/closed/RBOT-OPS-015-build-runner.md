---
id: RBOT-OPS-015
type: ticket
status: done
scope: tooling, testing
authority: supporting
priority: P1
release: RELEASE-2026-10-1
related: REQ-QUALITY, RBOT-QUAL-001
---

# Сборочный контейнер: наборы и интеграционные тесты в Docker

## Проблема

Интеграционные тесты искали демон Docker на машине разработчика: Lima-машину `pulse` или контекст Docker. 2026-10-01 машина `pulse` не стартует (отказ SSH внутри ВМ), а контекст `zavpn-mini` идёт по `ssh://`, которого Testcontainers не понимает. Наборы `verify` и `backend-test-integration` на машине разработчика остановились.

## Подтверждение

`mise run backend-test-integration` 2026-10-01: «Демон Docker не найден». Проброс сокета `mini` по SSH отвергнут владельцем: тесты гоняются в Docker, как в соседних проектах (`fileio`, `scripts/build-runner`).

## Последствия при сохранении текущего поведения

`verify` и закрытие выпуска зависят от состояния Lima на машине разработчика.

## Требуется

Сборочный контейнер на демоне текущего контекста Docker: образ с `mise` и Node, исходники — git-бандлом коммита или снимка рабочего дерева, тома для Maven, npm и инструментов, сокет демона внутри для Testcontainers.

## Критерии приёмки

- `mise run build-runner-prepare` готовит контейнер `rbot-build-runner` на `zavpn-mini`.
- `mise run build-runner-run -- backend-test-integration --worktree` проходит.
- `mise run build-runner-run -- verify` переносит расписку в `target/verify/<коммит>.json`.

## Ход работы

- 2026-10-01: `scripts/build-runner/` по образцу `fileio` (`Dockerfile`, `lib.sh`, `prepare.sh`, `run.sh`), задачи `build-runner-prepare` и `build-runner-run`. `scripts/ops/docker-host.sh` внутри контейнера берёт смонтированный `/var/run/docker.sock`: CLI Docker там нет.
- `build-runner-prepare` на `zavpn-mini` (lima-docker, x86_64) — контейнер готов. `build-runner-run -- backend-test-integration --worktree` — 10 тестов (`SchemaMigrationIT`, `QaAccountsIT`, `AdminAccountsIT`) без отказов, Testcontainers на демоне `mini` с адресом `host.docker.internal`.
- Расписка `verify` через контейнер проверяется закрытием выпуска `2026.10.1`.
