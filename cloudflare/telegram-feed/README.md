# Автоматическая лента Telegram

Отдельный Cloudflare Worker получает новые посты канала `@gattavasis` через webhook Telegram. В ленту попадают только посты со ссылкой на `pianikova.com` или `www.pianikova.com` в тексте, подписи, скрытой ссылке или кнопке. Сайт получает последние пять подходящих публикаций через `GET /feed`.

Бот не отправляет сообщения. Его токен нужен только для первоначального вызова Telegram `setWebhook` и **не хранится** в Worker или браузере. Worker проверяет отдельный секретный заголовок webhook и ID канала. Для защиты от повторных или пришедших не по порядку обновлений используется пара `event_at` и `update_id`.

## Разовая настройка

1. Создать бота через `@BotFather` и добавить его в канал `@gattavasis`. Не выдавать ему право публикации, если интерфейс Telegram позволяет выбрать права отдельно.
2. Войти в нужный Cloudflare-аккаунт командой `npx wrangler login`. Выполнять следующие команды из каталога `cloudflare/telegram-feed`.
3. Создать базу: `npx wrangler d1 create pianikova-telegram-feed`. Подставить выданный `database_id` в `wrangler.toml`.
4. Создать таблицу в удалённой базе: `npx wrangler d1 execute pianikova-telegram-feed --remote --file=./schema.sql`.
5. Узнать числовой ID канала через метод Telegram `getChat` с `chat_id=@gattavasis`. Задать его через `npx wrangler secret put TELEGRAM_CHANNEL_ID`.
6. Сгенерировать случайный секрет из символов `A-Z`, `a-z`, `0-9`, `_`, `-` и сохранить его в менеджере паролей. Задать через `npx wrangler secret put TELEGRAM_WEBHOOK_SECRET`.
7. Развернуть Worker: `npx wrangler deploy`. Проверить, что его URL совпадает с `telegramFeedUrl` в `content/settings/site.json`; если Cloudflare выдал другой адрес, обновить настройку сайта.
8. Вызвать Telegram `setWebhook` с `url=https://<адрес-worker>/telegram/webhook`, `secret_token=<секрет из шага 6>` и `allowed_updates=["channel_post","edited_channel_post"]`. Токен бота вводить только на своей машине, не добавлять в URL в документации, репозиторий или код сайта. Проверить результат через `getWebhookInfo`.
9. Опубликовать новую сборку сайта через GitHub Actions **Publish**. Сайт обращается к Worker напрямую, поэтому последующие посты не требуют новой сборки.

Официальные инструкции: [создание D1](https://developers.cloudflare.com/d1/get-started/), [секреты Workers](https://developers.cloudflare.com/workers/configuration/secrets/), [Telegram setWebhook](https://core.telegram.org/bots/api#setwebhook).

Пример команд PowerShell для шагов 5 и 8. Они запрашивают токен интерактивно, поэтому он не попадает в историю команд:

```powershell
$botToken = [Net.NetworkCredential]::new('', (Read-Host 'Telegram bot token' -AsSecureString)).Password
$channel = Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getChat" -Body @{ chat_id = '@gattavasis' }
$channel.result.id # это значение ввести в TELEGRAM_CHANNEL_ID

$webhookSecret = [Net.NetworkCredential]::new('', (Read-Host 'Webhook secret' -AsSecureString)).Password
Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/setWebhook" -Body @{
    url = 'https://pianikova-telegram-feed.nikolay-pyanikov.workers.dev/telegram/webhook'
    secret_token = $webhookSecret
    allowed_updates = '["channel_post","edited_channel_post"]'
}
Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/getWebhookInfo"
Remove-Variable botToken, webhookSecret
```

Если адрес Worker после деплоя отличается, заменить его в примере и в `content/settings/site.json`. Секрет из примера должен точно совпадать с `TELEGRAM_WEBHOOK_SECRET` в Cloudflare.

## Поведение

- `POST /telegram/webhook` принимает только запросы с правильным заголовком `X-Telegram-Bot-Api-Secret-Token` и ID канала. Ошибки базы дают неуспешный HTTP-ответ, чтобы Telegram повторил доставку.
- При правке поста Worker обновляет его. Если ссылка на сайт убрана, публикация исключается из ленты.
- `GET /feed` возвращает публичный JSON `{ "posts": [...] }`, разрешает чтение из браузера и кэшируется на 60 секунд. Токены и секреты в ответ не попадают.
- Удаление поста из канала Bot API не сообщает. Такой пост можно убрать вручную из D1: `DELETE FROM posts WHERE message_id = <ID> AND chat_id = '<ID канала>';`.
- Старая история канала в Bot API недоступна. Лента автоматически пополняется новыми публикациями после подключения webhook. При необходимости прежние посты можно импортировать отдельно.

## Локальная проверка

```powershell
npm test
```

Если webhook не получает посты, запустить `./verify-bot.ps1`. Скрипт спрашивает токен скрыто и печатает имя бота, ID канала, совпадение адреса webhook с Worker, подписанные типы обновлений и состояние проверки участника. Ошибка `getChatMember` выводится отдельно и не прерывает проверку webhook. Токен в вывод не попадает.

Тесты проверяют фильтр URL, безопасность webhook и обработку новых и исправленных публикаций. Для локального запуска Worker с D1 использовать `npx wrangler dev` и `.dev.vars` с теми же двумя секретами; `.dev.vars` не следует добавлять в Git.
