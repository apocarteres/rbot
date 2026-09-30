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

Всего: 20. Включены самостоятельные задачи и этапы планов функций.

| Задача | Приоритет | Статус | Выпуск | Области |
|---|---|---|---|---|
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
| [Копии данных службы объявлены, уходят с хоста и проверяются](RBOT-OPS-009-declare-service-backups.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | deployment, operations, personal-data |
| [Полный прогон после смены версий компонентов ядром 13.2.1](RBOT-OPS-010-full-run-13-2-1.md) | P1 | Запланирована | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | dependencies, testing |
| [Ядро 13.7.0](RBOT-OPS-012-core-13-7-0.md) | P1 | В работе | [RELEASE-2026-09-1](../releases/RELEASE-2026-09-1.md) | dependencies, build |
| [Проверка рабочей среды после раската выпуска 2026.09.1](RBOT-OPS-013-verify-production-after-release-2026-09-1.md) | P1 | Запланирована | Не назначен | deployment, operations |
| [Напоминания, закрытие сессий и очистка просроченного](features/mvp/10-reminders-and-session-closing.md) | P2 | Запланирована | Не назначен | backend, telegram |
| [Вопросы психологу по домашнему заданию](features/mvp/11-homework-questions.md) | P2 | Запланирована | Не назначен | backend, frontend, telegram, personal-data |
| [Клиент из Telegram не проходит через цепочку безопасности ядра](RBOT-SEC-001-telegram-client-outside-core-auth-chain.md) | P2 | Запланирована | Не назначен | security, backend, platform |
