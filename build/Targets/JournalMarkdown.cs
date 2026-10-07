using System.Text;
using System.Text.RegularExpressions;

namespace Build.Targets;

/// <summary>
/// Converts Telegram text entities to Markdown for the site's Markdig pipeline
/// (UseAdvancedExtensions + UseSoftlineBreakAsHardlineBreak): every single line break of a post stays a line break.
/// </summary>
internal static partial class JournalMarkdown
{
    private const int MaxTitleLength = 100;

    /// <summary>Takes the first line as the title when it looks like a heading followed by more text.</summary>
    public static (string? Title, IReadOnlyList<ExportSegment> Body) SplitTitle(IReadOnlyList<ExportSegment> segments)
    {
        var plain = string.Concat(segments.Select(segment => segment.Text));
        var start = plain.Length - plain.TrimStart().Length;
        var end = plain.IndexOf('\n', start);
        if (end < 0) return (null, segments);

        var line = plain[start..end].Trim();
        var title = TagsOnly().Replace(line, string.Empty).Trim();
        if (title.Length is < 2 or > MaxTitleLength || string.IsNullOrWhiteSpace(plain[end..]) ||
            title[^1] is ':' or ',' or ';' or '-' or '—' || title.EndsWith("...", StringComparison.Ordinal))
            return (null, segments);

        var skip = end;
        while (skip < plain.Length && char.IsWhiteSpace(plain[skip])) skip++;
        var body = new List<ExportSegment>();
        var position = 0;
        foreach (var segment in segments)
        {
            var segmentEnd = position + segment.Text.Length;
            if (segmentEnd > skip)
                body.Add(position >= skip ? segment : segment with { Text = segment.Text[(skip - position)..] });
            position = segmentEnd;
        }
        return (title, body);
    }

    public static string ToMarkdown(IEnumerable<ExportSegment> segments)
    {
        var builder = new StringBuilder();
        foreach (var segment in segments)
        {
            var text = segment.Text;
            if (text.Length == 0) continue;
            switch (segment.Type)
            {
                case "bold": builder.Append(Wrap(text, "**")); break;
                case "italic": builder.Append(Wrap(text, "*")); break;
                case "underline": builder.Append(Wrap(text, "++")); break;
                case "strikethrough": builder.Append(Wrap(text, "~~")); break;
                case "spoiler": builder.Append(Wrap(text, "==", "{.spoiler}")); break;
                case "code" when string.IsNullOrWhiteSpace(text):
                    builder.Append(text);
                    break;
                case "code":
                    var fence = text.Contains('`') ? "`` " : "`";
                    builder.Append(fence).Append(text).Append(fence.Length > 1 ? " ``" : "`");
                    break;
                case "pre":
                    builder.Append("\n\n```\n").Append(text.TrimEnd('\n')).Append("\n```\n\n");
                    break;
                case "blockquote" or "expandable_blockquote":
                    builder.Append("\n\n");
                    foreach (var line in text.TrimEnd('\n').Split('\n')) builder.Append("> ").Append(Escape(line, true)).Append('\n');
                    builder.Append('\n');
                    break;
                case "text_link" when !string.IsNullOrWhiteSpace(segment.Href):
                    builder.Append(Link(text, segment.Href!));
                    break;
                case "link":
                    builder.Append(Link(text, text.Contains("://", StringComparison.Ordinal) ? text : "https://" + text));
                    break;
                case "mention":
                    builder.Append(Link(text, "https://t.me/" + text.TrimStart('@')));
                    break;
                case "email":
                    builder.Append(Link(text, "mailto:" + text));
                    break;
                case "hashtag":
                    builder.Append(text);
                    break;
                default:
                    builder.Append(Escape(text, builder.Length == 0 || builder[^1] == '\n'));
                    break;
            }
        }
        return Normalize(builder.ToString());
    }

    /// <summary>Removes lines that contain nothing but hashtags; the tags live in the front matter.</summary>
    public static string StripTagLines(string markdown) =>
        Normalize(string.Join('\n', markdown.Split('\n').Where(line => !TagLine().IsMatch(line))));

    public static string Normalize(string markdown)
    {
        var lines = markdown.Replace("\r\n", "\n").Split('\n').Select(line => line.TrimEnd());
        return ExtraBlankLines().Replace(string.Join('\n', lines), "\n\n").Trim('\n');
    }

