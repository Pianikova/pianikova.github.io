# Документация проекта

## Разработка и сопровождение

- [Локальная разработка](./development.md) — требования, запуск, сборка, Rider и проверка изменений.
- [Архитектура](./architecture.md) — устройство приложения, источники данных и локализация.
- [Контент](./content.md) — структура каталога `content`, языковые версии, Markdown и правила данных.
- [Администрирование](./administration.md) — работа со встроенным редактором `/admin` и публикация изменений.
- [Публикация](./deployment.md) — production-сборка и GitHub Pages.
- [PWA](./pwa.md) — установка сайта как приложения, manifest, иконки и service worker.

## Инфраструктура

- [Настройка GitHub OAuth через Cloudflare Worker](../cloudflare/decap-oauth/README.md).
- [Настройка автоматической ленты Telegram](../cloudflare/telegram-feed/README.md).
- [Импорт исторических постов Telegram](./telegram-history.md).
- [Журнал Telegram](./telegram-journal.md) — архив канала в `content/telegram/journal`.

## Продукт и дизайн

- [Пакет дизайн-концепции](../concept/README.md).

## Быстрые команды

| Задача | Команда |
|---|---|
| Локальный запуск | `dotnet run --project build -- local-web` |
| Проверка контента | `dotnet run --project build -- content-check` |
| Сборка решения | `dotnet run --project build -- build` |
| Production-сборка | `dotnet run --project build -- web` |
| Импорт истории Telegram | `dotnet run --project build -- telegram-history` |
| Журнал Telegram из экспорта | `dotnet run --project build -- telegram-journal` |
