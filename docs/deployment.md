# Публикация

## Production-сборка

```powershell
dotnet run --project build -- web
```

Результат создаётся в:

```text
artifacts/web/wwwroot
```

Production-сборка включает каталог `content`: опубликованное приложение читает редакционные данные и файлы журнала с того же адреса, что и сайт.

## GitHub Pages

Workflow `.github/workflows/pages.yml` называется **Publish**. Он запускается вручную через `workflow_dispatch` на вкладке GitHub Actions и автоматически при изменениях в `content` ветки `main`.

Он выполняет:

1. проверку контента;
2. production-сборку;
3. загрузку `artifacts/web/wwwroot` как Pages artifact;
4. публикацию в GitHub Pages.

Workflow имеет разрешения `pages: write` и `id-token: write`. Публикация использует стандартное окружение `github-pages`.

## CI

Workflow `.github/workflows/ci.yml` запускается при каждом push и выполняет:

```powershell
dotnet run --project build -- content-check
dotnet run --project build -- build
```

## SPA-маршруты

GitHub Pages не поддерживает серверный fallback для клиентских маршрутов. Проект использует `404.html`, который сохраняет исходный путь и возвращает посетителя в `index.html`; приложение восстанавливает маршрут до запуска Blazor. Это необходимо для прямого открытия `/admin` и его подразделов.
