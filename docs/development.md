# Локальная разработка

## Требования

- .NET SDK 10;
- Git;
- современный браузер.

## Запуск

```powershell
dotnet run --project build -- local-web
```

Команда проверяет контент, синхронизирует каталог `content` в локальный web-root и открывает приложение по адресу `http://localhost:5188`. Встроенный редактор доступен по адресу `http://localhost:5188/admin`.

Запуск без автоматического открытия браузера:

```powershell
dotnet run --project build -- local-web --no-browser
```

Перед локальной сборкой web-проект автоматически синхронизирует исходные данные из `content` в игнорируемый каталог `src/Pianikova.Web/wwwroot/content`. Отдельная подготовительная команда не требуется.

## Сборка и проверка

Сборка всего решения:

```powershell
dotnet run --project build -- build
```

Проверка структуры JSON, языковых версий и медиаресурсов:

```powershell
dotnet run --project build -- content-check
```

Эта же проверка запускается в `.github/workflows/ci.yml` при каждом push.

## JetBrains Rider

Общие конфигурации запуска находятся в `.run`:

- `Pianikova.Web` — запуск сайта с возможностью отладки Blazor WebAssembly;
- `Build solution` — сборка решения;
- `Publish GitHub Pages` — создание production-каталога `artifacts/web/wwwroot`.

## Рекомендуемая проверка изменений

1. Выполнить `content-check`.
2. Собрать решение.
3. Запустить сайт локально.
4. Проверить русскую и английскую версии через параметры `?lang=ru` и `?lang=en`.
5. Проверить широкую и мобильную компоновку.
6. Для изменений PWA дополнительно проверить вкладки **Application → Manifest** и **Application → Service Workers** в Chrome DevTools.
