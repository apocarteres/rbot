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
