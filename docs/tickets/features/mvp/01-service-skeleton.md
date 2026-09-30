---
id: MVP-01
type: ticket
status: in_progress
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

## Ход работы

2026-09-30 — первый рабочий срез по запросу владельца: минимальное приложение со входом по почте и паролю, развёрнутое на `admin.yanapaderina.com` (кабинет) и `bot.yanapaderina.com` (приложение клиента, пока заглушка). Хост — `fileio.ru`, общий с `fileio`.

Сделано:

- Служба `com.yanapaderina:rbot` на `platform-service-parent` 13.4.0: `platform-auth`, `platform-persistence`, `platform-time`, `platform-web-errors`, `platform-rate-limit`. Таблицы ядра — переходом `V1__platform_auth.sql`; миграции — отдельным запуском `java -jar rbot.jar migrate` (`REQ-DEPLOYMENT-007`) на конфигурации `com.yanapaderina.migration.MigrationRun` без сканирования компонентов: иначе контроллеры требуют бинов ядра, которых нет вне веб-контекста.
- Роли `ADMIN` и `PSYCHOLOGIST`. Первый администратор создаётся ядром из `PLATFORM_AUTH_ADMIN_EMAIL` и `PLATFORM_AUTH_ADMIN_PASSWORD` (`REQ-AUTH-009`).
- Почты нет (решение владельца 2026-09-30). `LoginOnlyEntry` открывает из точек входа ядра только `/api/auth/login`: регистрация, подтверждение и сброс по почте закрыты `entry-closed`. `NoLetters` не отправляет письма и пишет в журнал только вид письма, без адреса (`REQ-AUTH-013`).
- `/api/admin/accounts`: список с поиском, создание с ролью, смена пароля, блокировка — только `ADMIN`. Пароль задаёт администратор, потому что сброса по почте нет.
- Клиент: Angular 22, два приложения `admin` и `bot` в одном воркспейсе, общий вход в `frontend/shared`.
- Развёртывание по образцу `fileio`: `mise run provision -- <почта администратора>` готовит хост (пользователь, каталоги, база, секреты, сертификаты), `mise run deploy` собирает коммит на хосте, делает копию базы, миграции, поднимает свободный экземпляр 8091 или 8092, переключает nginx и проверяет оба сайта снаружи.
- Redis общий с `fileio` на хосте, база Redis 1 и пространство сессий `rbot:session`.

Проверки:

- `mise run backend-test` — модульный тест `LoginOnlyEntry`.
- `mise run backend-test-integration` — 4 теста на PostgreSQL и Redis в Testcontainers: администратор создаёт психолога, психолог не видит учётных записей, занятая почта — `email-taken`, регистрация — `entry-closed`, гость — `401`.
- Локальный стенд (`mise run backend-run`, `npm run start:admin`): вход администратора, создание учётной записи психолога в кабинете.

Не сделано из «Требуется» и остаётся в задаче: модули Modulith и тест графа (п. 2), `platform-notifications` и колокольчик (пп. 5, 6), смена своего пароля в кабинете (п. 6), хук `pre-push` и расписка `verify` ядра (п. 7). Манифест раската, журнал компонентов и проверки `conventions` при развёртывании, как у `fileio`, — в MVP-13.

2026-09-30 — ядро `v13.5.0` и правила ядра:

- `.platform-version`, `platform-service-parent` и пакеты npm — `13.5.0`.
- `AGENTS.md` с блоком правил ядра (`conventions sync`) и разделом проекта; `CLAUDE.md` импортирует его.
- `REQ-CLIENT-UPDATE`: служба подключает `platform-api-version`, `platform.api.min-supported-version=1`. Оба приложения объявляют `provideAppUpdate` и `appUpdateInterceptor` в своём `app.config.ts`; компоненты «вышла новая версия» и «страница устарела» — в `frontend/shared`, версия API клиента — `frontend/shared/api-version.ts`.
- `REQ-QUALITY`: ESLint с конфигурацией пакета правил ядра, `mise run frontend-lint` входит в `check`.
- Проверки: `conventions check` — «Правила соблюдены»; `mise run verify` — документация, правила, lint, 7 модульных и 4 интеграционных теста, сборка обоих приложений; вход в кабинет в браузере на локальном стенде — запросы уходят с `X-Api-Version`.
- Обязательства ядра (`conventions check` показывает их как долг, проверку они не роняют) заводятся задачами при открытии первого выпуска (`REQ-ADOPTION-013`).

2026-09-30 — QA-стенд и тестовые учётные записи по запросу владельца: `mise run deploy-qa` собирает рабочее дерево и поднимает `docker-compose.qa.yml` в Docker на `mini` (контекст `zavpn-mini`), порт 4206, имена `qa.admin.yanapaderina.test` и `qa.bot.yanapaderina.test` разводит nginx в контейнере. Профиль `qa` (`QaAccounts`, `application-qa.properties`) заводит администратора и психолога с паролями из репозитория, перечень — в `RUN-QA`; с `production` профиль не действует. Локальный стенд тестовых учётных записей не заводит. Проверки: `QaAccountsIT` — обе учётные записи входят, психологу закрыт `/api/admin/**`; `AdminAccountsIT` — без профиля `qa` тестовая почта не входит; на стенде в браузере — администратор видит обе учётные записи в кабинете, психолог входит в приложение.
