using System.Globalization;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using TL;
using JsonArray = System.Text.Json.Nodes.JsonArray;
using JsonObject = System.Text.Json.Nodes.JsonObject;

namespace Build.Targets;

internal sealed partial class TelegramHistoryTarget(BuildPaths paths)
{
    private const string ChannelUsername = JournalExport.ChannelUsername;
    private const long ChannelId = JournalExport.ChannelId;
    private static readonly TimeZoneInfo MoscowTime = JournalExport.MoscowTime;
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public async Task<int> RunAsync(CancellationToken cancellationToken)
    {
        if (Console.IsInputRedirected)
        {
            Console.Error.WriteLine("telegram-history requires an interactive terminal for credentials.");
            return 1;
        }

        using var session = new TelegramChannelSession(allowPrompts: true, useEnvironment: false);
        try
        {
            PrintInstructions();
            var firstDay = ReadDate("From (YYYY-MM-DD, Moscow time): ");
            var lastDay = ReadDate("Through (YYYY-MM-DD, Moscow time): ");
            if (lastDay < firstDay) throw new InvalidOperationException("The end date must be on or after the start date.");

            var channel = await session.ConnectAsync(cancellationToken);

            var existing = ReadExistingPosts();
            var ids = existing["posts"]!.AsArray()
                .Select(post => post!["id"]!.GetValue<long>())
                .ToHashSet();

            var scanned = 0;
            var matched = 0;
            var added = 0;
            var offsetId = 0;
            while (true)
            {
                cancellationToken.ThrowIfCancellationRequested();
                var history = await session.Client.Messages_GetHistory(channel, offset_id: offsetId, limit: 100);
                if (history.Messages.Length == 0) break;

                foreach (var message in history.Messages.OfType<Message>())
                {
                    scanned++;
                    var date = TelegramChannelSession.AsUtc(message.date);
                    var localDay = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(date, MoscowTime));
                    if (localDay < firstDay || localDay > lastDay) continue;

                    matched++;
                    if (!ids.Add(message.ID)) continue;

                    var text = Whitespace().Replace(message.message ?? string.Empty, " ").Trim();
                    if (text.Length == 0) text = "Telegram";
                    existing["posts"]!.AsArray().Add(new JsonObject
                    {
                        ["id"] = message.ID,
                        ["date"] = new DateTimeOffset(date).ToString("O", CultureInfo.InvariantCulture),
                        ["text"] = text,
                        ["url"] = $"https://t.me/{ChannelUsername}/{message.ID}"
                    });
                    added++;
                }

                var lastId = history.Messages[^1].ID;
                if (lastId <= 0 || lastId == offsetId) break;
                offsetId = lastId;
                var oldestPost = history.Messages.OfType<Message>().LastOrDefault();
                if (oldestPost is not null &&
                    DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(TelegramChannelSession.AsUtc(oldestPost.date), MoscowTime)) < firstDay)
                    break;
            }