    public static string Escape(string text, bool atLineStart = false)
    {
        var builder = new StringBuilder(text.Length + 8);
        var lineStart = atLineStart;
        for (var index = 0; index < text.Length; index++)
        {
            var current = text[index];
            if (current == '\n')
            {
                builder.Append(current);
                lineStart = true;
                continue;
            }

            if (lineStart)
            {
                if (char.IsWhiteSpace(current)) { builder.Append(current); continue; }
                var rest = text.AsSpan(index);
                var match = OrderedListMarker().Match(text, index);
                if (match.Success && match.Index == index)
                {
                    builder.Append(text, index, match.Length - 1).Append('\\').Append(text[index + match.Length - 1]);
                    index += match.Length - 1;
                    lineStart = false;
                    continue;
                }
                if (current == '>' || current is '#' or '-' or '+' or ':' && (rest.Length == 1 || rest[1] is ' ' or '#' or ':' or '-' or '\t'))
                {
                    builder.Append('\\').Append(current);
                    lineStart = false;
                    continue;
                }
            }

            if (current is '\\' or '`' or '*' or '_' or '[' or ']' or '<' or '>' or '~' or '^' or '$' or '|' or '{' or '}')
                builder.Append('\\');
            else if (current is '=' or '+' && (index + 1 < text.Length && text[index + 1] == current || index > 0 && text[index - 1] == current))
                builder.Append('\\');
            builder.Append(current);
            lineStart = false;
        }
        return builder.ToString();
    }

    public static string Slug(string? text, int maxLength = 48)
    {
        if (string.IsNullOrWhiteSpace(text)) return string.Empty;
        var builder = new StringBuilder();
        foreach (var current in text.ToLowerInvariant().Normalize(NormalizationForm.FormD))
        {
            if (Transliteration.TryGetValue(current, out var latin)) builder.Append(latin);
            else if (current is >= 'a' and <= 'z' or >= '0' and <= '9') builder.Append(current);
            else if (char.GetUnicodeCategory(current) == System.Globalization.UnicodeCategory.NonSpacingMark) continue;
            else builder.Append('-');
        }

        var slug = Dashes().Replace(builder.ToString(), "-").Trim('-');
        if (slug.Length <= maxLength) return slug;
        var cut = slug.LastIndexOf('-', maxLength);
        return (cut > maxLength / 2 ? slug[..cut] : slug[..maxLength]).Trim('-');
    }

    public static string Quote(string value) => "\"" + value
        .Replace("\\", "\\\\", StringComparison.Ordinal)
        .Replace("\"", "\\\"", StringComparison.Ordinal)
        .Replace("\n", "\\n", StringComparison.Ordinal)
        .Replace("\r", string.Empty, StringComparison.Ordinal)
        .Replace("\t", "\\t", StringComparison.Ordinal) + "\"";

    private static string Link(string text, string href)
    {
        var target = href.IndexOfAny([' ', '(', ')', '<', '>']) >= 0 ? "<" + href.Replace("<", "%3C").Replace(">", "%3E").Replace(" ", "%20") + ">" : href;
        return Wrap(text, string.Empty, string.Empty, core => $"[{Escape(core)}]({target})");
    }

    /// <summary>Wraps every line separately so emphasis never spans a line break and markers hug the text.</summary>
    private static string Wrap(string text, string marker, string suffix = "", Func<string, string>? format = null)
    {
        format ??= core => marker + Escape(core) + marker + suffix;
        return string.Join('\n', text.Split('\n').Select(line =>
        {
            var core = line.Trim();
            if (core.Length == 0) return line;
            if (marker.Length > 0 && !core.Any(char.IsLetterOrDigit)) return Escape(line);
            var leading = line[..(line.Length - line.TrimStart().Length)];
            var trailing = line[line.TrimEnd().Length..];
            return leading + format(core) + trailing;
        }));
    }

    private static readonly Dictionary<char, string> Transliteration = new()
    {
        ['а'] = "a", ['б'] = "b", ['в'] = "v", ['г'] = "g", ['д'] = "d", ['е'] = "e", ['ё'] = "e", ['ж'] = "zh",
        ['з'] = "z", ['и'] = "i", ['й'] = "y", ['к'] = "k", ['л'] = "l", ['м'] = "m", ['н'] = "n", ['о'] = "o",
        ['п'] = "p", ['р'] = "r", ['с'] = "s", ['т'] = "t", ['у'] = "u", ['ф'] = "f", ['х'] = "kh", ['ц'] = "ts",
        ['ч'] = "ch", ['ш'] = "sh", ['щ'] = "shch", ['ъ'] = "", ['ы'] = "y", ['ь'] = "", ['э'] = "e", ['ю'] = "yu",
        ['я'] = "ya"
    };

    [GeneratedRegex(@"^\s*(?:#[\p{L}\p{N}_]+[\s,.]*)+$")]
    private static partial Regex TagLine();

    [GeneratedRegex(@"(?:^|\s)#[\p{L}\p{N}_]+")]
    private static partial Regex TagsOnly();

    [GeneratedRegex(@"\G\d{1,9}[.)](?=\s|$)")]
    private static partial Regex OrderedListMarker();

    [GeneratedRegex(@"\n{3,}")]
    private static partial Regex ExtraBlankLines();

    [GeneratedRegex(@"-{2,}")]
    private static partial Regex Dashes();
}
