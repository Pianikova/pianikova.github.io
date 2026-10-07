using System.Globalization;
using System.Text.Json;

namespace Build.Targets;

internal sealed record ExportSegment(string Type, string Text, string? Href);

internal sealed record ExportPoll(string Question, IReadOnlyList<string> Answers);

internal sealed record ExportMessage(
    long Id,
    DateTimeOffset Date,
    bool Forwarded,
    string? MediaType,
    string? Photo,
    string? File,
    string? Thumbnail,
    int? Width,
    int? Height,
    int? Duration,
    string? Performer,
    string? Title,
    string? FileName,
    bool MediaSpoiler,
    ExportPoll? Poll,
    IReadOnlyList<ExportSegment> Segments,
    int Reactions)
{
    public string PlainText => string.Concat(Segments.Select(segment => segment.Text));
    public bool HasText => !string.IsNullOrWhiteSpace(PlainText);

    public JournalMediaKind Media => this switch
    {
        { Photo: not null } => JournalMediaKind.Photo,
        { MediaType: "audio_file" or "voice_message" } => JournalMediaKind.Audio,
        { MediaType: "video_file" or "animation" or "video_message" } => JournalMediaKind.Video,
        { MediaType: "sticker" } => JournalMediaKind.Sticker,
        { Poll: not null } => JournalMediaKind.Poll,
        { File: not null } => JournalMediaKind.Document,
        _ => JournalMediaKind.None
    };

    public IEnumerable<string> Tags => Segments
        .Where(segment => segment.Type == "hashtag")
        .Select(segment => segment.Text.TrimStart('#').Trim().ToLowerInvariant())
        .Where(tag => tag.Length > 0);
}

internal enum JournalMediaKind { None, Photo, Audio, Video, Sticker, Poll, Document }

/// <summary>Reads a Telegram Desktop JSON export of the @gattavasis channel.</summary>
internal static class JournalExport
{
    public const string ChannelUsername = "gattavasis";
    public const long ChannelId = 2266924011;
    public static readonly TimeZoneInfo MoscowTime = GetMoscowTimeZone();

    public static IReadOnlyList<ExportMessage> Read(string path)
    {
        using var document = JsonDocument.Parse(System.IO.File.ReadAllText(path));
        var root = document.RootElement;
        if (root.ValueKind != JsonValueKind.Object ||
            !root.TryGetProperty("type", out var type) || type.GetString() != "public_channel" ||
            !root.TryGetProperty("id", out var channelId) || !channelId.TryGetInt64(out var id) || id != ChannelId ||
            !root.TryGetProperty("messages", out var messages) || messages.ValueKind != JsonValueKind.Array)
            throw new InvalidOperationException("Нужен JSON-экспорт истории именно канала @gattavasis, а не общий экспорт аккаунта или другого чата.");

        var result = new List<ExportMessage>();
        foreach (var message in messages.EnumerateArray())
        {
            if (message.ValueKind != JsonValueKind.Object || String(message, "type") != "message") continue;
            if (!message.TryGetProperty("id", out var messageId) || !messageId.TryGetInt64(out var postId) || postId <= 0 ||
                !long.TryParse(String(message, "date_unixtime"), NumberStyles.None, CultureInfo.InvariantCulture, out var seconds))
                throw new InvalidOperationException("В экспорте есть пост без ID или даты date_unixtime.");

            var date = TimeZoneInfo.ConvertTime(DateTimeOffset.FromUnixTimeSeconds(seconds), MoscowTime);
            result.Add(new ExportMessage(
                postId,
                date,
                message.TryGetProperty("forwarded_from", out _) || message.TryGetProperty("forwarded_from_id", out _),
                String(message, "media_type"),
                String(message, "photo"),
                String(message, "file"),
                String(message, "thumbnail"),
                Int(message, "width"),
                Int(message, "height"),
                Int(message, "duration_seconds"),
                String(message, "performer"),
                String(message, "title"),
                String(message, "file_name"),
                message.TryGetProperty("media_spoiler", out var spoiler) && spoiler.ValueKind == JsonValueKind.True,
                Poll(message),
                Segments(message),
                Reactions(message)));
        }

        return result.OrderBy(message => message.Id).ToArray();
    }

    private static IReadOnlyList<ExportSegment> Segments(JsonElement message)
    {
        var segments = new List<ExportSegment>();
        if (message.TryGetProperty("text_entities", out var entities) && entities.ValueKind == JsonValueKind.Array)
        {
            foreach (var entity in entities.EnumerateArray())
            {
                if (entity.ValueKind != JsonValueKind.Object) continue;
                segments.Add(new ExportSegment(String(entity, "type") ?? "plain", String(entity, "text") ?? string.Empty, String(entity, "href")));
            }
            return segments;
        }

        if (!message.TryGetProperty("text", out var text)) return segments;
        if (text.ValueKind == JsonValueKind.String) segments.Add(new ExportSegment("plain", text.GetString() ?? string.Empty, null));
        else if (text.ValueKind == JsonValueKind.Array)
            foreach (var part in text.EnumerateArray())
            {
                if (part.ValueKind == JsonValueKind.String) segments.Add(new ExportSegment("plain", part.GetString() ?? string.Empty, null));
                else if (part.ValueKind == JsonValueKind.Object)
                    segments.Add(new ExportSegment(String(part, "type") ?? "plain", String(part, "text") ?? string.Empty, String(part, "href")));
            }
        return segments;
    }

    private static ExportPoll? Poll(JsonElement message)
    {
        if (!message.TryGetProperty("poll", out var poll) || poll.ValueKind != JsonValueKind.Object) return null;
        var answers = new List<string>();
        if (poll.TryGetProperty("answers", out var items) && items.ValueKind == JsonValueKind.Array)
            answers.AddRange(items.EnumerateArray().Select(answer => String(answer, "text")).OfType<string>());
        return new ExportPoll(String(poll, "question") ?? string.Empty, answers);
    }

    private static int Reactions(JsonElement message)
    {
        if (!message.TryGetProperty("reactions", out var reactions) || reactions.ValueKind != JsonValueKind.Array) return 0;
        return reactions.EnumerateArray().Sum(reaction => Int(reaction, "count") ?? 0);
    }

    private static string? String(JsonElement element, string name) =>
        element.ValueKind == JsonValueKind.Object && element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;

    private static int? Int(JsonElement element, string name) =>
        element.ValueKind == JsonValueKind.Object && element.TryGetProperty(name, out var value) && value.TryGetInt32(out var number)
            ? number
            : null;

    private static TimeZoneInfo GetMoscowTimeZone()
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById("Europe/Moscow"); }
        catch (TimeZoneNotFoundException) { return TimeZoneInfo.FindSystemTimeZoneById("Russian Standard Time"); }
    }
}
