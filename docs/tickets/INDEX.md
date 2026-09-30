---
id: IDX-TICKETS
type: index
status: active
scope: planning
authority: navigation
---

# Открытые задачи

Сгенерировано командой `mise run tickets-index`. Вручную не редактировать.

[Закрытые задачи](closed/INDEX.md) · [Планы функций](features/INDEX.md)

Правила ведения задач — `REQ-TICKETS` в поставке пакета правил.

Всего: 44. Включены самостоятельные задачи и этапы планов функций.

| Задача | Приоритет | Статус | Выпуск | Области |
|---|---|---|---|---|
| [Настройка среды едет развёртыванием: составляющая, место, разбор и проверки поведения](RBOT-OPS-007-adopt-environment-configuration.md) | P0 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, operations |
| [План MVP: регистратор психолога в Telegram](features/mvp/INDEX.md) | P1 | Запланирована | Не назначен | planning |
| [Каркас службы и кабинета на ядре platform](features/mvp/01-service-skeleton.md) | P1 | В работе | Не назначен | backend, frontend, build |
| [Расписание психолога и свободные слоты](features/mvp/02-work-schedule.md) | P1 | Запланирована | Не назначен | backend, frontend, data |
| [Карточки клиентов, приглашения и согласие](features/mvp/03-clients-and-consent.md) | P1 | Запланирована | Не назначен | backend, frontend, data, personal-data |
| [Канал Telegram: webhook, привязка, исходящие сообщения](features/mvp/04-telegram-channel.md) | P1 | Запланирована | Не назначен | backend, security, telegram |
| [Запись, перенос, отмена и календарь кабинета](features/mvp/05-booking.md) | P1 | Запланирована | Не назначен | backend, frontend, data |
| [Политика отмен: версии и решение](features/mvp/06-cancellation-policy.md) | P1 | Запланирована | Не назначен | backend, frontend, billing |
| [Сценарии бота: мои сессии, перенос, отмена](features/mvp/07-bot-session-scenarios.md) | P1 | Запланирована | Не назначен | telegram, backend |
| [Mini App: выбор слота и первичная консультация](features/mvp/08-mini-app.md) | P1 | Запланирована | Не назначен | frontend, backend, security, telegram |
| [Штрафы и отметка оплаты психологом](features/mvp/09-charges-and-payment-marks.md) | P1 | Запланирована | Не назначен | backend, frontend, billing |
| [Защитная эскалация тревожных сообщений](features/mvp/12-crisis-escalation.md) | P1 | Запланирована | Не назначен | backend, telegram, safety |
| [Готовность к работе: хостинг в РФ, резервные копии, документы 152-ФЗ](features/mvp/13-production-readiness.md) | P1 | Запланирована | Не назначен | operations, personal-data, legal |
| [Анкеты: шаблон, приём и раздел в кабинете](features/mvp/14-intake-forms.md) | P1 | Запланирована | Не назначен | backend, frontend, data, personal-data |
| [Java-код разделён на модули, граф связей ациклический](RBOT-ARC-001-adopt-java-modules.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | backend, java, architecture |
| [Каждое модальное окно явно решает, что делать по Escape](RBOT-ARC-002-adopt-modal-escape.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно с удалённым действием закрывается после успеха, каждая кнопка окна объявляет своё действие](RBOT-ARC-003-adopt-modal-actions.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно не закрывается во время вызова, фон решает явно, отказ показывается в окне](RBOT-ARC-004-adopt-modal-busy-backdrop.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend |
| [Клиент узнаёт о новой сборке и об устаревшем API механизмом ядра](RBOT-ARC-005-adopt-client-update.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend, backend |
| [Доступ к данным: без ORM, простой DAO, транзакция в прикладном слое](RBOT-DATA-001-adopt-data-access.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | backend, java, persistence |
| [Цикл выпуска закрывается одной командой и помечает проверенный коммит](RBOT-OPS-001-adopt-release-cycle.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | release, build, deployment |
| [Расписку о проверках пишет наблюдавший прогон, а не ручной вызов](RBOT-OPS-002-adopt-observed-receipt.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | quality, build, release |
| [Сборка потребителя идёт на Java 25, как и сборка ядра](RBOT-OPS-003-adopt-java-25.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | build, tooling |
| [Развёртывание через интерфейс ядра: среда именем, составляющие объявлены, манифест и сверка развёрнутого](RBOT-OPS-004-adopt-deployment-interface.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, build |
| [Неизвестный адрес отвечает как неизвестный: порт клиента и 404 на сервере](RBOT-OPS-005-adopt-unknown-path.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, client |
| [Написание входа в развёртывание: scripts/deploy.sh, разбор доводов ядром, среда production](RBOT-OPS-006-adopt-deploy-entry-spelling.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, build |
| [Куски прежней сборки клиента переживают раскат](RBOT-OPS-008-adopt-static-archive.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, frontend |
| [Копии данных службы объявлены, уходят с хоста и проверяются](RBOT-OPS-009-declare-service-backups.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, operations, personal-data |
| [Полный прогон после смены версий компонентов ядром 13.2.1](RBOT-OPS-010-full-run-13-2-1.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | dependencies, testing |
| [Ядро 13.7.0](RBOT-OPS-012-core-13-7-0.md) | P1 | В работе | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | dependencies, build |
| [Проверки собраны в наборы check и verify со шлагбаумом перед отправкой](RBOT-QUAL-001-adopt-local-quality-gates.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | quality, build, tooling |
| [Проверка проводки входит в набор check](RBOT-QUAL-003-adopt-wiring-check.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | quality, tooling |
| [Своя аутентификация проекта заменена аутентификацией ядра](RBOT-SEC-003-adopt-core-auth.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | backend, frontend, security |
| [Напоминания, закрытие сессий и очистка просроченного](features/mvp/10-reminders-and-session-closing.md) | P2 | Запланирована | Не назначен | backend, telegram |
| [Вопросы психологу по домашнему заданию](features/mvp/11-homework-questions.md) | P2 | Запланирована | Не назначен | backend, frontend, telegram, personal-data |
| [Ошибки API отдаются одним контрактом ProblemDetail](RBOT-API-001-adopt-web-errors.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | backend, api |
| [Каждый блок @defer объявляет @error](RBOT-ARC-006-adopt-defer-error.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend |
| [Идентификаторы задач несут префикс проекта](RBOT-DOC-001-adopt-ticket-prefix.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | process, documentation |
| [Сообщение фиксации начинается с идентификатора задачи](RBOT-DOC-002-adopt-commit-ticket-id.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | process, release |
| [Переходы базы — Liquibase, таблицы ядра — журналами ядра](RBOT-OPS-011-liquibase-migrations.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | persistence, deployment |
| [Размер сборки клиента держит проверка](RBOT-QUAL-004-adopt-bundle-budgets.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | frontend, build |
| [Клиент из Telegram не проходит через цепочку безопасности ядра](RBOT-SEC-001-telegram-client-outside-core-auth-chain.md) | P2 | Запланирована | Не назначен | security, backend, platform |
| [Секреты шифруются средством платформы](RBOT-SEC-002-adopt-secret-cipher.md) | P2 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | backend, security |
| [Ограничитель пояснительных комментариев сведён к нулю](RBOT-QUAL-002-reduce-comment-ratchet.md) | P3 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | quality, backend, frontend |
