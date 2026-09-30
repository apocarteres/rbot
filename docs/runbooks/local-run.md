---
id: RUN-LOCAL
type: runbook
status: active
scope: operations, development
authority: supporting
related: MVP-01
---

# Локальный запуск

[Инструкции](INDEX.md)

1. Инструменты: `mise install` (Java, Node, npm из `mise.toml`).
2. Ядро: `mise run platform-install` — артефакты Maven и пакеты npm закреплённой версии из `.platform-version`.
3. Клиент: `mise run frontend-install`.
4. Сервер: `mise run backend-run` — PostgreSQL и Redis из `docker-compose.dev.yml`, миграции, служба на `http://localhost:8080`. Настройки — `deploy/local.env`, создаётся из `deploy/local.env.example`.
5. Кабинет: `npm --prefix frontend run start:admin` → `http://localhost:4200`. Приложение: `npm --prefix frontend run start:bot` → `http://localhost:4201`.

## Тестовые учётные записи

Сервер локального стенда запускается с профилем `local` и при старте заводит учётные записи из `src/main/resources/application-local.properties`, если их ещё нет:

| Почта | Пароль | Роли |
|---|---|---|
| `admin@rbot.localhost` | `local-admin-password` | администратор и психолог |
| `psychologist@rbot.localhost` | `local-psychologist-password` | психолог |

Профиль `local` не включается вместе с `production`. На рабочей среде тестовые учётные записи заводит администратор в кабинете, в разделе «Учётные записи»: пароли из репозитория там не действуют.

Интеграционные тесты: `mise run backend-test-integration`. Демон Docker ищется в контексте Docker и в машинах Lima; для rootless Docker выставляются переменные Testcontainers.
