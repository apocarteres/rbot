---
id: RBOT-OPS-022
type: ticket
status: done
scope: deployment, operations
authority: supporting
priority: P1
release: RELEASE-2026-10-18
related: RBOT-OPS-021, RBOT-OPS-020
---

# Локальный путь метрик: Host, который принимает Tomcat

## Проблема

После раската 2026.10.17 опрос `127.0.0.1:19100/metrics/rbot` отвечал `400 Bad Request`.

## Подтверждение

2026-10-06: без `proxy_set_header Host` nginx передаёт в заголовке имя upstream `rbot_backend`; Tomcat отвергает его разбором `Host.parse` (подчёркивание в имени хоста). Прямой запрос на экземпляр — `200`.

## Последствия при сохранении текущего поведения

Метрик нет, оповещение `RbotBackendDown` срабатывает ложно.

## Требуется

1. В локальном блоке метрик nginx — `proxy_set_header Host localhost`.

## Критерии приёмки

- `up{job="rbot-backend"} == 1` на `metrics`.

## Ход работы

- 2026-10-06: строка добавлена в `deploy/nginx/rbot.conf`.
