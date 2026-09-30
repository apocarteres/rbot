---
id: RUN-QA
type: runbook
status: active
scope: operations, testing
authority: supporting
related: MVP-01
---

# QA-стенд

[Инструкции](INDEX.md)

Стенд работает в Docker на `mini` (192.168.88.235), контекст Docker `zavpn-mini`. Имена стенда прописаны в `/etc/hosts`:

```
192.168.88.235 qa.admin.yanapaderina.test
192.168.88.235 qa.bot.yanapaderina.test
```

- Кабинет — http://qa.admin.yanapaderina.test:4206
- Приложение — http://qa.bot.yanapaderina.test:4206

## Раскат

`mise run deploy-qa` собирает сервер и клиент из рабочего дерева и поднимает `docker-compose.qa.yml`: PostgreSQL, Redis, миграции, сервер с профилем `qa`, nginx с обоими сайтами. Данные базы сохраняются в томе `rbot-qa_postgres` между раскатами. Чистая база: `DOCKER_CONTEXT=zavpn-mini docker compose -f docker-compose.qa.yml down -v`.

## Тестовые учётные записи

Сервер с профилем `qa` при старте заводит учётные записи из `src/main/resources/application-qa.properties`, если их ещё нет:

| Почта | Пароль | Роли |
|---|---|---|
| `admin@yanapaderina.test` | `qa-admin-password` | администратор и психолог |
| `psychologist@yanapaderina.test` | `qa-psychologist-password` | психолог |

Пароли лежат в публичном репозитории, поэтому профиль `qa` не включается вместе с `production`. На стенде нельзя держать реальные данные клиентов (152-ФЗ).
