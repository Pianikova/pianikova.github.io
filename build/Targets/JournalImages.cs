using SkiaSharp;

namespace Build.Targets;

internal sealed record JournalImage(string File, int Width, int Height, string Color, string Lqip);

/// <summary>
/// Writes <c>NN.webp</c> (at most <see cref="MaxSide"/> px on the long side) and <c>NN-480.webp</c> for every image,
/// and computes the average color and a tiny blurred preview for placeholders.
/// </summary>
internal static class JournalImages
{
    public const int MaxSide = 1600;
    public const int SmallWidth = 480;
    private const int Quality = 75;
    private const int PreviewWidth = 16;

    public static JournalImage Convert(string source, string directory, string name)
    {
        using var original = SKBitmap.Decode(source) ?? throw new InvalidOperationException($"Не удалось прочитать изображение: {source}");
        var scale = Math.Min(1d, (double)MaxSide / Math.Max(original.Width, original.Height));
        using var full = Resize(original, (int)Math.Round(original.Width * scale));
        using var small = Resize(full, Math.Min(SmallWidth, full.Width));
        using var preview = Resize(full, PreviewWidth);
        using var pixel = Resize(preview, 1, 1);

        var file = name + ".webp";
        Save(full, Path.Combine(directory, file), Quality);
        Save(small, Path.Combine(directory, name + "-" + SmallWidth + ".webp"), Quality);

        var color = pixel.GetPixel(0, 0);
        using var encodedPreview = Encode(preview, 40);
        return new JournalImage(
            file,
            full.Width,
            full.Height,
            $"#{color.Red:x2}{color.Green:x2}{color.Blue:x2}",
            "data:image/webp;base64," + System.Convert.ToBase64String(encodedPreview.ToArray()));
    }

    private static SKBitmap Resize(SKBitmap source, int width, int? height = null)
    {
        width = Math.Max(1, width);
        var targetHeight = height ?? Math.Max(1, (int)Math.Round(source.Height * (double)width / source.Width));
        if (width == source.Width && targetHeight == source.Height) return source.Copy();
        var sampling = width < source.Width / 4
            ? new SKSamplingOptions(SKFilterMode.Linear, SKMipmapMode.Linear)
            : new SKSamplingOptions(SKCubicResampler.Mitchell);
        return source.Resize(new SKImageInfo(width, targetHeight, SKColorType.Rgba8888, SKAlphaType.Premul), sampling)
               ?? throw new InvalidOperationException("Не удалось изменить размер изображения.");
    }

    private static SKData Encode(SKBitmap bitmap, int quality)
    {
        using var image = SKImage.FromBitmap(bitmap);
        return image.Encode(SKEncodedImageFormat.Webp, quality) ?? throw new InvalidOperationException("Не удалось сохранить WebP.");
    }

    private static void Save(SKBitmap bitmap, string path, int quality)
    {
        using var data = Encode(bitmap, quality);
        using var stream = File.Create(path);
        data.SaveTo(stream);
    }
}
