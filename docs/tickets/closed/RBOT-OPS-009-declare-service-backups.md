---
id: RBOT-OPS-009
type: ticket
status: done
scope: deployment, operations, personal-data
authority: supporting
priority: P1
release: RELEASE-2026-10-1
obligation: backups
related: REQ-BACKUPS
---

# Копии данных службы объявлены, уходят с хоста и проверяются

## Проблема

Регулярная копия данных службы не объявлена ядру: что копируется, куда, как долго хранится, включено ли задание и проверялось ли восстановление, известно только осмотром хоста.

## Основание

Обязательство ядра `backups`, требование `REQ-BACKUPS-009`, объявлено в версии ядра 11.0.0. Срок исполнения: 3 выпуск(ов) потребителя.

## Последствия при сохранении текущего поведения

Требование ядра действует, а проект ему не соответствует: расхождение обнаруживается не проверкой, а при следующем изменении в этой области.

## Требуется

1. Раздел `backups` в `.conventions.json`: каждый набор данных службы с видом, расписанием, конечным сроком хранения, внешним хранилищем и путём квитанции (REQ-BACKUPS-001, REQ-BACKUPS-003).
2. Задание копии своей базы и своих файлов пишет квитанцию `conventions backups --receipt` (REQ-BACKUPS-002, REQ-BACKUPS-004).
3. Таймер задания доставляется составляющей с `enable: true` и включается развёртыванием (REQ-BACKUPS-005).
4. Восстановление проверено и записано `conventions backups --restored` (REQ-BACKUPS-007).
5. Соглашение называет срок хранения копий (REQ-BACKUPS-008).

## Критерии приёмки

- `conventions backups --check` проходит на рабочей среде.
- Развёртывание рабочей среды вызывает `--check` и сообщает отказ.
- Проект без данных закрывает обязательство задачей, которая это называет.

## Ход работы

- 2026-10-01: обязательство перенесено командой `release defer backups` с причиной: данных клиентов в службе нет, база хранит только учётные записи сотрудников; внешнее хранилище копий в РФ владелец не выбрал. До этого копию базы перед каждым раскатом делает `host-release.sh` (`/var/backups/rbot`, 7 дней) — это не копия с хоста и обязательство не закрывает. Срок — 3 выпуска; исполняется до приёма данных клиентов (MVP-03, MVP-13).
- 2026-10-01: решение владельца — копии на хост `bones` (Ubuntu, РФ) по SSH с ограниченным ключом, кольцо не больше 5 копий.
- `bones`: пользователь `rbot-backup`, каталог `/srv/backups/rbot` (0700); ключ в `authorized_keys` — `restrict,command="/usr/local/bin/rbot-backup-store"`; приёмник (`deploy/backup/rbot-backup-store`) понимает только `put`, `get`, `latest`, `list`, имена — `rbot-<UTC>.dump`, после `put` оставляет 5 новейших. Проверено: произвольная команда — отказ с перечнем команд, `get ../.ssh/authorized_keys` — «имя копии не принято», проброс портов закрыт `restrict`.
- Рабочий хост: ключ `/etc/rbot/backup_ed25519`, отпечаток `bones` — `/etc/rbot/backup_known_hosts` (`StrictHostKeyChecking=yes`), адрес — `RBOT_BACKUP_TARGET` в `/etc/rbot/rbot.env`; адреса и ключей в репозитории нет. Подготовка — `mise run provision-backup-store`.
- Объявление `backups` в `.conventions.json`: набор `database` — `every 24h`, `keep 5d` (кольцо из 5 ежедневных копий), `store bones`, квитанция `/var/lib/rbot/backups/database.receipt.json`, таймер `rbot-backup.timer`; `restoreRecord` — `/var/lib/rbot/backups/restored.json`, `restoreEvery 90d`.
- Задание `scripts/ops/backup.sh` (`rbot-backup.service`): `pg_dump -Fc rbot` своей базы, `put` в `bones`, квитанция `conventions backups --receipt`. Таймер — ежедневно 03:30 МСК, составляющая `timer-backup` с `enable`. Проверка восстановления — `scripts/ops/backup-restore-check.sh`: `latest` → `get` → `pg_restore` во временную базу `rbot_restore_check`, подсчёт строк, удаление базы, `conventions backups --restored`. Раскат рабочей среды вызывает `conventions backups --check` и сообщает отказ.
- Срок хранения копий называется в тексте соглашения (`REQ-BACKUPS-008`) — документы MVP-13.
- Проверки на рабочей среде после раската — RBOT-OPS-016.