            if (added > 0) await SaveAsync(existing, cancellationToken);
            Console.WriteLine($"Scanned {scanned} posts; matched {matched}; added {added}. Existing entries were preserved.");
            Console.WriteLine($"Static feed: {paths.TelegramPosts}");
            if (added > 0) Console.WriteLine("Чтобы записи появились на опубликованном сайте, отправьте content/telegram/posts.json в main.");
            else Console.WriteLine("Новых записей нет. Проверьте период; существующие ID повторно не добавляются.");
            return 0;
        }
        catch (OperationCanceledException)
        {
            Console.Error.WriteLine("Telegram history import was cancelled.");
            return 1;
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine($"Telegram history import failed: {session.Redact(exception.Message)}");
            return 1;
        }
    }

    public async Task<int> RunExportAsync(CancellationToken cancellationToken)
    {
        if (Console.IsInputRedirected)
        {
            Console.Error.WriteLine("telegram-history-export requires an interactive terminal.");
            return 1;
        }

        try
        {
            Console.WriteLine();
            Console.WriteLine("Экспортируйте историю канала @gattavasis в Telegram Desktop: меню канала → Export chat history → JSON.");
            Console.WriteLine("Выберите нужный период; файлы медиа для текстовой ленты не требуются.");
            Console.WriteLine("Экспорт сохраните вне репозитория. Укажите полный путь к result.json.");
            var exportPath = ReadText("Telegram Desktop result.json: ")?.Trim('"');
            if (string.IsNullOrWhiteSpace(exportPath) || !File.Exists(exportPath))
                throw new InvalidOperationException("Файл экспорта не найден.");

            var firstDay = ReadDate("From (YYYY-MM-DD, Moscow time): ");
            var lastDay = ReadDate("Through (YYYY-MM-DD, Moscow time): ");
            if (lastDay < firstDay) throw new InvalidOperationException("The end date must be on or after the start date.");

            using var document = JsonDocument.Parse(File.ReadAllText(exportPath));
            var root = document.RootElement;
            if (root.ValueKind != JsonValueKind.Object ||
                !root.TryGetProperty("type", out var type) || type.GetString() != "public_channel" ||
                !root.TryGetProperty("id", out var channelId) || !channelId.TryGetInt64(out var id) || id != ChannelId ||
                !root.TryGetProperty("messages", out var messages) || messages.ValueKind != JsonValueKind.Array)
                throw new InvalidOperationException("Нужен JSON-экспорт истории именно канала @gattavasis, а не общий экспорт аккаунта или другого чата.");

            var existing = ReadExistingPosts();
            var ids = existing["posts"]!.AsArray()
                .Select(post => post!["id"]!.GetValue<long>())
                .ToHashSet();
            var scanned = 0;
            var matched = 0;
            var added = 0;
            foreach (var message in messages.EnumerateArray())
            {
                cancellationToken.ThrowIfCancellationRequested();
                if (message.ValueKind != JsonValueKind.Object ||
                    !message.TryGetProperty("type", out var messageType) || messageType.GetString() != "message") continue;
                scanned++;
                if (!message.TryGetProperty("id", out var messageId) || !messageId.TryGetInt64(out var postId) || postId <= 0 ||
                    !message.TryGetProperty("date_unixtime", out var unixDate) ||
                    !long.TryParse(unixDate.GetString(), NumberStyles.None, CultureInfo.InvariantCulture, out var seconds))
                    throw new InvalidOperationException("В экспорте есть пост без ID или даты date_unixtime.");

                var date = DateTimeOffset.FromUnixTimeSeconds(seconds);
                var localDay = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(date.UtcDateTime, MoscowTime));
                if (localDay < firstDay || localDay > lastDay) continue;

                var text = ExportText(message);
                matched++;
                if (!ids.Add(postId)) continue;

                var normalizedText = Whitespace().Replace(text, " ").Trim();
                if (normalizedText.Length == 0) normalizedText = "Telegram";
                existing["posts"]!.AsArray().Add(new JsonObject
                {
                    ["id"] = postId,
                    ["date"] = date.ToString("O", CultureInfo.InvariantCulture),
                    ["text"] = normalizedText,
                    ["url"] = $"https://t.me/{ChannelUsername}/{postId}"
                });
                added++;
            }

            if (added > 0) await SaveAsync(existing, cancellationToken);
            Console.WriteLine($"Scanned {scanned} posts; matched {matched}; added {added}. Existing entries were preserved.");
            Console.WriteLine($"Static feed: {paths.TelegramPosts}");
            if (added > 0) Console.WriteLine("Чтобы записи появились на опубликованном сайте, отправьте content/telegram/posts.json в main.");
            else Console.WriteLine("Новых записей нет. Проверьте период; существующие ID повторно не добавляются.");
            return 0;
        }
        catch (OperationCanceledException)
        {
            Console.Error.WriteLine("Telegram Desktop export import was cancelled.");
            return 1;
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException or JsonException or InvalidOperationException or ArgumentOutOfRangeException)
        {
            Console.Error.WriteLine($"Telegram Desktop export import failed: {exception.Message}");
            return 1;
        }
    }

    private static string ExportText(JsonElement message)
    {
        if (!message.TryGetProperty("text", out var text)) return string.Empty;
        if (text.ValueKind == JsonValueKind.String) return text.GetString() ?? string.Empty;
        if (text.ValueKind != JsonValueKind.Array) return string.Empty;
        var result = new StringBuilder();
        foreach (var part in text.EnumerateArray())
        {
            if (part.ValueKind == JsonValueKind.String) result.Append(part.GetString());
            else if (part.ValueKind == JsonValueKind.Object && part.TryGetProperty("text", out var value) && value.ValueKind == JsonValueKind.String)
                result.Append(value.GetString());
        }
        return result.ToString();
    }

    private static void PrintInstructions()
    {
        Console.WriteLine();
        Console.WriteLine("Импорт старых постов канала @gattavasis");
        Console.WriteLine("1. Откройте https://my.telegram.org/ и войдите под своим номером Telegram.");
        Console.WriteLine("   Код для входа на сайт придёт в Telegram.");
        Console.WriteLine("2. Откройте API development tools и создайте приложение, если его ещё нет.");
        Console.WriteLine("   На странице приложения найдите api_id (число) и api_hash (секрет).");
        Console.WriteLine("3. Введите ниже период по Москве, api_id, api_hash и тот же номер телефона.");
        Console.WriteLine("   Если потребуется код входа или пароль двухэтапной проверки, введите его здесь.");
        Console.WriteLine("   Токен бота не нужен. Не публикуйте api_hash, коды и пароль.");
        Console.WriteLine("В JSON попадут все посты выбранного периода.");
        Console.WriteLine();
    }

    private JsonObject ReadExistingPosts()
    {
        var root = JsonNode.Parse(File.ReadAllText(paths.TelegramPosts)) as JsonObject
                   ?? throw new InvalidOperationException("content/telegram/posts.json must be a JSON object.");
        if (root["schemaVersion"]?.GetValue<int>() != 1 || root["posts"] is not JsonArray posts)
            throw new InvalidOperationException("content/telegram/posts.json has an unsupported schema.");
        var ids = new HashSet<long>();
        foreach (var post in posts)
        {
            if (post is not JsonObject item || item["id"] is null || !ids.Add(item["id"]!.GetValue<long>()))
                throw new InvalidOperationException("content/telegram/posts.json has a missing or duplicate post ID.");
        }
        return root;
    }

    private async Task SaveAsync(JsonObject root, CancellationToken cancellationToken)
    {
        var temporary = paths.TelegramPosts + ".tmp";
        try
        {
            await File.WriteAllTextAsync(temporary, root.ToJsonString(JsonOptions) + Environment.NewLine, new UTF8Encoding(false), cancellationToken);
            File.Move(temporary, paths.TelegramPosts, true);
        }
        finally
        {
            if (File.Exists(temporary)) File.Delete(temporary);
        }
    }

    private static DateOnly ReadDate(string prompt)
    {
        Console.Write(prompt);
        var raw = Console.ReadLine()?.Trim();
        return DateOnly.TryParseExact(raw, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var date)
            ? date
            : throw new InvalidOperationException("Dates must use YYYY-MM-DD.");
    }

    private static string? ReadText(string prompt)
    {
        Console.Write(prompt);
        return Console.ReadLine()?.Trim();
    }

    [GeneratedRegex(@"\s+")]
    private static partial Regex Whitespace();
}
