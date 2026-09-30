---
id: MVP-01
type: ticket
status: backlog
scope: backend, frontend, build
authority: supporting
priority: P1
release: unassigned
questions: resolved
related: ADR-0001
---

# Каркас службы и кабинета на ядре platform

## Проблема

Кода нет. Этапам нужны служба с модулями, база, вход психолога и набор проверок ядра.

## Подтверждение

[ADR-0001](../../../decisions/ADR-0001-service-architecture.md): одна служба на Spring Boot и Spring Modulith, кабинет на Angular, ядро platform.

## Последствия при сохранении текущего поведения

Этапы 02–13 начинать не на чем.

## Требуется

1. Служба на `platform-service-parent` и `platform-bom` закреплённой версии; скрипт установки ядра (`REQ-ADOPTION-007`, `REQ-ADOPTION-008`).
2. Пустые модули Modulith из ADR-0001 и тест `ApplicationModules.verify()`; правила `platform-arch-rules`.
3. PostgreSQL с переходами базы, `btree_gist` в первом переходе; Redis для сессий.
4. `platform-auth`: роль `PSYCHOLOGIST`, регистрация закрыта `EntryAccess`, учётная запись психолога создаётся при развёртывании; письма входа — порт `AuthLetters`.
5. `platform-time`, `platform-web-errors`, `platform-rate-limit`, `platform-notifications` подключены.
6. Кабинет на Angular: `provideAuth()`, вход, выход, смена пароля, пустая главная с колокольчиком.
7. Пакет правил `@apocarteres/project-conventions`, `.conventions.json` с `ticketPrefix`, наборы `check` и `verify`, хук `pre-push` (`REQ-ADOPTION-009`, `REQ-ADOPTION-010`).
8. Локальный запуск одной командой: инструкция в `docs/runbooks`.

## Критерии приёмки

- `mise run verify` проходит.
- Психолог входит в кабинет локально; гость получает `401` на `/api/**`, кроме открытых точек ядра.
- Тест модулей падает при добавлении цикла между модулями.

## Открытые вопросы

**Вопросы:** решены

1. **Префикс задач.** `ticketPrefix` для самостоятельных задач проекта. Предложение — `RCPT`.
   Ответ 2026-09-30: `RBOT`, по имени репозитория `apocarteres/rbot`. Объявлен в `.conventions.json`.
