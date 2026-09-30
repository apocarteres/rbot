---
id: RBOT-OPS-011
type: ticket
status: in_progress
scope: persistence, deployment
authority: supporting
priority: P2
release: RELEASE-2026-09-1
obligation: liquibase-migrations
related: REQ-DATA-ACCESS
---

# Переходы базы — Liquibase, таблицы ядра — журналами ядра

## Проблема

Таблицы ядра созданы переходами проекта по образцам create-*.sql, а ядро теперь поставляет их журналами Liquibase (REQ-DATA-ACCESS-007). Проект на Flyway ведёт переходы инструментом, которого ядро не поддерживает, и каждое изменение таблиц ядра переносит руками.

## Основание

Обязательство ядра `liquibase-migrations`, требование `REQ-DATA-ACCESS-008`, объявлено в версии ядра 13.6.0. Срок исполнения: 3 выпуск(ов) потребителя.

## Последствия при сохранении текущего поведения

Требование ядра действует, а проект ему не соответствует: расхождение обнаруживается не проверкой, а при следующем изменении в этой области.

## Требуется

1. Главный журнал Liquibase проекта подключает журналы ядра директивой include — platform/changelog/platform-auth.yaml первым, затем журналы остальных подключённых модулей ядра — и после них свои наборы изменений (REQ-DATA-ACCESS-007).
2. Существующая база принимает журналы ядра командой changelogSync один раз, до первого update с журналами ядра (REQ-DATA-ACCESS-008).
3. Проект на Flyway переносит свои переходы в журнал Liquibase и помечает уже применённые тем же changelogSync; зависимость Flyway убрана.
4. Свои копии таблиц ядра в переходах проекта больше не ведутся.

## Критерии приёмки

- На рабочей среде liquibase status не называет невыполненных наборов, а update ничего не меняет.
- Правило migration-tool не находит Flyway в зависимостях проекта.
- Проект без базы закрывает обязательство задачей, которая это называет.

## Ход работы

- 2026-09-30: Flyway убран из `pom.xml`, подключён `spring-boot-starter-liquibase` (версия — ядра, Liquibase 5.0.3). Главный журнал `src/main/resources/db/changelog/db.changelog-master.yaml` подключает `platform/changelog/platform-auth.yaml`, свои наборы — `db/changelog/project/` (пока пусто: у проекта своих таблиц нет, прежний `V1__platform_auth.sql` был копией таблиц ядра и снят; он оставлен тестовым образцом `src/test/resources/legacy/flyway-v1.sql`).
- Переходы выполняет запуск `migrate` (`com.yanapaderina.migration.SchemaMigration`), при старте веб-службы Liquibase выключен (`spring.liquibase.enabled=false`).
- Отклонение от `RUN-LIQUIBASE-ADOPTION` с причиной: базы проекта созданы Flyway по образцам ядра 13.4.0 и содержат `platform_account`, `platform_account_role`, `platform_account_token`, но не `platform_access_key` — схема совпадает с наборами 001–003 журнала ядра, а не со всем журналом. `changelogSync` целиком пометил бы применённой несуществующую таблицу. Поэтому `migrate` на базе с `flyway_schema_history` и без `databasechangelog` один раз помечает три набора командой `markNextChangesetRan`, сверяет, что помечены именно `platform-auth:001-account`, `002-role`, `003-token`, и дальше выполняет `update`: `004-access-key` применяется из журнала ядра, своей копии SQL у проекта нет.
- Проверки: `SchemaMigrationIT` — чистая база получает четыре набора ядра; база Flyway принимается, учётная запись сохраняется, повторный запуск ничего не меняет. Локальная база стенда, созданная Flyway: после `migrate` в `databasechangelog` четыре набора ядра, обе учётные записи на месте, `platform_access_key` создана. `conventions check`: замечания `migration-tool` нет.
- Осталось: принять базы QA и рабочей среды раскатом; на рабочей среде `liquibase status` не называет невыполненных наборов. Таблица `flyway_schema_history` удаляется отдельным набором после выпуска, когда откат на сборку с Flyway перестанет быть нужен.
