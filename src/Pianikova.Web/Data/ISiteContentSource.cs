namespace Pianikova.Web.Data;

public interface ISiteContentSource
{
    Task<SiteSettings> LoadSettingsAsync(CancellationToken cancellationToken = default);
    Task<SiteContent> LoadAsync(string language, CancellationToken cancellationToken = default);
    string AssetUrl(string relativePath);
    string SourceDescription { get; }
}
