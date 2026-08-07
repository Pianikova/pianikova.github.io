using System.Text.Json;
using System.Text.Json.Nodes;

namespace Build.Targets;

internal sealed class ContentValidationTarget(BuildPaths paths)
{
    private readonly List<string> _errors = [];

    public int Run()
    {
        _errors.Clear();
        var contentRoot = Path.Combine(paths.SolutionDirectory, "content");
        var settings = ReadObject(Path.Combine(contentRoot, "settings", "site.json"));
        if (settings is null) return Report();

        if (settings["schemaVersion"]?.GetValue<int>() != 2) Error("content/settings/site.json", "schemaVersion must be 2.");
        var defaultLanguage = settings["defaultLanguage"]?.GetValue<string>();
        var languages = settings["availableLanguages"]?.AsArray()
            .Select(x => x?.GetValue<string>())
            .Where(x => !string.IsNullOrWhiteSpace(x))
            .Cast<string>()
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToArray() ?? [];

        if (languages.Length == 0) Error("content/settings/site.json", "availableLanguages must contain at least one language.");
        if (defaultLanguage is null || !languages.Contains(defaultLanguage, StringComparer.OrdinalIgnoreCase))
            Error("content/settings/site.json", "defaultLanguage must be present in availableLanguages.");

        var schedules = new Dictionary<string, JsonObject>(StringComparer.OrdinalIgnoreCase);
        var videos = new Dictionary<string, JsonObject>(StringComparer.OrdinalIgnoreCase);
        var photos = new Dictionary<string, JsonObject>(StringComparer.OrdinalIgnoreCase);
        foreach (var language in languages)
        {
            var sitePath = Path.Combine(contentRoot, "site", language, "site.json");
            var schedulePath = Path.Combine(contentRoot, "schedule", language, "schedule.json");
            var videosPath = Path.Combine(contentRoot, "videos", language, "videos.json");
            var photosPath = Path.Combine(contentRoot, "photos", language, "photos.json");
            var site = ReadObject(sitePath);
            var schedule = ReadObject(schedulePath);
            var videosDoc = ReadObject(videosPath);
            var photosDoc = ReadObject(photosPath);
            if (site is not null) ValidateSite(contentRoot, language, site);
            if (schedule is not null)
            {
                ValidateSchedule(language, schedule);
                schedules[language] = schedule;
            }
            if (videosDoc is not null)
            {
                ValidateVideos(language, videosDoc);
                videos[language] = videosDoc;
            }
            if (photosDoc is not null)
            {
                ValidatePhotos(contentRoot, language, photosDoc);
                photos[language] = photosDoc;
            }
        }

        ValidateSchedulesMatch(schedules);
        ValidateVideosMatch(videos);
        ValidatePhotosMatch(photos);
        ValidateArticleTranslations(contentRoot, languages);
        return Report();
    }

    private void ValidateSite(string contentRoot, string language, JsonObject site)
    {
        Required(site, "id", $"content/site/{language}/site.json");
        Required(site["identity"] as JsonObject, "name", $"content/site/{language}/site.json");
        ValidateMedia(contentRoot, site["hero"]?["image"] as JsonObject, $"content/site/{language}/site.json hero.image");
    }

