using System.Globalization;
using System.Text;

namespace Build.Targets;

/// <summary>
/// Turns a Telegram Desktop export of @gattavasis into journal issues under <c>content/telegram/journal</c>.
/// Consecutive messages published within <see cref="IssueGap"/> form one issue (albums, essays split into several messages,
/// audio examples). Existing issues keep manual edits; only Telegram-owned fields (tags, reactions) are refreshed.
/// </summary>
internal sealed class TelegramJournalTarget(BuildPaths paths, JournalStore store)
{
    private static readonly TimeSpan IssueGap = TimeSpan.FromMinutes(2);

    public Task<int> RunAsync(IReadOnlyList<string> args, CancellationToken cancellationToken)
    {
        try
        {
            var reindexOnly = args.Contains("--reindex", StringComparer.OrdinalIgnoreCase);
            var rebuild = ParseRebuild(args);
            var settings = store.ReadSettings();
            if (!reindexOnly) Import(ResolveExport(args), settings, rebuild, cancellationToken);

            var changed = store.WriteIndexes(settings, store.ReadPosts());
            Console.WriteLine(changed ? "Индексы журнала обновлены." : "Индексы журнала не изменились.");
            Console.WriteLine($"Журнал: {store.Root}");
            return Task.FromResult(0);
        }
        catch (OperationCanceledException)
        {
            Console.Error.WriteLine("Telegram journal export was cancelled.");
            return Task.FromResult(1);
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException or System.Text.Json.JsonException or InvalidOperationException or FormatException)
        {
            Console.Error.WriteLine($"Telegram journal export failed: {exception.Message}");
            return Task.FromResult(1);
        }
    }

    private void Import(string exportPath, JournalSettings settings, RebuildRequest rebuild, CancellationToken cancellationToken)
    {
        var exportDirectory = Path.GetDirectoryName(Path.GetFullPath(exportPath))!;
        var messages = JournalExport.Read(exportPath);
        var byId = messages.ToDictionary(message => message.Id);
        var posts = store.ReadPosts();
        var known = posts.SelectMany(post => post.Meta.Messages.Append(post.Meta.Id)).ToHashSet();
        Console.WriteLine($"Экспорт: {exportPath}");
        Console.WriteLine($"Сообщений в экспорте: {messages.Count}; выпусков в журнале: {posts.Count}.");

        var refreshed = 0;
        var rebuilt = 0;
        foreach (var post in posts)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var group = post.Meta.Messages.Select(id => byId.GetValueOrDefault(id)).OfType<ExportMessage>().ToArray();
            if (rebuild.Includes(post.Meta.Id))
            {
                if (group.Length != post.Meta.Messages.Count)
                    throw new InvalidOperationException($"{post.RelativePath}: не все сообщения выпуска есть в экспорте, пересборка невозможна.");
                foreach (var file in Directory.EnumerateFiles(post.Directory)) File.Delete(file);
                WriteIssue(group, exportDirectory, settings, post.Directory, post.Meta);
                rebuilt++;
                continue;
            }

            if (group.Length == 0) continue;
            var tags = group.SelectMany(message => message.Tags).Distinct(StringComparer.Ordinal).ToArray();
            var reactions = group.Sum(message => message.Reactions);
            if (tags.SequenceEqual(post.Meta.Tags) && reactions == post.Meta.Reactions) continue;
            var postFile = Path.Combine(post.Directory, JournalStore.PostFile);
            File.WriteAllText(postFile, JournalStore.UpdateTelegramFields(postFile, tags, reactions), new UTF8Encoding(false));
            refreshed++;
        }

        var added = 0;
        var skippedForwards = 0;
        var current = new List<ExportMessage>();
        void Flush()
        {
            if (current.Count == 0) return;
            if (current.Any(message => message.HasText || message.Media is not (JournalMediaKind.None or JournalMediaKind.Sticker)))
            {
                WriteIssue(current, exportDirectory, settings, null, null);
                added++;
            }
            current = [];
        }

        foreach (var message in messages)
        {
            cancellationToken.ThrowIfCancellationRequested();
            if (known.Contains(message.Id)) { Flush(); continue; }
            if (message.Forwarded) { skippedForwards++; Flush(); continue; }
            if (message.Media == JournalMediaKind.Sticker && !message.HasText) continue;
            if (current.Count > 0 && message.Date - current[^1].Date > IssueGap) Flush();
            current.Add(message);
        }
        Flush();

