---
id: MVP-05
type: ticket
status: done
scope: backend, frontend, data
authority: supporting
priority: P1
release: RELEASE-2026-10-22
depends-on: MVP-02, MVP-03
related: ADR-0003
questions: resolved
---

# Запись, перенос, отмена и календарь кабинета

## Проблема

Нет сессий: записать клиента, перенести и отменить встречу нельзя.

## Подтверждение

[ADR-0003](../../../decisions/ADR-0003-own-calendar-and-booking.md): таблица `session`, ограничение `EXCLUDE`, состояния и команды.

## Последствия при сохранении текущего поведения

Ни бот, ни кабинет не выполняют основную функцию.

## Требуется

1. Модуль `booking`: таблица `session` с `occupied tstzrange` и ограничением `session_no_overlap`.
2. Команды `book`, `confirm`, `decline`, `reschedule`, `cancel`, `markNoShow`, `complete`. Каждая фиксирует `created_by` или автора перехода. Отказ условной записи при пересечении — тип результата, а не исключение.
3. `book` снимает с типа сессии `price_snapshot`, а с принятой клиентом политики — `policy_version`. До MVP-06 поле допускает пустое значение.
4. Реализация порта «занятые интервалы» для `freeSlots` (MVP-02).
5. События `SessionBooked`, `SessionRescheduled`, `SessionCancelled`, `SessionNoShow` после фиксации транзакции.
6. Кабинет: календарь недели и дня, создание записи психологом (в том числе вне расписания, с предупреждением), перенос перетаскиванием с подтверждением, отмена, отметка неявки. Колокольчик: новая запись и отмена клиентом.

## Критерии приёмки

- Параллельный тест: 10 одновременных `book` на один слот дают ровно одну сессию.
- Перенос на занятое время оставляет старую сессию `BOOKED`.
- Отменённая сессия освобождает слот в `freeSlots`.
- Переходы вне таблицы состояний отвергаются.

## Открытые вопросы

**Вопросы:** решены

1. **Подтверждение первичной консультации** — [ADR-0003](../../../decisions/ADR-0003-own-calendar-and-booking.md), вопрос 1.
   Ответ 2026-10-01: сразу `BOOKED`. До политики отмен (MVP-06) клиент отменяет сам без штрафа не позже минимального срока записи.
2. **Регулярные сессии** — [ADR-0003](../../../decisions/ADR-0003-own-calendar-and-booking.md), вопрос 2. Если серии нужны, они выносятся в отдельный этап после MVP-05.
   Ответ 2026-10-01: серий нет (ADR-0003, вопрос 2).

## Ход работы

- 2026-10-01: запись и отмена клиентом — [RBOT-FEAT-002](../../closed/RBOT-FEAT-002-client-booking-wizard.md); раздел «Записи», запись психологом, перенос, отмена психологом и неявка — [RBOT-FEAT-005](../../closed/RBOT-FEAT-005-cabinet-sessions-and-reschedule.md). Остаются события после фиксации и колокольчик (вместе с MVP-01).
- 2026-10-08: сверка. События — `SessionNotice` (`BOOKED`, `RESCHEDULED`, `CANCELLED`) после фиксации транзакции; колокольчик о записи, отмене и переносе клиентом — [RBOT-FEAT-020](../../closed/RBOT-FEAT-020-cabinet-bell-with-sound.md). Ограничение `session_no_overlap` — в пределах практики ([RBOT-FEAT-017](../../closed/RBOT-FEAT-017-practice-per-psychologist.md)). Критерии: 10 одновременных записей — `ClientBookingIT.tenSimultaneousBookingsOfOneSlotGiveOneSession`; перенос на занятое время и отмена — `CabinetSessionsIT`, `ClientBookingIT`; переход из неактивной записи — `session-not-active`. Отличия от «Требуется», принятые в RBOT-FEAT-005: календарь — неделя списком по дням, перенос — окном, а не перетаскиванием; `confirm` и `decline` не нужны, раз запись сразу `BOOKED`; `complete` — в MVP-10; `policy_version` пуст до MVP-06.