    private void ValidateMedia(string contentRoot, JsonObject? media, string location)
    {
        if (media is null) { Error(location, "Media object is required."); return; }
        var relativePath = media["path"]?.GetValue<string>();
        if (string.IsNullOrWhiteSpace(relativePath)) { Error(location, "Media path is required."); return; }
        if (Uri.TryCreate(relativePath, UriKind.Absolute, out _)) return;

        var fullPath = Path.GetFullPath(Path.Combine(contentRoot, relativePath.Replace('/', Path.DirectorySeparatorChar)));
        if (!fullPath.StartsWith(Path.GetFullPath(contentRoot) + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
            Error(location, "Media path leaves the content directory.");
        else if (!File.Exists(fullPath))
            Error(location, $"Media file does not exist: {relativePath}");
    }

    private void ValidateSchedule(string language, JsonObject schedule)
    {
        if (schedule["concerts"] is not JsonArray concerts) { Error($"content/schedule/{language}/schedule.json", "concerts must be an array."); return; }
        var ids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var node in concerts)
        {
            if (node is not JsonObject concert) continue;
            var id = concert["id"]?.GetValue<string>();
            if (string.IsNullOrWhiteSpace(id)) Error($"content/schedule/{language}/schedule.json", "Every concert requires an id.");
            else if (!ids.Add(id)) Error($"content/schedule/{language}/schedule.json", $"Duplicate concert id: {id}");
            Required(concert, "startsAt", $"concert {id} ({language})");
            Required(concert, "timezone", $"concert {id} ({language})");
            Required(concert, "venue", $"concert {id} ({language})");
        }
    }

    private void ValidateSchedulesMatch(IReadOnlyDictionary<string, JsonObject> schedules)
    {
        var baseline = schedules.FirstOrDefault();
        if (baseline.Value is null) return;
        var baselineItems = ConcertsById(baseline.Value);
        foreach (var (language, schedule) in schedules.Skip(1))
        {
            var items = ConcertsById(schedule);
            foreach (var id in baselineItems.Keys.Union(items.Keys, StringComparer.OrdinalIgnoreCase))
            {
                if (!baselineItems.TryGetValue(id, out var left) || !items.TryGetValue(id, out var right))
                {
                    Error("content/schedule", $"Concert {id} must exist in both {baseline.Key} and {language}.");
                    continue;
                }

                foreach (var field in new[] { "startsAt", "timezone", "format", "ticketUrl", "status", "sourceUrl", "verifiedAt" })
                {
                    if (left[field]?.ToJsonString() != right[field]?.ToJsonString())
                        Error("content/schedule", $"Concert {id}: field {field} differs between {baseline.Key} and {language}.");
                }
            }
        }
    }

    private static Dictionary<string, JsonObject> ConcertsById(JsonObject schedule) =>
        schedule["concerts"]?.AsArray()
            .OfType<JsonObject>()
            .Where(x => !string.IsNullOrWhiteSpace(x["id"]?.GetValue<string>()))
            .ToDictionary(x => x["id"]!.GetValue<string>(), StringComparer.OrdinalIgnoreCase) ?? [];

    private void ValidateVideos(string language, JsonObject videos)
    {
        if (videos["items"] is not JsonArray items) { Error($"content/videos/{language}/videos.json", "items must be an array."); return; }
        var ids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var featuredCount = 0;
        foreach (var node in items)
        {
            if (node is not JsonObject video) continue;
            var id = video["id"]?.GetValue<string>();
            if (string.IsNullOrWhiteSpace(id)) Error($"content/videos/{language}/videos.json", "Every video requires an id.");
            else if (!ids.Add(id)) Error($"content/videos/{language}/videos.json", $"Duplicate video id: {id}");
            Required(video, "title", $"video {id} ({language})");
            Required(video, "youtubeUrl", $"video {id} ({language})");
            if (video["featured"]?.GetValue<bool>() == true) featuredCount++;
        }

        if (featuredCount > 1) Error($"content/videos/{language}/videos.json", "Only one video can be marked as featured.");
    }

    private void ValidateVideosMatch(IReadOnlyDictionary<string, JsonObject> videos)
    {
        var baseline = videos.FirstOrDefault();
        if (baseline.Value is null) return;
        var baselineItems = VideosById(baseline.Value);
        foreach (var (language, doc) in videos.Skip(1))
        {
            var items = VideosById(doc);
            foreach (var id in baselineItems.Keys.Union(items.Keys, StringComparer.OrdinalIgnoreCase))
            {
                if (!baselineItems.TryGetValue(id, out var left) || !items.TryGetValue(id, out var right))
                {
                    Error("content/videos", $"Video {id} must exist in both {baseline.Key} and {language}.");
                    continue;
                }

                foreach (var field in new[] { "youtubeUrl", "featured", "recordedAt" })
                {
                    if (left[field]?.ToJsonString() != right[field]?.ToJsonString())
                        Error("content/videos", $"Video {id}: field {field} differs between {baseline.Key} and {language}.");
                }
            }
        }
    }

    private static Dictionary<string, JsonObject> VideosById(JsonObject videos) =>
        videos["items"]?.AsArray()
            .OfType<JsonObject>()
            .Where(x => !string.IsNullOrWhiteSpace(x["id"]?.GetValue<string>()))
            .ToDictionary(x => x["id"]!.GetValue<string>(), StringComparer.OrdinalIgnoreCase) ?? [];

    private void ValidatePhotos(string contentRoot, string language, JsonObject photos)
    {
        if (photos["items"] is not JsonArray items) { Error($"content/photos/{language}/photos.json", "items must be an array."); return; }
        var ids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var node in items)
        {
            if (node is not JsonObject photo) continue;
            var id = photo["id"]?.GetValue<string>();
            if (string.IsNullOrWhiteSpace(id)) Error($"content/photos/{language}/photos.json", "Every photo requires an id.");
            else if (!ids.Add(id)) Error($"content/photos/{language}/photos.json", $"Duplicate photo id: {id}");
            Required(photo, "path", $"photo {id} ({language})");
            Required(photo, "caption", $"photo {id} ({language})");
            ValidateMedia(contentRoot, photo, $"content/photos/{language}/photos.json photo {id}");
        }
    }

