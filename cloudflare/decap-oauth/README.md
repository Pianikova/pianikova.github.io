# OAuth-прокси для встроенного admin-редактора (GitHub)

Обмен кода авторизации GitHub OAuth на токен требует client secret, а хранить его на клиенте нельзя — поэтому браузер не может сделать это напрямую. Этот Cloudflare Worker — минимальный посредник: принимает вход через GitHub OAuth и отдаёт токен обратно открывшей его странице через `postMessage`.

Изначально воркер обслуживал Decap CMS; сейчас Decap CMS в проекте не используется — тот же воркер без изменений обслуживает редактор `/admin`, встроенный в само Blazor-приложение (`src/Pianikova.Web/Data/Admin/GitHubAuthState.cs` + `wwwroot/js/adminAuth.js`). Протокол `postMessage` общий, поэтому смена "открывающей" страницы не потребовала правок в `worker.js`.

## Разовая настройка

### 1. GitHub OAuth App

GitHub → Settings → Developer settings → OAuth Apps → New OAuth App:

- **Homepage URL**: `https://pianikova.github.io`
- **Authorization callback URL**: `https://pianikova-cms-oauth.nikolay-pyanikov.workers.dev/callback`

После создания получите **Client ID** и **Client Secret**.

### 2. Установка wrangler

```bash
npm install -g wrangler
wrangler login
```

### 3. Настройка секретов

В `wrangler.toml` заменить `GITHUB_CLIENT_ID` на реальный Client ID (это не секрет, он публичный).

Client Secret — **никогда** не в файлах репозитория:

```bash
cd cloudflare/decap-oauth
wrangler secret put GITHUB_CLIENT_SECRET
```

Значение нужно вставить в интерактивном запросе — оно не попадёт ни в файлы, ни в историю git.

### 4. Деплой

```bash
wrangler deploy
```

Команда выведет адрес воркера — должен совпасть с `https://pianikova-cms-oauth.nikolay-pyanikov.workers.dev`. Если отличается — обновите callback URL в настройках GitHub OAuth App.

### 5. Подключить к сайту

Уже сделано: адрес воркера зашит константой `AuthUrl` в `src/Pianikova.Web/Data/Admin/GitHubAuthState.cs` и указывает на `https://pianikova-cms-oauth.nikolay-pyanikov.workers.dev/auth`.

## Проверка

Открыть `https://pianikova.github.io/admin`, нажать «Войти через GitHub» — должно открыться окно авторизации GitHub, а не ошибка.
