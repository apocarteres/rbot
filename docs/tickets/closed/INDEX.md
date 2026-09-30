---
id: IDX-TICKETS-CLOSED
type: index
status: active
scope: planning
authority: navigation
---

# Закрытые задачи

Сгенерировано командой `mise run tickets-index`. Вручную не редактировать.

[Открытые задачи](../INDEX.md) · [Планы функций](../features/INDEX.md)

Правила ведения задач — `REQ-TICKETS` в поставке пакета правил.

Всего: 9. Включены самостоятельные задачи и этапы планов функций.

| Задача | Приоритет | Статус | Выпуск | Области |
|---|---|---|---|---|
| [Java-код разделён на модули, граф связей ациклический](RBOT-ARC-001-adopt-java-modules.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, java, architecture |
| [Каждое модальное окно явно решает, что делать по Escape](RBOT-ARC-002-adopt-modal-escape.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно с удалённым действием закрывается после успеха, каждая кнопка окна объявляет своё действие](RBOT-ARC-003-adopt-modal-actions.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно не закрывается во время вызова, фон решает явно, отказ показывается в окне](RBOT-ARC-004-adopt-modal-busy-backdrop.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Доступ к данным: без ORM, простой DAO, транзакция в прикладном слое](RBOT-DATA-001-adopt-data-access.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, java, persistence |
| [Проверка проводки входит в набор check](RBOT-QUAL-003-adopt-wiring-check.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | quality, tooling |
| [Ошибки API отдаются одним контрактом ProblemDetail](RBOT-API-001-adopt-web-errors.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, api |
| [Каждый блок @defer объявляет @error](RBOT-ARC-006-adopt-defer-error.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Размер сборки клиента держит проверка](RBOT-QUAL-004-adopt-bundle-budgets.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend, build |
