---
id: RBOT-OPS-021
type: ticket
status: done
scope: deployment, operations
authority: supporting
priority: P1
release: RELEASE-2026-10-17
related: RBOT-OPS-020, RBOT-OPS-019
---

# Метрики rbot — через обратный проброс туннеля, без публичного пути

## Проблема

После раската 2026.10.16 Prometheus на `metrics` не получал метрики rbot: `up{job="rbot-backend"} == 0`.

## Подтверждение

2026-10-06, после раската 2026.10.16:

- Prometheus получал `404`: он шлёт в `Host` адрес цели `37.230.112.169:443`, и nginx отдавал запрос блоку fileio по адресу, а не `admin.yanapaderina.com`;
- новые соединения с `metrics` (исходящий адрес 65.108.55.171) на рабочую машину по 443 проходят примерно одно из пяти — та же сетевая беда, что у туннеля Telegram (RBOT-OPS-019); метрики fileio держатся на уже открытом соединении.

## Последствия при сохранении текущего поведения

Оповещения RBOT-OPS-020 срабатывают ложно (`RbotBackendDown`), дашборд пуст.

## Требуется

1. Публичного пути метрик нет: nginx отдаёт `/metrics/rbot` только на `127.0.0.1:19100` с паролем basic auth.
2. Туннель `rbot-monitoring-tunnel` (бывший `rbot-logs-tunnel`, рабочая машина → `metrics` по 65.109.246.127) несёт и журналы (`-L 13100 → Loki 3100`), и метрики (`-R 127.0.0.1:19100 → 127.0.0.1:19100`); ключу туннеля на `metrics` разрешено `permitlisten="127.0.0.1:19100"`.
3. Задание `rbot-backend` опрашивает `127.0.0.1:19100` на `metrics`; `provision-monitoring.sh` заменяет прежний блок задания.

## Критерии приёмки

- `up{job="rbot-backend"} == 1`, метрики Telegram и записей есть в Prometheus; снаружи `https://admin.yanapaderina.com/metrics/rbot` — не метрики.

## Ход работы

- 2026-10-06: подготовка повторена — задание переписано на `127.0.0.1:19100`, ключу туннеля добавлен `permitlisten`.
