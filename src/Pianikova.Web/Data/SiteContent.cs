namespace Pianikova.Web.Data;

public sealed record SiteSettings(int SchemaVersion, string DefaultLanguage, IReadOnlyList<string> AvailableLanguages);

public sealed record SiteContent(
    int SchemaVersion,
    SiteMeta Meta,
    ArtistIdentity Identity,
    HeroContent Hero,
    IReadOnlyList<PerformanceVideo> Videos,
    IReadOnlyList<GalleryPhoto> Photos,
    IReadOnlyList<Concert> Concerts,
    Biography Biography,
    LocalizedText Repertoire,
    IReadOnlyList<ArtisticProject> Projects,
    PressContent Press,
    ContactContent Contacts);

public sealed record SiteMeta(string DefaultLanguage, IReadOnlyList<string> AvailableLanguages, DateOnly LastUpdated);
public sealed record ArtistIdentity(LocalizedText Name, LocalizedText Profession, LocalizedText Location);
public sealed record HeroContent(LocalizedText Statement, MediaAsset Image);
public sealed record MediaAsset(string Path, LocalizedText Alt, string Credit, string Usage);
public sealed record PerformanceVideo(string Id, bool Featured, LocalizedText Title, string Composer, LocalizedText Caption, string YouTubeUrl, DateOnly RecordedAt);
public sealed record GalleryPhoto(string Id, string Path, LocalizedText Caption, DateOnly TakenAt);

public sealed record Concert(
    string Id,
    DateTimeOffset StartsAt,
    string Timezone,
    LocalizedText City,
    LocalizedText Country,
    LocalizedText Venue,
    LocalizedText Series,
    string Format,
    IReadOnlyList<ProgramWork> Program,
    IReadOnlyList<ConcertArtist> Artists,
    string TicketUrl,
    string Status,
    string SourceUrl,
    DateOnly VerifiedAt);

public sealed record ProgramWork(string Composer, LocalizedText Work);
public sealed record ConcertArtist(LocalizedText Role, LocalizedText Name);
public sealed record Biography(LocalizedText Quote, LocalizedText Summary, LocalizedText Full, string CvPath, DateOnly UpdatedAt);
public sealed record ArtisticProject(string Id, LocalizedText Title, LocalizedText Description, MediaAsset Image);
public sealed record PressContent(IReadOnlyList<PressQuote> Quotes, string PressKitPath, string PhotosPath);
public sealed record PressQuote(LocalizedText Text, string Publication, string Author, DateOnly PublishedAt, string SourceUrl);
public sealed record ContactContent(string GeneralEmail, string ManagementEmail, string PressEmail, IReadOnlyList<ExternalLink> SocialLinks);
public sealed record ExternalLink(string Service, string Url);

public sealed record LocalizedText(string Ru, string En)
{
    public string In(string language) => language.Equals("en", StringComparison.OrdinalIgnoreCase) && !string.IsNullOrWhiteSpace(En) ? En : Ru;

    public static LocalizedText From(string text) => new(text, text);
}

internal sealed record SiteDocument(
    string Id,
    DateOnly LastUpdated,
    ArtistIdentityDocument Identity,
    HeroDocument Hero,
    BiographyDocument Biography,
    string? Repertoire,
    IReadOnlyList<ArtisticProjectDocument> Projects,
    PressDocument Press,
    ContactDocument Contacts);

internal sealed record ArtistIdentityDocument(string Name, string Profession, string Location);
internal sealed record HeroDocument(string Statement, MediaAssetDocument Image);
internal sealed record MediaAssetDocument(string Path, string Alt, string Credit, string Usage);
internal sealed record BiographyDocument(string Quote, string Summary, string Full, string CvPath, DateOnly UpdatedAt);
internal sealed record ArtisticProjectDocument(string Id, string Title, string Description, MediaAssetDocument Image);
internal sealed record PressDocument(IReadOnlyList<PressQuoteDocument> Quotes, string PressKitPath, string PhotosPath);
internal sealed record PressQuoteDocument(string Text, string Publication, string Author, DateOnly PublishedAt, string SourceUrl);
internal sealed record ContactDocument(string GeneralEmail, string ManagementEmail, string PressEmail, IReadOnlyList<ExternalLink> SocialLinks);

internal sealed record VideosDocument(string Id, DateOnly LastUpdated, IReadOnlyList<VideoDocument> Items);
internal sealed record VideoDocument(string Id, bool Featured, string Title, string Composer, string Caption, string YouTubeUrl, DateOnly RecordedAt);

internal sealed record PhotosDocument(string Id, DateOnly LastUpdated, IReadOnlyList<PhotoDocument> Items);
internal sealed record PhotoDocument(string Id, string Path, string Caption, DateOnly TakenAt);

internal sealed record ScheduleDocument(string Id, DateOnly LastUpdated, IReadOnlyList<ConcertDocument> Concerts);
internal sealed record ConcertDocument(
    string Id,
    DateTimeOffset StartsAt,
    string Timezone,
    string City,
    string Country,
    string Venue,
    string Series,
    string Format,
    IReadOnlyList<ProgramWorkDocument> Program,
    IReadOnlyList<ConcertArtistDocument> Artists,
    string TicketUrl,
    string Status,
    string SourceUrl,
    DateOnly VerifiedAt);
internal sealed record ProgramWorkDocument(string Composer, string Work);
internal sealed record ConcertArtistDocument(string Role, string Name);