    private void ValidatePhotosMatch(IReadOnlyDictionary<string, JsonObject> photos)
    {
        var baseline = photos.FirstOrDefault();
        if (baseline.Value is null) return;
        var baselineItems = PhotosById(baseline.Value);
        foreach (var (language, doc) in photos.Skip(1))
        {
            var items = PhotosById(doc);
            foreach (var id in baselineItems.Keys.Union(items.Keys, StringComparer.OrdinalIgnoreCase))
            {
                if (!baselineItems.TryGetValue(id, out var left) || !items.TryGetValue(id, out var right))
                {
                    Error("content/photos", $"Photo {id} must exist in both {baseline.Key} and {language}.");
                    continue;
                }

                foreach (var field in new[] { "path", "takenAt" })
                {
                    if (left[field]?.ToJsonString() != right[field]?.ToJsonString())
                        Error("content/photos", $"Photo {id}: field {field} differs between {baseline.Key} and {language}.");
                }
            }
        }
    }

    private static Dictionary<string, JsonObject> PhotosById(JsonObject photos) =>
        photos["items"]?.AsArray()
            .OfType<JsonObject>()
            .Where(x => !string.IsNullOrWhiteSpace(x["id"]?.GetValue<string>()))
            .ToDictionary(x => x["id"]!.GetValue<string>(), StringComparer.OrdinalIgnoreCase) ?? [];

    private void ValidateArticleTranslations(string contentRoot, IReadOnlyList<string> languages)
    {
        if (languages.Count < 2) return;
        var published = new Dictionary<string, HashSet<string>>(StringComparer.OrdinalIgnoreCase);
        foreach (var language in languages)
        {
            var directory = Path.Combine(contentRoot, "articles", language);
            published[language] = Directory.Exists(directory)
                ? Directory.EnumerateFiles(directory, "*.md").Select(path => Path.GetFileName(path)!).ToHashSet(StringComparer.OrdinalIgnoreCase)
                : [];
        }

        foreach (var file in published.Values.SelectMany(x => x).Distinct(StringComparer.OrdinalIgnoreCase))
        {
            foreach (var language in languages.Where(language => !published[language].Contains(file)))
                Error("content/articles", $"Article {file} has no {language} translation.");
        }
    }

    private JsonObject? ReadObject(string path)
    {
        if (!File.Exists(path)) { RequireFile(path); return null; }
        try { return JsonNode.Parse(File.ReadAllText(path)) as JsonObject ?? throw new JsonException("Root must be an object."); }
        catch (Exception exception) when (exception is JsonException or FormatException)
        {
            Error(Path.GetRelativePath(paths.SolutionDirectory, path), exception.Message);
            return null;
        }
    }

    private void RequireFile(string path)
    {
        if (!File.Exists(path)) Error(Path.GetRelativePath(paths.SolutionDirectory, path), "Required file is missing.");
    }

    private void Required(JsonObject? value, string property, string location)
    {
        if (value is null || string.IsNullOrWhiteSpace(value[property]?.GetValue<string>())) Error(location, $"{property} is required.");
    }

    private void Error(string location, string message) => _errors.Add($"{location}: {message}");

    private int Report()
    {
        if (_errors.Count == 0) { Console.WriteLine("Content validation succeeded."); return 0; }
        Console.Error.WriteLine("Content validation failed:");
        foreach (var error in _errors) Console.Error.WriteLine($"  - {error}");
        return 1;
    }
}
