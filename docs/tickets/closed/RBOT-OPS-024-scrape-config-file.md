---
id: RBOT-OPS-024
type: ticket
status: done
scope: operations
authority: supporting
priority: P1
release: RELEASE-2026-10-20
related: RBOT-OPS-020, RBOT-OPS-021
---

# Задание Prometheus для rbot — отдельным файлом

## Проблема

2026-10-06 в 15:12 задание `rbot-backend` пропало из `prometheus.yml` на `metrics`: Prometheus перестал опрашивать rbot, оповещение `RbotBackendDown` сработало.

## Подтверждение

Рядом с `prometheus.yml` — копия `prometheus.yml.pre-fio-ops-030` (14:14) с заданием `rbot-backend`; в новом файле задания fileio переписаны на `127.0.0.1:9443`, а блок rbot, стоявший в конце файла сразу за ними, срезан. Правки соседних проектов в общем `prometheus.yml` задевают чужие блоки.

## Последствия при сохранении текущего поведения

Любая правка `prometheus.yml` соседом снимает наблюдение за rbot без следа.

## Требуется

1. Задание — файл `scrape/rbot.yml` (из `monitoring/prometheus/rbot-scrape.yml`), подключённый через `scrape_config_files`; в `prometheus.yml` от rbot — только строка этого списка и маска правил.
2. `provision-monitoring.sh` убирает прежний блок из `prometheus.yml`, ставит файл и проверяет `promtool check config`.

## Критерии приёмки

- `up{job="rbot-backend"} == 1`; задания соседей не изменились.

## Ход работы

- 2026-10-06: подготовка повторена — `scrape/rbot.yml` подключён, `rbot-backend` снова `up`; clanlog, fileio, zavpn и узлы — как были.
