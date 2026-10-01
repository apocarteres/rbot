# rbot

<!-- conventions:begin v14.0.0 -->
## Правила кода

Тексты: `node_modules/@apocarteres/project-conventions/docs`. Прочитать перед правкой кода.
Словарь терминов и запрещённых слов: `node_modules/@apocarteres/project-conventions/docs/terms`, свой — `docs/terms`.
Проверка: `mise run conventions-check`.
<!-- conventions:end -->

Сервис записи к психологу: Telegram-бот, Mini App и кабинет психолога на общем ядре [platform](https://github.com/apocarteres/platform). Документация — [docs/INDEX.md](docs/INDEX.md).

## Назначение и границы

- Архитектура — `docs/decisions/ADR-0001-service-architecture.md`; порядок работ — план MVP в `docs/tickets/features/mvp/`.
- Решения о деньгах (штрафы за отмену) принимает детерминированный код, не языковая модель: `ADR-0004`.
- Данные клиентов — сведения о здоровье, специальная категория 152-ФЗ: `ADR-0005`. Реальные данные клиентов не попадают в репозиторий, фикстуры, журналы и метрики.
- Версия ядра закреплена в `.platform-version`; обновление ядра — отдельное изменение.

## Проверки

- `mise run check` — документация, правила ядра, модульные тесты сервера, сборка клиента.
- `mise run verify` — `check`, интеграционные тесты на PostgreSQL и Redis, сборка сервера.
- Раскат — `mise run deploy`, порядок — `docs/runbooks/production.md`.
