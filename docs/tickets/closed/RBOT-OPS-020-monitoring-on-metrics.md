---
id: RBOT-OPS-020
type: ticket
status: done
scope: backend, deployment, operations, telegram, personal-data
authority: supporting
priority: P1
release: unassigned
related: RBOT-OPS-019, RBOT-OPS-018, RBOT-FEAT-010, ADR-0005
questions: resolved
---

# Наблюдение за rbot: метрики, журналы, дашборд и оповещения на metrics

## Проблема

О том, что бот на рабочей среде молчит, никто не узнал: с 2026-10-05 14:19 туннель к прокси Telegram не подключался, сервер каждую секунду писал отказ опроса, и это обнаружилось только при раскате через сутки (RBOT-OPS-019). У rbot нет ни метрик во внешней системе, ни оповещений, ни дашборда.

## Подтверждение

Поручение владельца от 2026-10-06: мониторинг — дашборд и оповещения, как у соседних проектов; журналы и метрики; Grafana и остальное уже есть на `root@metrics`.

Что есть на `metrics` (осмотр 2026-10-06):

- Prometheus `/opt/prometheus-3.9.1.linux-amd64`: задания по проектам в `prometheus.yml`, правила `rules/<проект>-*.yml` подключаются маской, у правил метка `project`;
- Alertmanager `/opt/alertmanager-0.31.0.linux-amd64`: получатели Telegram, маршрут по `project` (у clanlog свой бот, остальные — получатель по умолчанию);
- Grafana `/opt/grafana-12.3.2`: дашборды из файлов, папка на проект (`provisioning/dashboards/<проект>.yml` и каталог с JSON);
- Loki и Alloy: журналы clanlog (файл), zavpn (приём по API), journald самой машины.

Образец для rbot — fileio: он на той же рабочей машине `fileio.ru`. Его требования `REQ-MON-001…005` (репозиторий fileio, `docs/requirements/monitoring.md`): служба отдаёт `/actuator/prometheus`; снаружи — только через nginx, только с адреса `metrics` и с паролем basic auth; правила — в репозитории проекта, доставляются раскатом (`promtool check`, `promtool test rules`, `SIGHUP`); доставка оповещения проверяется пробным правилом. Машина `fileio.ru` уже в Prometheus как `instance="fileio:9100"` — её узловые метрики общие для fileio и rbot.

Сеть: `metrics` → `fileio.ru` работает (fileio опрашивается, `up=1`); `fileio.ru` → `metrics` — только на 65.109.246.127, адрес 65.108.55.171 с рабочей машины недоступен (RBOT-OPS-019).

## Последствия при сохранении текущего поведения

Сбои бота, туннеля, службы и копий базы обнаруживаются случайно — клиентами или при следующем раскате.

## Требуется

1. **Метрики службы** — `/actuator/prometheus` (Micrometer, решение ядра о метриках), у всех метрик `application="rbot"`:
   - стандартные HTTP-сервера и JVM;
   - Telegram: мгновение последнего успешного опроса `rbot_telegram_poll_success_timestamp_seconds`, исходы обращений `rbot_telegram_requests_total{method, outcome}`, держит ли экземпляр аренду опроса `rbot_telegram_polling_lease_held`;
   - записи: `rbot_sessions_changes_total{change="booked|rescheduled|cancelled|no_show", by="client|psychologist"}`;
   - в метках нет персональных данных: ни клиентов, ни практик, ни `telegram_user_id` (ADR-0005).
2. **Доступ снаружи** — как у fileio: nginx рабочей машины отдаёт метрики rbot на отдельном пути только адресу `metrics` и с паролем basic auth; прочим — `403`, без пароля — `401`; `/actuator/` снаружи остаётся `404`; новых портов нет. Метрики берутся с действующего экземпляра через upstream `rbot_backend`.
3. **Prometheus** — задание `rbot-backend` с метками `project="rbot"`, `environment="production"`.
4. **Оповещения** — `monitoring/prometheus/rbot-alerts.yml` в репозитории, метка `project="rbot"` и `severity`:
   - служба не отвечает опросу 5 минут — `critical`;
   - успешного опроса Telegram не было 5 минут — `critical` (случай RBOT-OPS-019);
   - ошибки отправки сообщений Telegram за 15 минут — `warning`;
   - доля ответов 5xx выше порога 10 минут — `warning`;
   - копия базы старше 26 часов — `critical` (если копия отдаёт метрику; иначе — отдельной задачей).
   Правила проверяются `promtool check rules` и `promtool test rules` и доставляются раскатом.
