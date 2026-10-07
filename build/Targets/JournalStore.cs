using System.Globalization;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using Markdig;
using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace Build.Targets;

internal sealed class JournalCover
{
    public string File { get; set; } = string.Empty;
    public int Width { get; set; }
    public int Height { get; set; }
    public string Color { get; set; } = string.Empty;
    public string Lqip { get; set; } = string.Empty;
}

internal sealed class JournalFrontMatter
{
    public long Id { get; set; }
    public List<long> Messages { get; set; } = [];
    public string Date { get; set; } = string.Empty;
    public string? Title { get; set; }
    public string? Source { get; set; }
    public List<string> Tags { get; set; } = [];
    public int Reactions { get; set; }
    public string? Rubric { get; set; }
    public string? Series { get; set; }
    public bool Hidden { get; set; }
    public JournalCover? Cover { get; set; }
}

internal sealed record JournalPost(string Directory, string RelativePath, JournalFrontMatter Meta, string FrontMatter, string Body);

internal sealed class JournalSettings
{
    public int SchemaVersion { get; set; }
    public List<JournalTaxon> Rubrics { get; set; } = [];
    public List<JournalTaxon> Series { get; set; } = [];
    public List<long> PublishedAudio { get; set; } = [];
}

internal sealed class JournalTaxon
{
    public string Id { get; set; } = string.Empty;
    public Dictionary<string, string> Title { get; set; } = [];
    public List<string> Tags { get; set; } = [];
}

/// <summary>Reads issue folders of <c>content/telegram/journal</c> and builds <c>index.json</c> and <c>search.json</c> from them.</summary>
internal sealed partial class JournalStore(BuildPaths paths)
{
    public const string IndexFile = "index.json";
    public const string SearchFile = "search.json";
    public const string SettingsFile = "settings.json";
    public const string PostFile = "index.md";
    private const int WordsPerMinute = 180;
    private const int ExcerptLength = 220;

    public static readonly MarkdownPipeline Pipeline = new MarkdownPipelineBuilder()
        .UseAdvancedExtensions()
        .UseSoftlineBreakAsHardlineBreak()
        .UseYamlFrontMatter()
        .DisableHtml()
        .Build();

    public static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        NewLine = "\n",
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    private static readonly IDeserializer Yaml = new DeserializerBuilder()
        .WithNamingConvention(CamelCaseNamingConvention.Instance)
        .IgnoreUnmatchedProperties()
        .Build();

    public string Root => paths.TelegramJournal;
    public string PostsRoot => Path.Combine(Root, "posts");

    public JournalSettings ReadSettings()
    {
        var path = Path.Combine(Root, SettingsFile);
        if (!File.Exists(path))
        {
            Directory.CreateDirectory(Root);
            var defaults = new JournalSettings
            {
                SchemaVersion = 1,
                Rubrics =
                [
                    new JournalTaxon { Id = "stage", Title = new() { ["ru"] = "Сцена", ["en"] = "Stage" }, Tags = ["концерт", "фестиваль", "репетиция"] }
                ],
                PublishedAudio = [209]
            };
            File.WriteAllText(path, JsonSerializer.Serialize(defaults, JsonOptions) + "\n", new UTF8Encoding(false));
        }

        var settings = JsonSerializer.Deserialize<JournalSettings>(File.ReadAllText(path), JsonOptions)
                       ?? throw new InvalidOperationException($"{SettingsFile} must be a JSON object.");
        if (settings.SchemaVersion != 1) throw new InvalidOperationException($"{SettingsFile}: schemaVersion must be 1.");
        foreach (var taxon in settings.Rubrics.Concat(settings.Series))
            taxon.Tags = taxon.Tags.Select(tag => tag.TrimStart('#').Trim().ToLowerInvariant()).ToList();
        return settings;
    }

    public IReadOnlyList<JournalPost> ReadPosts()
    {
        if (!Directory.Exists(PostsRoot)) return [];
        return Directory.EnumerateFiles(PostsRoot, PostFile, SearchOption.AllDirectories)
            .Select(ReadPost)
            .OrderBy(post => post.Meta.Id)
            .ToArray();
    }

    public JournalPost ReadPost(string file)
    {
        var text = File.ReadAllText(file).Replace("\r\n", "\n");
        var match = FrontMatterBlock().Match(text);
        var relative = Path.GetRelativePath(Root, Path.GetDirectoryName(file)!).Replace('\\', '/');
        if (!match.Success) throw new InvalidOperationException($"{relative}/{PostFile}: front matter is missing.");
        JournalFrontMatter meta;
        try { meta = Yaml.Deserialize<JournalFrontMatter>(match.Groups["yaml"].Value) ?? new JournalFrontMatter(); }
        catch (YamlDotNet.Core.YamlException exception)
        {
            throw new InvalidOperationException($"{relative}/{PostFile}: front matter is invalid: {exception.Message}");
        }
        return new JournalPost(Path.GetDirectoryName(file)!, relative, meta, match.Groups["yaml"].Value, text[match.Length..].Trim('\n'));
    }

