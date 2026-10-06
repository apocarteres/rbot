---
id: RUN-PRODUCTION
type: runbook
status: active
scope: operations
authority: supporting
related: MVP-01, MVP-13
---

# Рабочая среда

[Инструкции](INDEX.md)

Хост `root@fileio.ru` (37.230.112.169), общий с `fileio`. Кабинет — `https://admin.yanapaderina.com`, приложение — `https://bot.yanapaderina.com`.

## Подготовка хоста

`mise run provision -- <почта администратора> [почта для Let's Encrypt]` — один раз и повторно без вреда:

- пользователь `rbot`, каталоги `/opt/rbot`, `/var/lib/rbot`, `/var/www/rbot`, `/var/backups/rbot`;
- `/etc/rbot/rbot.env` с новыми секретами, если файла нет; пароль Redis берётся из настроек Redis хоста;
- роль и база PostgreSQL `rbot`, расширение `btree_gist`;
- сертификаты Let's Encrypt для обоих имён, продление — общим таймером `certbot`.

## Первый вход

Пароль первого администратора лежит только на хосте:

```
ssh root@fileio.ru grep PLATFORM_AUTH_ADMIN_PASSWORD /etc/rbot/rbot.env
```

После входа смените пароль в разделе «Учётные записи» и удалите строки `PLATFORM_AUTH_ADMIN_EMAIL` и `PLATFORM_AUTH_ADMIN_PASSWORD` из `/etc/rbot/rbot.env`: учётная запись уже создана, а пароль в файле больше не нужен.

## Раскат

`mise run deploy` — разворачивает `HEAD` при чистом рабочем дереве. `--only backend,frontend,web` ограничивает состав: `web` — конфигурация nginx и unit systemd.

Порядок на хосте: сборка → копия базы в `/var/backups/rbot` (хранится 7 дней) → миграции → свободный экземпляр `rbot@8091` или `rbot@8092` → проверка готовности → переключение nginx → прежний экземпляр останавливается через 5 минут → проверка обоих сайтов снаружи. Журнал раскатов — `/opt/rbot/journal.log`.

## Откат

`RBOT_PROD_COMMIT=<коммит> mise run deploy` разворачивает прежний коммит. Переход базы назад не выполняется: для отката схемы восстановите копию из `/var/backups/rbot` командой `pg_restore --clean -d rbot`.

## Наблюдение

RBOT-OPS-020. Всё на общем хосте `metrics` (Prometheus, Alertmanager, Grafana, Loki), как у соседних проектов.

- **Метрики.** Служба отдаёт `/actuator/prometheus`; nginx рабочей машины отдаёт их только локально — `http://127.0.0.1:19100/metrics/rbot` с паролем basic auth `prometheus`, наружу путей метрик нет. Туннель `rbot-monitoring-tunnel` (рабочая машина → `metrics` по 65.109.246.127) пробрасывает этот порт обратно на `metrics` (`127.0.0.1:19100`), Prometheus опрашивает его там: новые соединения с `metrics` (65.108.55.171) на рабочую машину с 2026-10-05 проходят через раз. Пароль — `/etc/rbot/metrics.password` на рабочей машине и `/etc/prometheus/rbot-metrics.password` на `metrics`. Задание Prometheus `rbot-backend` — `monitoring/prometheus/rbot-scrape.yml`, вписано в `prometheus.yml` подготовкой.
- **Оповещения.** `monitoring/prometheus/rbot-alerts.yml` с тестами `rbot-alerts.test.yml`, метка `project="rbot"`; Alertmanager шлёт получателю по умолчанию (Telegram владельца). Раскат (составляющая `alert-rules`) проверяет `promtool check` и `promtool test rules`, ставит в `rules/rbot-alerts.yml`, `SIGHUP`. Правила: сервер не отвечает 5 минут; успешного опроса Telegram не было 5 минут; не ушли сообщения бота за 15 минут; больше 5% ответов 5xx.
- **Дашборд.** `monitoring/grafana/dashboards/rbot-service.json`, папка `rbot`, доставляет раскат (составляющая `dashboard`). Открыть: `ssh -N -L 3000:127.0.0.1:3000 -L 9090:127.0.0.1:9090 root@metrics`, затем `http://localhost:3000/d/rbot-service` и `http://localhost:9090/targets`.
- **Журналы.** Alloy на рабочей машине читает journald служб `rbot@*`, `rbot-telegram-tunnel`, `rbot-monitoring-tunnel`, `rbot-backup` (`deploy/alloy/rbot.alloy` → `/etc/alloy/config.alloy`, составляющая `logs-pipeline`), маскирует почту и числа от 7 знаков и отправляет в Loki на `metrics` через туннель `rbot-monitoring-tunnel` (`127.0.0.1:13100` → `127.0.0.1:3100`, тот же ключ, что у туннеля Telegram). В Grafana: `{project="rbot"}`.
- **Подготовка** (разово, повторный запуск безопасен): `mise run provision-monitoring` — пароль и htpasswd, задание Prometheus и маска правил, папка Grafana, доступ ключа туннеля к Loki и обратный проброс метрик, установка Alloy.
