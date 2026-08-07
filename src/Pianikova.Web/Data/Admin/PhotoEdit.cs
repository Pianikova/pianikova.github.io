namespace Pianikova.Web.Data;

public sealed class PhotoEdit
{
    public string Id { get; set; } = "";
    public string Path { get; set; } = "";
    public string CaptionEn { get; set; } = "";
    public string CaptionRu { get; set; } = "";
    public DateOnly TakenAt { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);

    public byte[]? PendingBytes { get; set; }
    public string? PendingFileName { get; set; }
    public string? PendingDataUrl { get; set; }

    internal static PhotoEdit FromDocuments(PhotoDocument en, PhotoDocument ru) => new()
    {
        Id = en.Id,
        Path = en.Path,
        CaptionEn = en.Caption,
        CaptionRu = ru.Caption,
        TakenAt = en.TakenAt,
    };

    internal PhotoDocument ToDocument(bool english) => new(
        Id.Trim(),
        Path.Trim(),
        english ? CaptionEn.Trim() : CaptionRu.Trim(),
        TakenAt);
}
