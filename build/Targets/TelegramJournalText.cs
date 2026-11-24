using TL;

namespace Build.Targets;

internal static class TelegramJournalText
{
    public static IReadOnlyList<ExportSegment> Read(string text, IReadOnlyList<MessageEntity> entities)
    {
        // Telegram offsets and .NET string indices both count UTF-16 code units.
        var spans = entities
            .Where(entity => entity.Offset >= 0 && entity.Length > 0 && entity.Offset <= text.Length - entity.Length)
            .Select(ToSpan)
            .OfType<EntitySpan>()
            .OrderByDescending(span => span.Priority)
            .ThenBy(span => span.End - span.Start)
            .ToArray();
        var boundaries = spans.SelectMany(span => new[] { span.Start, span.End }).Append(0).Append(text.Length)
            .Distinct().Order().ToArray();
        var segments = new List<ExportSegment>();
        for (var index = 0; index + 1 < boundaries.Length;)
        {
            var start = boundaries[index];
            var span = At(start);
            do { index++; }
            while (index + 1 < boundaries.Length && ReferenceEquals(span, At(boundaries[index])));
            segments.Add(new ExportSegment(span?.Type ?? "plain", text[start..boundaries[index]], span?.Href));
        }
        return segments;

        EntitySpan? At(int position) => spans.FirstOrDefault(span => span.Start <= position && position < span.End);
    }

    private static EntitySpan? ToSpan(MessageEntity entity)
    {
        // The journal renderer accepts one entity per segment. Keep spoilers and code intact when entities overlap.
        var (type, priority) = entity switch
        {
            MessageEntitySpoiler => ("spoiler", 5),
            MessageEntityPre => ("pre", 4),
            MessageEntityCode => ("code", 4),
            MessageEntityBlockquote => ("blockquote", 3),
            MessageEntityTextUrl => ("text_link", 2),
            MessageEntityUrl => ("link", 2),
            MessageEntityEmail => ("email", 2),
            MessageEntityMention => ("mention", 2),
            MessageEntityHashtag => ("hashtag", 2),
            MessageEntityBold => ("bold", 1),
            MessageEntityItalic => ("italic", 1),
            MessageEntityUnderline => ("underline", 1),
            MessageEntityStrike => ("strikethrough", 1),
            _ => (null, 0)
        };
        return type is null ? null : new EntitySpan(entity.Offset, entity.Offset + entity.Length, type, priority, (entity as MessageEntityTextUrl)?.url);
    }

    private sealed record EntitySpan(int Start, int End, string Type, int Priority, string? Href);
}