    public static string WritePost(JournalFrontMatter meta, string body)
    {
        var builder = new StringBuilder("---\n");
        builder.Append("id: ").Append(meta.Id.ToString(CultureInfo.InvariantCulture)).Append('\n');
        builder.Append("messages: [").Append(string.Join(", ", meta.Messages.Select(id => id.ToString(CultureInfo.InvariantCulture)))).Append("]\n");
        builder.Append("date: ").Append(JournalMarkdown.Quote(meta.Date)).Append('\n');
        builder.Append("title: ").Append(meta.Title is null ? "null" : JournalMarkdown.Quote(meta.Title)).Append('\n');
        builder.Append("source: ").Append(JournalMarkdown.Quote(meta.Source ?? string.Empty)).Append('\n');
        builder.Append(TagsLine(meta.Tags)).Append('\n');
        builder.Append(ReactionsLine(meta.Reactions)).Append('\n');
        builder.Append("rubric: ").Append(JournalMarkdown.Quote(meta.Rubric ?? string.Empty)).Append('\n');
        builder.Append("series: ").Append(JournalMarkdown.Quote(meta.Series ?? string.Empty)).Append('\n');
        builder.Append("hidden: ").Append(meta.Hidden ? "true" : "false").Append('\n');
        if (meta.Cover is { } cover)
            builder.Append("cover: { file: ").Append(JournalMarkdown.Quote(cover.File))
                .Append(", width: ").Append(cover.Width.ToString(CultureInfo.InvariantCulture))
                .Append(", height: ").Append(cover.Height.ToString(CultureInfo.InvariantCulture))
                .Append(", color: ").Append(JournalMarkdown.Quote(cover.Color))
                .Append(", lqip: ").Append(JournalMarkdown.Quote(cover.Lqip)).Append(" }\n");
        else builder.Append("cover: null\n");
        builder.Append("---\n\n").Append(body.Trim('\n')).Append('\n');
        return builder.ToString();
    }

    /// <summary>Rewrites only the lines owned by Telegram (tags, reactions), keeping manual edits elsewhere.</summary>
    public static string UpdateTelegramFields(string file, IReadOnlyList<string> tags, int reactions)
    {
        var text = File.ReadAllText(file).Replace("\r\n", "\n");
        var match = FrontMatterBlock().Match(text);
        if (!match.Success) return text;
        var yaml = match.Groups["yaml"].Value;
        yaml = ReplaceLine(yaml, "tags", TagsLine(tags));
        yaml = ReplaceLine(yaml, "reactions", ReactionsLine(reactions));
        return "---\n" + yaml + "\n---\n" + text[match.Length..];
    }

    public static string TagsLine(IEnumerable<string> tags) => "tags: [" + string.Join(", ", tags.Select(JournalMarkdown.Quote)) + "]";
    public static string ReactionsLine(int reactions) => "reactions: " + reactions.ToString(CultureInfo.InvariantCulture);

