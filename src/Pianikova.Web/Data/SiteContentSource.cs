namespace Pianikova.Web.Data;

internal sealed class SiteContentSource(HttpClient httpClient, NavigationManager navigationManager) : ISiteContentSource
{
    private static readonly string GitHubRoot = $"https://raw.githubusercontent.com/{GitHubRepository.FullName}/{GitHubRepository.Branch}/content/";
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web);

    private bool IsLocal => navigationManager.ToAbsoluteUri(navigationManager.Uri).IsLoopback;
    private string ContentRoot => IsLocal ? "content/" : GitHubRoot;

    public string SourceDescription => IsLocal ? "local content" : "GitHub main";

    public async Task<SiteSettings> LoadSettingsAsync(CancellationToken cancellationToken = default)
    {
        var settings = await ReadAsync<SiteSettings>("settings/site.json", cancellationToken);
        if (settings.SchemaVersion != 2)
        {
            throw new InvalidOperationException($"Unsupported content schema version {settings.SchemaVersion}.");
        }

        return settings;
    }

    public async Task<SiteContent> LoadAsync(string language, CancellationToken cancellationToken = default)
    {
        var settings = await LoadSettingsAsync(cancellationToken);
        var culture = NormalizeLanguage(language);
        if (!settings.AvailableLanguages.Contains(culture, StringComparer.OrdinalIgnoreCase))
        {
            culture = settings.DefaultLanguage;
        }

        var siteTask = ReadAsync<SiteDocument>($"site/{culture}/site.json", cancellationToken);
        var scheduleTask = ReadAsync<ScheduleDocument>($"schedule/{culture}/schedule.json", cancellationToken);
        var videosTask = ReadAsync<VideosDocument>($"videos/{culture}/videos.json", cancellationToken);
        var photosTask = ReadAsync<PhotosDocument>($"photos/{culture}/photos.json", cancellationToken);
        await Task.WhenAll(siteTask, scheduleTask, videosTask, photosTask);

        var site = await siteTask;
        var schedule = await scheduleTask;
        var videos = await videosTask;
        var photos = await photosTask;
        return Map(settings, site, schedule, videos, photos);
    }

    public string AssetUrl(string relativePath)
    {
        if (Uri.TryCreate(relativePath, UriKind.Absolute, out var absolute))
        {
            return absolute.ToString();
        }

        var safePath = string.Join('/', relativePath.Split('/', StringSplitOptions.RemoveEmptyEntries).Select(Uri.EscapeDataString));
        return $"{ContentRoot}{safePath}";
    }

    private async Task<T> ReadAsync<T>(string relativePath, CancellationToken cancellationToken)
    {
        var path = $"{ContentRoot}{relativePath}";
        var json = await httpClient.GetStringAsync(path, cancellationToken);
        if (string.IsNullOrWhiteSpace(json))
        {
            throw new InvalidOperationException($"Content file content/{relativePath} is empty.");
        }

        try
        {
            return JsonSerializer.Deserialize<T>(json, SerializerOptions)
                   ?? throw new InvalidOperationException($"Content file content/{relativePath} is empty.");
        }
        catch (JsonException exception)
        {
            throw new InvalidOperationException($"Content file content/{relativePath} is invalid: {exception.Message}", exception);
        }
    }

    private static SiteContent Map(SiteSettings settings, SiteDocument site, ScheduleDocument schedule, VideosDocument videos, PhotosDocument photos) => new(
        settings.SchemaVersion,
        new SiteMeta(settings.DefaultLanguage, settings.AvailableLanguages, Max(Max(Max(site.LastUpdated, schedule.LastUpdated), videos.LastUpdated), photos.LastUpdated)),
        new ArtistIdentity(Text(site.Identity.Name), Text(site.Identity.Profession), Text(site.Identity.Location)),
        new HeroContent(Text(site.Hero.Statement), Media(site.Hero.Image)),
        videos.Items.Select(Video).ToArray(),
        photos.Items.Select(Photo).ToArray(),
        schedule.Concerts.Select(Concert).ToArray(),
        new Biography(
            Text(site.Biography.Quote),
            Text(site.Biography.Summary),
            Text(site.Biography.Full),
            site.Biography.CvPath,
            site.Biography.UpdatedAt),
        Text(site.Repertoire),
        site.Projects.Select(Project).ToArray(),
        new PressContent(
            site.Press.Quotes.Select(x => new PressQuote(Text(x.Text), x.Publication, x.Author, x.PublishedAt, x.SourceUrl)).ToArray(),
            site.Press.PressKitPath,
            site.Press.PhotosPath),
        new ContactContent(site.Contacts.GeneralEmail, site.Contacts.ManagementEmail, site.Contacts.PressEmail, site.Contacts.SocialLinks));

    private static Concert Concert(ConcertDocument value) => new(
        value.Id,
        value.StartsAt,
        value.Timezone,
        Text(value.City),
        Text(value.Country),
        Text(value.Venue),
        Text(value.Series),
        value.Format,
        value.Program.Select(x => new ProgramWork(x.Composer, Text(x.Work))).ToArray(),
        value.Artists.Select(x => new ConcertArtist(Text(x.Role), Text(x.Name))).ToArray(),
        value.TicketUrl,
        value.Status,
        value.SourceUrl,
        value.VerifiedAt);

    private static PerformanceVideo Video(VideoDocument value) =>
        new(value.Id, value.Featured, Text(value.Title), value.Composer, Text(value.Caption), value.YouTubeUrl, value.RecordedAt);

    private static GalleryPhoto Photo(PhotoDocument value) =>
        new(value.Id, value.Path, Text(value.Caption), value.TakenAt);

    private static ArtisticProject Project(ArtisticProjectDocument value) =>
        new(value.Id, Text(value.Title), Text(value.Description), Media(value.Image));

    private static MediaAsset Media(MediaAssetDocument value) => new(value.Path, Text(value.Alt), value.Credit, value.Usage);
    private static LocalizedText Text(string? value) => LocalizedText.From(value ?? string.Empty);
    private static string NormalizeLanguage(string language) => language.Split('-', '_')[0].ToLowerInvariant();
    private static DateOnly Max(DateOnly left, DateOnly right) => left >= right ? left : right;
}
