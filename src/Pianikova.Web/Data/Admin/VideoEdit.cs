namespace Pianikova.Web.Data;

public sealed class VideoEdit
{
    public string Id { get; set; } = "";
    public bool Featured { get; set; }
    public string TitleEn { get; set; } = "";
    public string TitleRu { get; set; } = "";
    public string Composer { get; set; } = "";
    public string CaptionEn { get; set; } = "";
    public string CaptionRu { get; set; } = "";
    public string YouTubeUrl { get; set; } = "";
    public DateOnly RecordedAt { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);

    internal static VideoEdit FromDocuments(VideoDocument en, VideoDocument ru) => new()
    {
        Id = en.Id,
        Featured = en.Featured,
        TitleEn = en.Title,
        TitleRu = ru.Title,
        Composer = en.Composer,
        CaptionEn = en.Caption,
        CaptionRu = ru.Caption,
        YouTubeUrl = en.YouTubeUrl,
        RecordedAt = en.RecordedAt,
    };

    internal VideoDocument ToDocument(bool english) => new(
        Id.Trim(),
        Featured,
        english ? TitleEn.Trim() : TitleRu.Trim(),
        Composer.Trim(),
        english ? CaptionEn.Trim() : CaptionRu.Trim(),
        YouTubeUrl.Trim(),
        RecordedAt);
}