    public (string Index, string Search) BuildIndexes(JournalSettings settings, IReadOnlyList<JournalPost> posts)
    {
        var visible = posts.Where(post => !post.Meta.Hidden)
            .OrderByDescending(post => DateTimeOffset.Parse(post.Meta.Date, CultureInfo.InvariantCulture))
            .ThenByDescending(post => post.Meta.Id)
            .ToArray();

        var issues = new JsonArray();
        var search = new JsonArray();
        var rubricCounts = new Dictionary<string, int>(StringComparer.Ordinal);
        var seriesIssues = new Dictionary<string, List<(DateTimeOffset Date, long Id)>>(StringComparer.Ordinal);
        foreach (var post in visible)
        {
            var meta = post.Meta;
            var date = DateTimeOffset.Parse(meta.Date, CultureInfo.InvariantCulture);
            var plain = Whitespace().Replace(Markdown.ToPlainText(post.Body, Pipeline), " ").Trim();
            var words = plain.Split(' ', StringSplitOptions.RemoveEmptyEntries).Length;
            var rubric = Resolve(meta.Rubric, meta.Tags, settings.Rubrics);
            var series = Resolve(meta.Series, meta.Tags, settings.Series);
            if (rubric is not null) rubricCounts[rubric] = rubricCounts.GetValueOrDefault(rubric) + 1;
            if (series is not null)
            {
                if (!seriesIssues.TryGetValue(series, out var list)) seriesIssues[series] = list = [];
                list.Add((date, meta.Id));
            }

            var audio = AudioLink().Matches(post.Body).Count;
            var videos = VideoLink().Matches(post.Body).Count;
            issues.Add(new JsonObject
            {
                ["id"] = meta.Id,
                ["date"] = meta.Date,
                ["path"] = post.RelativePath,
                ["title"] = meta.Title,
                ["excerpt"] = Excerpt(plain),
                ["tags"] = new JsonArray(meta.Tags.Select(tag => (JsonNode)tag).ToArray()),
                ["rubric"] = rubric,
                ["series"] = series,
                ["readingMinutes"] = words == 0 ? 0 : Math.Max(1, (int)Math.Round(words / (double)WordsPerMinute)),
                ["reactions"] = meta.Reactions,
                ["photos"] = Math.Max(0, ImageTag().Matches(post.Body).Count - videos),
                ["audio"] = audio,
                ["videos"] = videos,
                ["cover"] = meta.Cover is null ? null : JsonSerializer.SerializeToNode(meta.Cover, JsonOptions),
                ["source"] = meta.Source
            });
            search.Add(new JsonObject { ["id"] = meta.Id, ["title"] = meta.Title, ["text"] = plain });
        }

        var index = new JsonObject
        {
            ["schemaVersion"] = 1,
            ["rubrics"] = new JsonArray(settings.Rubrics.Where(rubric => rubricCounts.ContainsKey(rubric.Id)).Select(rubric => (JsonNode)new JsonObject
            {
                ["id"] = rubric.Id,
                ["title"] = JsonSerializer.SerializeToNode(rubric.Title, JsonOptions),
                ["count"] = rubricCounts[rubric.Id]
            }).ToArray()),
            ["series"] = new JsonArray(settings.Series.Where(series => seriesIssues.ContainsKey(series.Id)).Select(series => (JsonNode)new JsonObject
            {
                ["id"] = series.Id,
                ["title"] = JsonSerializer.SerializeToNode(series.Title, JsonOptions),
                ["issues"] = new JsonArray(seriesIssues[series.Id].OrderBy(item => item.Date).ThenBy(item => item.Id).Select(item => (JsonNode)item.Id).ToArray())
            }).ToArray()),
            ["issues"] = issues
        };
        var searchDocument = new JsonObject { ["schemaVersion"] = 1, ["issues"] = search };
        return (index.ToJsonString(JsonOptions) + "\n", searchDocument.ToJsonString(new JsonSerializerOptions { Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping }) + "\n");
    }

    /// <summary>Writes the indexes; returns true when anything changed.</summary>
    public bool WriteIndexes(JournalSettings settings, IReadOnlyList<JournalPost> posts)
    {
        var (index, search) = BuildIndexes(settings, posts);
        return WriteIfChanged(Path.Combine(Root, IndexFile), index) | WriteIfChanged(Path.Combine(Root, SearchFile), search);
    }

    public static IEnumerable<string> LocalReferences(string body) =>
        LocalLink().Matches(body).Select(match => match.Groups["path"].Value).Distinct(StringComparer.Ordinal);

    private static string? Resolve(string? manual, IReadOnlyList<string> tags, IReadOnlyList<JournalTaxon> taxa)
    {
        if (!string.IsNullOrWhiteSpace(manual)) return manual.Trim();
        return taxa.FirstOrDefault(taxon => taxon.Tags.Any(tags.Contains))?.Id;
    }

    private static string Excerpt(string plain)
    {
        if (plain.Length <= ExcerptLength) return plain;
        var cut = plain.LastIndexOf(' ', ExcerptLength);
        return plain[..(cut > ExcerptLength / 2 ? cut : ExcerptLength)].TrimEnd(',', '.', ';', ':', ' ', '—', '-') + "…";
    }

    private static string ReplaceLine(string yaml, string key, string line)
    {
        var pattern = new Regex($"^{key}:.*$", RegexOptions.Multiline);
        return pattern.IsMatch(yaml) ? pattern.Replace(yaml, line.Replace("$", "$$"), 1) : yaml + "\n" + line;
    }

    private static bool WriteIfChanged(string path, string content)
    {
        if (File.Exists(path) && File.ReadAllText(path).Replace("\r\n", "\n") == content) return false;
        File.WriteAllText(path, content, new UTF8Encoding(false));
        return true;
    }

    [GeneratedRegex(@"\A---\n(?<yaml>.*?)\n---\n", RegexOptions.Singleline)]
    private static partial Regex FrontMatterBlock();

    [GeneratedRegex(@"\s+")]
    private static partial Regex Whitespace();

    [GeneratedRegex(@"!\[[^\]]*\]\(")]
    private static partial Regex ImageTag();

    [GeneratedRegex(@"\)\{\.audio[ }]")]
    private static partial Regex AudioLink();

    [GeneratedRegex(@"\)\{\.video[ }]")]
    private static partial Regex VideoLink();

    [GeneratedRegex(@"\]\((?<path>(?![a-z][a-z0-9+.-]*:)(?!<)[^)\s#?]+)\)", RegexOptions.IgnoreCase)]
    private static partial Regex LocalLink();
}
