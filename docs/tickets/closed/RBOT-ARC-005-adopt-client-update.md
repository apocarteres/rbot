---
id: RBOT-ARC-005
type: ticket
status: done
scope: frontend, backend
authority: supporting
priority: P1
release: RELEASE-2026-09-1
obligation: client-update
related: REQ-CLIENT-UPDATE
---

# Клиент узнаёт о новой сборке и об устаревшем API механизмом ядра

## Проблема

После раската открытая вкладка работает на прежней сборке и видит отказ загрузки куска как ошибку сети; устаревший клиент продолжает слать запросы, которые сервер уже не понимает. Каждый проект решает это сам или не решает вовсе.

## Основание

Обязательство ядра `client-update`, требование `REQ-CLIENT-UPDATE-001`, объявлено в версии ядра 5.0.0. Срок исполнения: 3 выпуск(ов) потребителя.

## Последствия при сохранении текущего поведения

Требование ядра действует, а проект ему не соответствует: расхождение обнаруживается не проверкой, а при следующем изменении в этой области.

## Требуется

1. Клиент объявляет provideAppUpdate({ apiVersion, available, required }) из @apocarteres/app-update; available — компонент проекта либо UPDATE_IGNORED, required — компонент проекта (REQ-CLIENT-UPDATE-001).
2. Перехватчик appUpdateInterceptor подключён в provideHttpClient(withInterceptors([...])) (REQ-CLIENT-UPDATE-006).
3. Служба подключает platform-api-version и задаёт platform.api.min-supported-version; машинные вызовы без заголовка проходят сами, platform.api.version-exempt-paths нужен только для путей, освобождаемых и от браузера (REQ-CLIENT-UPDATE-005, REQ-CLIENT-UPDATE-006).
4. Собственные опрос сборки, плашка и сверка версии API сняты: их заменяет механизм ядра.

## Критерии приёмки

- Правило client-update в check замечаний не даёт.
- Вкладка, открытая до раската, после раската показывает компонент available — проверено один раз при принятии.
- Запрос с версией ниже минимальной получает 426 с кодом client-outdated, клиент показывает required.

## Ход работы

- 2026-09-30: механизм с MVP-01: `provideAppUpdate` с компонентами `UpdateAvailable` и `UpdateRequired`, `appUpdateInterceptor`, `platform-api-version` с `platform.api.min-supported-version=1`; своего опроса сборки нет. Правило `client-update` замечаний не даёт.
- QA, вкладка до раската: старая сборка `main-FSDMSN4X.js`, после раската сервер отдаёт `main-F3WQF2MQ.js`, вкладка показала «Вышла новая версия». Панель браузера была скрыта, поэтому возврат на вкладку подан событием `visibilitychange` с `document.hidden = false`.
- QA, устаревший API: `PLATFORM_API_MIN_SUPPORTED_VERSION=2` на время проверки — запрос с `X-Api-Version: 1` получил 426 `client-outdated`, новая сборка показала «Страница устарела»; окно не закрылось ни Escape, ни щелчком по фону. Настройка возвращена, запрос версии 1 — 200.