5. **Alertmanager** — маршрут `project="rbot"` к получателю (вопрос 1); шаблон срабатывания и снятия — как у соседей.
6. **Дашборд Grafana** — папка `rbot`, JSON в репозитории (`monitoring/grafana/rbot-service.json`), доставка раскатом: доступность и экземпляры, HTTP (частота, задержка, 5xx), JVM, возраст последнего опроса Telegram и исходы обращений, изменения записей, машина `fileio:9100`.
7. **Журналы** — journald служб `rbot@*`, `rbot-telegram-tunnel`, `rbot-backup` в Loki на `metrics` (объём — вопрос 2). Перед отправкой — проверка, что в журналах нет персональных данных клиентов (ADR-0005, MVP-03): `metrics` вне РФ.
8. **Проба доставки** — пробное правило срабатывает и доходит до получателя, снимается сразу после проверки.
9. Runbook `docs/runbooks/production.md`: где метрики, дашборд, правила, как открыть Grafana и Prometheus через ssh-туннель.

## Критерии приёмки

- Prometheus на `metrics` опрашивает rbot, `up{job="rbot-backend"} == 1`; снаружи метрики без пароля — `401`, с чужого адреса — `403`.
- Остановленный туннель Telegram даёт оповещение `critical` не позже чем через 10 минут, восстановление — сообщение о снятии.
- Дашборд rbot открывается в Grafana и показывает данные по всем панелям.
- Журналы rbot видны в Loki; выборочная проверка не находит в них персональных данных.

## Открытые вопросы

**Вопросы:** решены

1. **Куда слать оповещения.** Рекомендация: получатель по умолчанию — тот же чат владельца, что у fileio; отдельный бот, как у clanlog, — если нужен отдельный канал.
   Ответ 2026-10-06: по рекомендации — получатель по умолчанию.
2. **Какие журналы отправлять за рубеж.** Хост `metrics` в Финляндии, журналы с персональными данными должны оставаться в РФ (152-ФЗ, ADR-0005). Рекомендация: отправлять все журналы служб rbot после проверки, что ПДн в них нет, и добавить проверку в тесты (захват журнала, как в критерии MVP-03); если проверку не делать — только уровни `WARN` и `ERROR`.
   Ответ 2026-10-06: по рекомендации — все журналы служб rbot после проверки и с тестом.
3. **Путь журналов до `metrics`.** Рабочая машина видит `metrics` только по 65.109.246.127. Рекомендация: Alloy на `fileio.ru` отправляет журналы в `loki.source.api` на `metrics` по ssh-туннелю, как прокси Telegram, без нового открытого порта.
   Ответ 2026-10-06: по рекомендации — Alloy и ssh-туннель.

## Ход работы

- 2026-10-06: задача заведена по осмотру `metrics` и образцу fileio (`REQ-MON`).
- 2026-10-06: Micrometer `/actuator/prometheus` (`application="rbot"`), метрики `rbot_telegram_poll_success_timestamp_seconds`, `rbot_telegram_requests_total`, `rbot_telegram_polling_lease_held`, `rbot_sessions_changes_total`; nginx `/metrics/rbot`; правила с `promtool test rules`; дашборд `rbot-service`; Alloy и туннель `rbot-logs-tunnel`; `mise run provision-monitoring` проведена. Alloy на рабочую машину — пакетом с `metrics`: apt.grafana.com из РФ отвечает 403. `TelegramIT`: идентификатор Telegram, подпись клиента и токен приглашения не попадают ни в журнал, ни в метрики. Интеграционных тестов 34. Резервное копирование в оповещения не вошло: копия не отдаёт метрику, нужна отдельная задача.
