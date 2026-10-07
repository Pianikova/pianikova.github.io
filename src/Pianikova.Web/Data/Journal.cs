using Markdig;
using Markdig.Renderers.Html;
using Markdig.Syntax;
using Markdig.Syntax.Inlines;

namespace Pianikova.Web.Data;

public sealed record JournalIndex(
    int SchemaVersion,
    IReadOnlyList<JournalRubric> Rubrics,
    IReadOnlyList<JournalSeries> Series,
    IReadOnlyList<JournalIssue> Issues);

public sealed record JournalRubric(string Id, IReadOnlyDictionary<string, string> Title, int Count);
public sealed record JournalSeries(string Id, IReadOnlyDictionary<string, string> Title, IReadOnlyList<long> Issues);
public sealed record JournalCover(string File, int Width, int Height, string Color, string Lqip);

public sealed record JournalIssue(
    long Id,
    DateTimeOffset Date,
    string Path,
    string? Title,
    string Excerpt,
    IReadOnlyList<string> Tags,
    string? Rubric,
    string? Series,
    int ReadingMinutes,
    int Reactions,
    int Photos,
    int Audio,
    int Videos,
    JournalCover? Cover,
    string Source);

public sealed record JournalSearchIndex(int SchemaVersion, IReadOnlyList<JournalSearchEntry> Issues);
public sealed record JournalSearchEntry(long Id, string? Title, string Text);

public interface IJournalSource
{
    Task<JournalIndex> LoadIndexAsync(CancellationToken cancellationToken = default);
    Task<IReadOnlyDictionary<long, string>> LoadSearchAsync(CancellationToken cancellationToken = default);
    Task<string> LoadIssueHtmlAsync(JournalIssue issue, CancellationToken cancellationToken = default);
    string IssueAssetUrl(JournalIssue issue, string file);
}

/// <summary>Reads the journal prepared by the <c>telegram-journal</c> build target from <c>content/telegram/journal</c>.</summary>
internal sealed class JournalSource(HttpClient httpClient, ISiteContentSource content) : IJournalSource
{
    private const string Root = "telegram/journal/";
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web);

    private static readonly MarkdownPipeline Pipeline = new MarkdownPipelineBuilder()
        .UseAdvancedExtensions()
        .UseSoftlineBreakAsHardlineBreak()
        .UseYamlFrontMatter()
        .DisableHtml()
        .Build();

    private Task<JournalIndex>? _index;
    private Task<IReadOnlyDictionary<long, string>>? _search;

    public Task<JournalIndex> LoadIndexAsync(CancellationToken cancellationToken = default)
    {
        if (_index is { IsFaulted: false, IsCanceled: false }) return _index;
        return _index = ReadIndexAsync(cancellationToken);
    }

    public Task<IReadOnlyDictionary<long, string>> LoadSearchAsync(CancellationToken cancellationToken = default)
    {
        if (_search is { IsFaulted: false, IsCanceled: false }) return _search;
        return _search = ReadSearchAsync(cancellationToken);
    }

    public async Task<string> LoadIssueHtmlAsync(JournalIssue issue, CancellationToken cancellationToken = default)
    {
        var markdown = await httpClient.GetStringAsync(content.AssetUrl($"{Root}{issue.Path}/index.md"), cancellationToken);
        return Render(markdown, file => IssueAssetUrl(issue, file));
    }

    public string IssueAssetUrl(JournalIssue issue, string file) => content.AssetUrl($"{Root}{issue.Path}/{file}");

    private async Task<JournalIndex> ReadIndexAsync(CancellationToken cancellationToken)
    {
        var index = await httpClient.GetFromJsonAsync<JournalIndex>(content.AssetUrl($"{Root}index.json"), SerializerOptions, cancellationToken)
                    ?? throw new InvalidOperationException("content/telegram/journal/index.json is empty.");
        if (index.SchemaVersion != 1) throw new InvalidOperationException($"Unsupported journal schema version {index.SchemaVersion}.");
        return index;
    }

    private async Task<IReadOnlyDictionary<long, string>> ReadSearchAsync(CancellationToken cancellationToken)
    {
        var search = await httpClient.GetFromJsonAsync<JournalSearchIndex>(content.AssetUrl($"{Root}search.json"), SerializerOptions, cancellationToken);
        return search?.Issues.ToDictionary(entry => entry.Id, entry => Normalize($"{entry.Title} {entry.Text}")) ?? new Dictionary<long, string>();
    }

    public static string Normalize(string text) => text.ToLowerInvariant().Replace('ё', 'е');

    /// <summary>
    /// Renders an issue: relative links point into the issue folder, images get <c>srcset</c> with the
    /// prepared <c>-480</c> variant, lazy loading and their average color as a placeholder, external links open in a new tab.
    /// </summary>
    private static string Render(string markdown, Func<string, string> assetUrl)
    {
        var document = Markdown.Parse(markdown, Pipeline);
        foreach (var link in document.Descendants<LinkInline>())
        {
            var url = link.Url ?? string.Empty;
            var attributes = link.GetAttributes();
            if (Uri.TryCreate(url, UriKind.Absolute, out _))
            {
                if (!link.IsImage)
                {
                    attributes.AddPropertyIfNotExist("target", "_blank");
                    attributes.AddPropertyIfNotExist("rel", "noopener noreferrer");
                }
                continue;
            }

            if (url.Length == 0 || url.Contains("..", StringComparison.Ordinal) || url.StartsWith('/')) continue;
            link.Url = assetUrl(url);
            if (!link.IsImage) continue;

            attributes.AddPropertyIfNotExist("loading", "lazy");
            attributes.AddPropertyIfNotExist("decoding", "async");
            var width = attributes.Properties?.FirstOrDefault(property => property.Key == "width").Value;
            if (url.EndsWith(".webp", StringComparison.OrdinalIgnoreCase) && !url.EndsWith("-480.webp", StringComparison.OrdinalIgnoreCase) &&
                int.TryParse(width, NumberStyles.None, CultureInfo.InvariantCulture, out var pixels) && pixels > 480)
            {
                attributes.AddPropertyIfNotExist("srcset", $"{assetUrl(url[..^5] + "-480.webp")} 480w, {link.Url} {pixels}w");
                attributes.AddPropertyIfNotExist("sizes", "(max-width: 600px) 100vw, (max-width: 1100px) 70vw, 760px");
            }
            var color = attributes.Properties?.FirstOrDefault(property => property.Key == "data-color").Value;
            if (color is { Length: 7 } && color[0] == '#' && color.Skip(1).All(Uri.IsHexDigit))
                attributes.AddPropertyIfNotExist("style", $"background-color:{color}");
        }

        return document.ToHtml(Pipeline);
    }
}
