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

Учётная запись администратора локального стенда задана в `deploy/local.env.example`. Тестовые учётные записи — на [QA-стенде](qa.md).

Интеграционные тесты: `mise run backend-test-integration`. Демон Docker ищется в контексте Docker и в машинах Lima; для rootless Docker выставляются переменные Testcontainers.
