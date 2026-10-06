---
id: RBOT-OPS-019
type: ticket
status: done
scope: deployment, operations, telegram
authority: supporting
priority: P1
release: unassigned
related: RBOT-FEAT-009, RBOT-FEAT-010
---

# Туннель к прокси Telegram — на второй адрес metrics

## Проблема

С 2026-10-05 14:19 бот на рабочей среде не получал и не отправлял сообщения: туннель `rbot-telegram-tunnel` не подключался к `metrics`, служба перезапускалась каждые ~2 минуты (к 2026-10-06 — 579 раз).

## Подтверждение

Обнаружено при раскате 2026.10.15 2026-10-06: `[telegram] deleteWebhook: прокси не ответил`, в журнале сервера каждую секунду отказ опроса. Журнал туннеля: `ssh: connect to host 65.108.55.171 port 22: Connection timed out`. С `fileio.ru` адрес 65.108.55.171 не отвечает ни на ping, ни на порты 22, 80, 443; на `metrics` sshd и tinyproxy работают, правила nftables порт 22 пропускают. Второй адрес `metrics` — 65.109.246.127 — с `fileio.ru` доступен, ключ хоста совпадает.

## Последствия при сохранении текущего поведения

Бот молчит: приглашения не открываются, клиенты не получают сообщений о записях.

## Требуется

1. Цель туннеля `RBOT_TELEGRAM_PROXY_TARGET` — `rbot-tunnel@65.109.246.127`; адрес в `/etc/rbot/telegram_proxy_known_hosts` с тем же ключом хоста.
2. `provision-telegram-proxy.sh`: адрес прокси по умолчанию — 65.109.246.127.
3. Выход `metrics` в Telegram не меняется: tinyproxy по-прежнему ходит с 65.108.55.171 (`Bind`), этот путь работает.

## Критерии приёмки

- Служба туннеля активна, `curl -x http://127.0.0.1:18888 https://api.telegram.org/` с `fileio.ru` отвечает; аренду опроса продлевает экземпляр сервера; `telegram-setup.sh` проходит.

## Ход работы

- 2026-10-06: на хосте сохранены копии `rbot.env.bak-20261006` и `telegram_proxy_known_hosts.bak-20261006`; цель туннеля переведена на 65.109.246.127, ключ хоста сверен `ssh-keyscan`. Туннель активен, прокси отвечает 302, отказов опроса нет, аренда продлевается; `telegram-setup.sh`: deleteWebhook, setChatMenuButton, setMyCommands — ok. Почему 65.108.55.171 недоступен с `fileio.ru` — не выяснено: блокировка на пути, не на `metrics`.
