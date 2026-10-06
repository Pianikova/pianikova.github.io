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
    private const string ChannelUsername = "gattavasis";
    private const long ChannelId = 2266924011;
    private static readonly TimeZoneInfo MoscowTime = GetMoscowTimeZone();
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public async Task<int> RunAsync(CancellationToken cancellationToken)
    {
        if (Console.IsInputRedirected)
        {
            Console.Error.WriteLine("telegram-history requires an interactive terminal for credentials.");
            return 1;
        }

        string? apiHash = null;
        string? phone = null;
        try
        {
            PrintInstructions();
            var firstDay = ReadDate("From (YYYY-MM-DD, Moscow time): ");
            var lastDay = ReadDate("Through (YYYY-MM-DD, Moscow time): ");
            if (lastDay < firstDay) throw new InvalidOperationException("The end date must be on or after the start date.");

            Console.Write("Telegram api_id: ");
            var apiId = Console.ReadLine()?.Trim();
            if (!int.TryParse(apiId, NumberStyles.None, CultureInfo.InvariantCulture, out var parsedApiId) || parsedApiId <= 0)
                throw new InvalidOperationException("api_id must be a positive number.");
            apiHash = ReadSecret("Telegram api_hash: ");
            if (string.IsNullOrWhiteSpace(apiHash)) throw new InvalidOperationException("api_hash is required.");
            Console.Write("Telegram phone number (international format): ");
            phone = Console.ReadLine()?.Trim();
            if (string.IsNullOrWhiteSpace(phone)) throw new InvalidOperationException("Phone number is required.");

            var sessionDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Pianikova");
            Directory.CreateDirectory(sessionDirectory);
            var sessionPath = Path.Combine(sessionDirectory, "telegram-history.session");

            string? Config(string name) => name switch
            {
                "api_id" => parsedApiId.ToString(CultureInfo.InvariantCulture),
                "api_hash" => apiHash,
                "phone_number" => phone,
                "verification_code" => ReadSecret("Telegram login code: "),
                "email" => ReadText("Telegram email (if requested): "),
                "email_verification_code" => ReadSecret("Telegram email code: "),
                "password" => ReadSecret("Telegram two-step password: "),
                "session_pathname" => sessionPath,
                "first_name" or "last_name" => throw new InvalidOperationException("This target only supports existing Telegram accounts."),
                _ => null
            };

            WTelegram.Helpers.Log = (_, _) => { };
            using var client = new WTelegram.Client(Config);
            await client.LoginUserIfNeeded();
            cancellationToken.ThrowIfCancellationRequested();

            var resolved = await client.Contacts_ResolveUsername(ChannelUsername);
            if (resolved.Chat is not Channel channel || channel.id != ChannelId)
                throw new InvalidOperationException("Telegram resolved a different channel. Import stopped.");

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
                var history = await client.Messages_GetHistory(channel, offset_id: offsetId, limit: 100);
                if (history.Messages.Length == 0) break;

                foreach (var message in history.Messages.OfType<Message>())
                {
                    scanned++;
                    var date = AsUtc(message.date);
                    var localDay = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(date, MoscowTime));
                    if (localDay < firstDay || localDay > lastDay) continue;

                    var siteUrl = FindSiteUrl(message);
                    if (siteUrl is null) continue;
                    matched++;
                    if (!ids.Add(message.ID)) continue;

                    var text = Whitespace().Replace(message.message ?? string.Empty, " ").Trim();
                    if (text.Length == 0) text = siteUrl;
                    existing["posts"]!.AsArray().Add(new JsonObject
                    {
                        ["id"] = message.ID,
                        ["date"] = new DateTimeOffset(date).ToString("O", CultureInfo.InvariantCulture),
                        ["text"] = text,
                        ["url"] = $"https://t.me/{ChannelUsername}/{message.ID}",
                        ["siteUrl"] = siteUrl
                    });
                    added++;
                }

                var lastId = history.Messages[^1].ID;
                if (lastId <= 0 || lastId == offsetId) break;
                offsetId = lastId;
                var oldestPost = history.Messages.OfType<Message>().LastOrDefault();
                if (oldestPost is not null &&
                    DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(AsUtc(oldestPost.date), MoscowTime)) < firstDay)
                    break;
            }

            if (added > 0) await SaveAsync(existing, cancellationToken);
            Console.WriteLine($"Scanned {scanned} posts; matched {matched}; added {added}. Existing entries were preserved.");
            Console.WriteLine($"Static feed: {paths.TelegramPosts}");
            if (added > 0) Console.WriteLine("Чтобы записи появились на опубликованном сайте, отправьте content/telegram/posts.json в main.");
            else Console.WriteLine("Новых записей нет. Проверьте период и наличие ссылок на pianikova.com в постах.");
            return 0;
        }
        catch (OperationCanceledException)
        {
            Console.Error.WriteLine("Telegram history import was cancelled.");
            return 1;
        }
        catch (Exception exception)
        {
            var message = exception.Message;
            if (!string.IsNullOrEmpty(apiHash)) message = message.Replace(apiHash, "[redacted]", StringComparison.Ordinal);
            if (!string.IsNullOrEmpty(phone)) message = message.Replace(phone, "[redacted]", StringComparison.Ordinal);
            Console.Error.WriteLine($"Telegram history import failed: {message}");
            return 1;
        }
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
        Console.WriteLine("В JSON попадут только посты выбранного периода со ссылкой на pianikova.com.");
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

    private static string? FindSiteUrl(Message message)
    {
        var candidates = new List<string>();
        var text = message.message ?? string.Empty;
        var fullUrls = FullUrl().Matches(text);
        candidates.AddRange(fullUrls.Select(match => match.Value));
        foreach (Match match in BareSite().Matches(text))
        {
            var site = match.Groups["site"];
            if (fullUrls.Any(url => site.Index >= url.Index && site.Index < url.Index + url.Length)) continue;
            candidates.Add(site.Value);
        }
        if (message.entities is not null)
            candidates.AddRange(message.entities.OfType<MessageEntityTextUrl>().Select(entity => entity.url));
        if (message.media is MessageMediaWebPage { webpage: WebPage page })
            candidates.Add(page.url);
        if (message.reply_markup is ReplyInlineMarkup markup)
        {
            candidates.AddRange(markup.rows.SelectMany(row => row.buttons).OfType<KeyboardButtonUrl>().Select(button => button.url));
            candidates.AddRange(markup.rows.SelectMany(row => row.buttons).OfType<KeyboardButtonUrlAuth>().Select(button => button.url));
        }
        return candidates.Select(NormalizeSiteUrl).FirstOrDefault(url => url is not null);
    }

    private static string? NormalizeSiteUrl(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return null;
        var candidate = raw.Trim().TrimEnd('.', ',', '!', '?', ';', ':', ')', '}', ']');
        if (!candidate.StartsWith("http://", StringComparison.OrdinalIgnoreCase) &&
            !candidate.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
            candidate = "https://" + candidate;
        if (!Uri.TryCreate(candidate, UriKind.Absolute, out var url) ||
            url.Scheme is not ("http" or "https") ||
            url.UserInfo.Length > 0 || !url.IsDefaultPort ||
            !(url.Host.Equals("pianikova.com", StringComparison.OrdinalIgnoreCase) ||
              url.Host.Equals("www.pianikova.com", StringComparison.OrdinalIgnoreCase))) return null;
        return url.AbsoluteUri;
    }

    private static DateTime AsUtc(DateTime value) => value.Kind switch
    {
        DateTimeKind.Utc => value,
        DateTimeKind.Local => value.ToUniversalTime(),
        _ => DateTime.SpecifyKind(value, DateTimeKind.Utc)
    };

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

    private static string ReadSecret(string prompt)
    {
        Console.Write(prompt);
        var value = new StringBuilder();
        while (true)
        {
            var key = Console.ReadKey(intercept: true);
            if (key.Key == ConsoleKey.Enter) { Console.WriteLine(); return value.ToString(); }
            if (key.Key == ConsoleKey.Backspace) { if (value.Length > 0) value.Length--; continue; }
            if (!char.IsControl(key.KeyChar)) value.Append(key.KeyChar);
        }
    }

    private static TimeZoneInfo GetMoscowTimeZone()
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById("Europe/Moscow"); }
        catch (TimeZoneNotFoundException) { return TimeZoneInfo.FindSystemTimeZoneById("Russian Standard Time"); }
    }

    [GeneratedRegex(@"(?:https?://|www\.)[^\s<>""']*", RegexOptions.IgnoreCase | RegexOptions.CultureInvariant)]
    private static partial Regex FullUrl();

    [GeneratedRegex(@"(?:^|[\s(«„“])(?<site>pianikova\.com(?![\p{L}\p{N}_.:@-])(?:[/?#][^\s<>""']*)?)", RegexOptions.IgnoreCase | RegexOptions.CultureInvariant)]
    private static partial Regex BareSite();

    [GeneratedRegex(@"\s+")]
    private static partial Regex Whitespace();
}