        Console.WriteLine($"Новых выпусков: {added}; обновлены теги или реакции: {refreshed}; пересобрано: {rebuilt}; пропущено пересылок: {skippedForwards}.");
    }

    private void WriteIssue(IReadOnlyList<ExportMessage> group, string exportDirectory, JournalSettings settings, string? directory, JournalFrontMatter? previous)
    {
        var first = group[0];
        var textMessage = group.FirstOrDefault(message => message.HasText);
        string? title = null;
        IReadOnlyList<ExportSegment>? titleBody = null;
        if (textMessage is not null) (title, titleBody) = JournalMarkdown.SplitTitle(textMessage.Segments);

        var created = directory is null;
        if (directory is null)
        {
            var slug = JournalMarkdown.Slug(title ?? FirstWords(textMessage?.PlainText));
            var name = first.Id.ToString(CultureInfo.InvariantCulture) + (slug.Length > 0 ? "-" + slug : string.Empty);
            directory = Path.Combine(store.PostsRoot, first.Date.Year.ToString(CultureInfo.InvariantCulture), name);
        }
        Directory.CreateDirectory(directory);

        try
        {
            var renderer = new IssueRenderer(directory, exportDirectory, settings);
            var blocks = new List<string>();
            var media = new List<(JournalMediaKind Kind, string Markdown)>();
            var captions = new List<string>();

            void Flush()
            {
                for (var index = 0; index < media.Count;)
                {
                    var end = index;
                    while (end < media.Count && media[end].Kind == JournalMediaKind.Photo) end++;
                    if (end - index >= 2)
                    {
                        blocks.Add(":::gallery\n" + string.Join("\n\n", media.Skip(index).Take(end - index).Select(item => item.Markdown)) + "\n:::");
                        index = end;
                    }
                    else blocks.Add(media[index++].Markdown);
                }
                blocks.AddRange(captions);
                media.Clear();
                captions.Clear();
            }

            foreach (var message in group)
            {
                var segments = ReferenceEquals(message, textMessage) && title is not null ? titleBody! : message.Segments;
                var text = message.HasText ? JournalMarkdown.StripTagLines(JournalMarkdown.ToMarkdown(segments)) : string.Empty;
                var rendered = renderer.Render(message);
                if (rendered is null)
                {
                    Flush();
                    if (text.Length > 0) blocks.Add(text);
                    continue;
                }
                if (message.Media != JournalMediaKind.Photo)
                {
                    Flush();
                    blocks.Add(rendered);
                    if (text.Length > 0) blocks.Add(text);
                    continue;
                }
                media.Add((message.Media, rendered));
                if (text.Length > 0) captions.Add(text);
            }
            Flush();

            var meta = new JournalFrontMatter
            {
                Id = first.Id,
                Messages = group.Select(message => message.Id).ToList(),
                Date = first.Date.ToString("yyyy-MM-dd'T'HH:mm:sszzz", CultureInfo.InvariantCulture),
                Title = title,
                Source = $"https://t.me/{JournalExport.ChannelUsername}/{first.Id}",
                Tags = group.SelectMany(message => message.Tags).Distinct(StringComparer.Ordinal).ToList(),
                Reactions = group.Sum(message => message.Reactions),
                Rubric = previous?.Rubric,
                Series = previous?.Series,
                Hidden = previous?.Hidden ?? false,
                Cover = renderer.Cover
            };
            var body = JournalMarkdown.Normalize(string.Join("\n\n", blocks));
            File.WriteAllText(Path.Combine(directory, JournalStore.PostFile), JournalStore.WritePost(meta, body), new UTF8Encoding(false));
        }
        catch when (created)
        {
            Directory.Delete(directory, true);
            throw;
        }
    }

    private string ResolveExport(IReadOnlyList<string> args)
    {
        var explicitPath = args.FirstOrDefault(arg => !arg.StartsWith("--", StringComparison.Ordinal) && !IsRebuildValue(args, arg));
        if (explicitPath is not null) return ExistingFile(explicitPath.Trim('"'));
        if (File.Exists(paths.TelegramDefaultExport)) return paths.TelegramDefaultExport;
        if (Console.IsInputRedirected)
            throw new InvalidOperationException($"Не найден {paths.TelegramDefaultExport}. Передайте путь к result.json аргументом.");

        Console.WriteLine("Экспортируйте канал @gattavasis в Telegram Desktop: меню канала → Export chat history → JSON, с фото и файлами.");
        Console.Write("Telegram Desktop result.json: ");
        return ExistingFile(Console.ReadLine()?.Trim().Trim('"') ?? string.Empty);
    }

    private static string ExistingFile(string path) =>
        File.Exists(path) ? path : throw new InvalidOperationException($"Файл экспорта не найден: {path}");

    private static bool IsRebuildValue(IReadOnlyList<string> args, string value)
    {
        var index = args.ToList().IndexOf(value);
        return index > 0 && args[index - 1].Equals("--rebuild", StringComparison.OrdinalIgnoreCase);
    }

    private static RebuildRequest ParseRebuild(IReadOnlyList<string> args)
    {
        var list = args.ToList();
        var index = list.FindIndex(arg => arg.Equals("--rebuild", StringComparison.OrdinalIgnoreCase));
        if (index < 0) return new RebuildRequest(false, []);
        if (index + 1 >= list.Count) throw new InvalidOperationException("После --rebuild укажите all или ID выпусков через запятую.");
        var value = list[index + 1];
        if (value.Equals("all", StringComparison.OrdinalIgnoreCase)) return new RebuildRequest(true, []);
        var ids = value.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(id => long.TryParse(id, NumberStyles.None, CultureInfo.InvariantCulture, out var parsed) ? parsed : throw new InvalidOperationException($"Неверный ID выпуска: {id}"))
            .ToHashSet();
        return new RebuildRequest(false, ids);
    }

    private static string? FirstWords(string? text) =>
        text is null ? null : string.Join(' ', text.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries).Take(6));

    private sealed record RebuildRequest(bool All, HashSet<long> Ids)
    {
        public bool Includes(long id) => All || Ids.Contains(id);
    }

    /// <summary>Renders the media of one message to Markdown and copies or converts its files into the issue folder.</summary>
    private sealed class IssueRenderer(string directory, string exportDirectory, JournalSettings settings)
    {
        private int _counter;
        public JournalCover? Cover { get; private set; }
        private bool _coverIsPhoto;

        public string? Render(ExportMessage message)
        {
            var source = $"https://t.me/{JournalExport.ChannelUsername}/{message.Id}";
            switch (message.Media)
            {
                case JournalMediaKind.Photo:
                {
                    var image = Image(message.Photo, true);
                    if (image is null) return null;
                    var classes = message.MediaSpoiler ? ".spoiler " : string.Empty;
                    return $"![]({image.File}){{{classes}{ImageAttributes(image)}}}";
                }
                case JournalMediaKind.Video:
                {
                    var classes = message.MediaType == "video_message" ? ".video .round" : ".video";
                    var duration = message.Duration is { } seconds ? $" data-duration=\"{seconds}\"" : string.Empty;
                    var poster = Image(message.Thumbnail, false);
                    var content = poster is null ? "Видео" : $"![]({poster.File}){{{ImageAttributes(poster)}}}";
                    return $"[{content}]({source}){{{classes}{duration}}}";
                }
                case JournalMediaKind.Audio:
                {
                    var href = source;
                    var extra = string.Empty;
                    var file = ExportFile(message.File);
                    if (settings.PublishedAudio.Contains(message.Id) && file is not null)
                    {
                        href = Next() + Path.GetExtension(file).ToLowerInvariant();
                        File.Copy(file, Path.Combine(directory, href), true);
                        extra = $" data-source=\"{source}\"";
                    }
                    var duration = message.Duration is { } seconds ? $" data-duration=\"{seconds}\"" : string.Empty;
                    return $"[{JournalMarkdown.Escape(AudioLabel(message))}]({href}){{.audio{duration}{extra}}}";
                }
                case JournalMediaKind.Document:
                    return $"[{JournalMarkdown.Escape(message.FileName ?? "Файл")}]({source}){{.document}}";
                case JournalMediaKind.Poll when message.Poll is { } poll:
                    return "**" + JournalMarkdown.Escape(poll.Question.Replace('\n', ' ')) + "**{.poll}\n\n" +
                           string.Join('\n', poll.Answers.Select(answer => "- " + JournalMarkdown.Escape(answer)));
                default:
                    return null;
            }
        }

        private JournalImage? Image(string? relative, bool photo)
        {
            var file = ExportFile(relative);
            if (file is null)
            {
                if (relative is not null && photo) Console.WriteLine($"  Нет файла в экспорте: {relative}");
                return null;
            }

            var image = JournalImages.Convert(file, directory, Next());
            if (Cover is null || photo && !_coverIsPhoto)
            {
                Cover = new JournalCover { File = image.File, Width = image.Width, Height = image.Height, Color = image.Color, Lqip = image.Lqip };
                _coverIsPhoto = photo;
            }
            return image;
        }

        private string? ExportFile(string? relative)
        {
            if (string.IsNullOrWhiteSpace(relative) || relative.StartsWith('(')) return null;
            var full = Path.GetFullPath(Path.Combine(exportDirectory, relative));
            return File.Exists(full) ? full : null;
        }

        private string Next() => (++_counter).ToString("00", CultureInfo.InvariantCulture);

        private static string ImageAttributes(JournalImage image) =>
            $"width=\"{image.Width}\" height=\"{image.Height}\" data-color=\"{image.Color}\"";

        private static string AudioLabel(ExportMessage message)
        {
            if (message.MediaType == "voice_message") return "Голосовое сообщение";
            var title = message.Title?.Trim();
            var performer = message.Performer?.Trim();
            if (!string.IsNullOrEmpty(title) && !string.IsNullOrEmpty(performer)) return $"{title} — {performer}";
            if (!string.IsNullOrEmpty(title)) return title;
            if (!string.IsNullOrEmpty(performer)) return performer;
            var name = Path.GetFileNameWithoutExtension(message.FileName ?? string.Empty).Replace('_', ' ').Trim();
            return name.Length > 0 ? name : "Аудио";
        }
    }
}
