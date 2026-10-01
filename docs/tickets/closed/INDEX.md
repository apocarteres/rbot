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

Всего: 50. Включены самостоятельные задачи и этапы планов функций.

| Задача | Приоритет | Статус | Выпуск | Области |
|---|---|---|---|---|
| [Настройка среды едет развёртыванием: составляющая, место, разбор и проверки поведения](RBOT-OPS-007-adopt-environment-configuration.md) | P0 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | deployment, operations |
| [Расписание психолога и свободные слоты](../features/mvp/02-work-schedule.md) | P1 | Выполнена | [RELEASE-2026-10-1](../../releases/RELEASE-2026-10-1.md) | backend, frontend, data |
| [Java-код разделён на модули, граф связей ациклический](RBOT-ARC-001-adopt-java-modules.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, java, architecture |
| [Каждое модальное окно явно решает, что делать по Escape](RBOT-ARC-002-adopt-modal-escape.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно с удалённым действием закрывается после успеха, каждая кнопка окна объявляет своё действие](RBOT-ARC-003-adopt-modal-actions.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Окно не закрывается во время вызова, фон решает явно, отказ показывается в окне](RBOT-ARC-004-adopt-modal-busy-backdrop.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Клиент узнаёт о новой сборке и об устаревшем API механизмом ядра](RBOT-ARC-005-adopt-client-update.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend, backend |
| [Доступ к данным: без ORM, простой DAO, транзакция в прикладном слое](RBOT-DATA-001-adopt-data-access.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, java, persistence |
| [Правка и создание в модальных окнах вместо inline-форм](RBOT-FEAT-001-dialogs-instead-of-inline-forms.md) | P1 | Выполнена | [RELEASE-2026-10-2](../../releases/RELEASE-2026-10-2.md) | frontend |
| [Приложение клиента: мастер записи и мои сессии](RBOT-FEAT-002-client-booking-wizard.md) | P1 | Выполнена | [RELEASE-2026-10-2](../../releases/RELEASE-2026-10-2.md) | backend, frontend, data |
| [Без сообщений об успехе, отказ — окном с причиной](RBOT-FEAT-003-failure-dialogs-without-success-notices.md) | P1 | Выполнена | [RELEASE-2026-10-2](../../releases/RELEASE-2026-10-2.md) | frontend |
| [Промежутки расписания для выбранных типов сессий](RBOT-FEAT-004-interval-session-types.md) | P1 | Выполнена | [RELEASE-2026-10-2](../../releases/RELEASE-2026-10-2.md) | backend, frontend, data |
| [Записи в кабинете и перенос](RBOT-FEAT-005-cabinet-sessions-and-reschedule.md) | P1 | Выполнена | [RELEASE-2026-10-3](../../releases/RELEASE-2026-10-3.md) | backend, frontend, data |
| [Тип сессии у промежутка обязателен](RBOT-FEAT-008-interval-types-required.md) | P1 | Выполнена | [RELEASE-2026-10-3](../../releases/RELEASE-2026-10-3.md) | backend, frontend, data |
| [Клиент через Telegram: приглашение, согласие, Mini App](RBOT-FEAT-009-telegram-client.md) | P1 | Выполнена | [RELEASE-2026-10-4](../../releases/RELEASE-2026-10-4.md) | backend, frontend, data, telegram, security, personal-data |
| [Обновления Telegram опросом и прокси по IPv4](RBOT-FEAT-010-telegram-polling.md) | P1 | Выполнена | [RELEASE-2026-10-5](../../releases/RELEASE-2026-10-5.md) | backend, telegram, deployment |
| [Стили не применялись на рабочей среде](RBOT-FEAT-011-styles-under-csp.md) | P1 | Выполнена | [RELEASE-2026-10-6](../../releases/RELEASE-2026-10-6.md) | frontend, deployment, security |
| [Типы сессий: удаление, перерыв у типа, без «для нового клиента»](RBOT-FEAT-016-session-type-edits.md) | P1 | Выполнена | [RELEASE-2026-10-10](../../releases/RELEASE-2026-10-10.md) | backend, frontend, data |
| [Своя практика у каждого психолога](RBOT-FEAT-017-practice-per-psychologist.md) | P1 | Выполнена | [RELEASE-2026-10-11](../../releases/RELEASE-2026-10-11.md) | backend, frontend, data, personal-data, telegram |
| [Цикл выпуска закрывается одной командой и помечает проверенный коммит](RBOT-OPS-001-adopt-release-cycle.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | release, build, deployment |
| [Расписку о проверках пишет наблюдавший прогон, а не ручной вызов](RBOT-OPS-002-adopt-observed-receipt.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | quality, build, release |
| [Сборка потребителя идёт на Java 25, как и сборка ядра](RBOT-OPS-003-adopt-java-25.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | build, tooling |
| [Развёртывание через интерфейс ядра: среда именем, составляющие объявлены, манифест и сверка развёрнутого](RBOT-OPS-004-adopt-deployment-interface.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | deployment, build |
| [Неизвестный адрес отвечает как неизвестный: порт клиента и 404 на сервере](RBOT-OPS-005-adopt-unknown-path.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | deployment, client |
| [Написание входа в развёртывание: scripts/deploy.sh, разбор доводов ядром, среда production](RBOT-OPS-006-adopt-deploy-entry-spelling.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | deployment, build |
| [Куски прежней сборки клиента переживают раскат](RBOT-OPS-008-adopt-static-archive.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | deployment, frontend |
| [Копии данных службы объявлены, уходят с хоста и проверяются](RBOT-OPS-009-declare-service-backups.md) | P1 | Выполнена | [RELEASE-2026-10-1](../../releases/RELEASE-2026-10-1.md) | deployment, operations, personal-data |
| [Полный прогон после смены версий компонентов ядром 13.2.1](RBOT-OPS-010-full-run-13-2-1.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | dependencies, testing |
| [Ядро 13.7.0](RBOT-OPS-012-core-13-7-0.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | dependencies, build |
| [Проверка рабочей среды после раската выпуска 2026.09.1](RBOT-OPS-013-verify-production-after-release-2026-09-1.md) | P1 | Выполнена | [RELEASE-2026-10-1](../../releases/RELEASE-2026-10-1.md) | deployment, operations |
| [Ядро 14.0.0](RBOT-OPS-014-core-14-0-0.md) | P1 | Выполнена | [RELEASE-2026-10-1](../../releases/RELEASE-2026-10-1.md) | dependencies, build, persistence |
| [Сборочный контейнер: наборы и интеграционные тесты в Docker](RBOT-OPS-015-build-runner.md) | P1 | Выполнена | [RELEASE-2026-10-1](../../releases/RELEASE-2026-10-1.md) | tooling, testing |
| [Проверка рабочей среды после раската выпуска 2026.10.1](RBOT-OPS-016-verify-production-after-release-2026-10-1.md) | P1 | Выполнена | [RELEASE-2026-10-3](../../releases/RELEASE-2026-10-3.md) | deployment, operations |
| [Проверки собраны в наборы check и verify со шлагбаумом перед отправкой](RBOT-QUAL-001-adopt-local-quality-gates.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | quality, build, tooling |
| [Проверка проводки входит в набор check](RBOT-QUAL-003-adopt-wiring-check.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | quality, tooling |
| [Своя аутентификация проекта заменена аутентификацией ядра](RBOT-SEC-003-adopt-core-auth.md) | P1 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, frontend, security |
| [Ошибки API отдаются одним контрактом ProblemDetail](RBOT-API-001-adopt-web-errors.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, api |
| [Каждый блок @defer объявляет @error](RBOT-ARC-006-adopt-defer-error.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend |
| [Идентификаторы задач несут префикс проекта](RBOT-DOC-001-adopt-ticket-prefix.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | process, documentation |
| [Сообщение фиксации начинается с идентификатора задачи](RBOT-DOC-002-adopt-commit-ticket-id.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | process, release |
| [Удаление исключения кнопкой-иконкой](RBOT-FEAT-006-exception-delete-icon.md) | P2 | Выполнена | [RELEASE-2026-10-3](../../releases/RELEASE-2026-10-3.md) | frontend |
| [Вкладка «Правила записи» с пояснениями к полям](RBOT-FEAT-007-booking-rules-tab.md) | P2 | Выполнена | [RELEASE-2026-10-3](../../releases/RELEASE-2026-10-3.md) | frontend |
| [Выключенный тип сессии нельзя отметить у промежутка](RBOT-FEAT-012-inactive-types-in-intervals.md) | P2 | Выполнена | [RELEASE-2026-10-7](../../releases/RELEASE-2026-10-7.md) | frontend |
| [Копирование часов другого дня недели](RBOT-FEAT-013-copy-weekday-hours.md) | P2 | Выполнена | [RELEASE-2026-10-8](../../releases/RELEASE-2026-10-8.md) | frontend |
| [Длительность типа в предпросмотре и пересоздание клиента стенда](RBOT-FEAT-014-preview-session-length.md) | P2 | Выполнена | [RELEASE-2026-10-8](../../releases/RELEASE-2026-10-8.md) | frontend, deployment |
| [Кнопка бота «Личный кабинет»](RBOT-FEAT-015-bot-cabinet-button.md) | P2 | Выполнена | [RELEASE-2026-10-9](../../releases/RELEASE-2026-10-9.md) | telegram |
| [Переходы базы — Liquibase, таблицы ядра — журналами ядра](RBOT-OPS-011-liquibase-migrations.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | persistence, deployment |
| [Размер сборки клиента держит проверка](RBOT-QUAL-004-adopt-bundle-budgets.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | frontend, build |
| [Секреты шифруются средством платформы](RBOT-SEC-002-adopt-secret-cipher.md) | P2 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | backend, security |
| [Ограничитель пояснительных комментариев сведён к нулю](RBOT-QUAL-002-reduce-comment-ratchet.md) | P3 | Выполнена | [RELEASE-2026-09-1](../../releases/RELEASE-2026-09-1.md) | quality, backend, frontend |
