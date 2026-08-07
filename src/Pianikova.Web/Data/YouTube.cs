namespace Pianikova.Web.Data;

public static class YouTube
{
    public static string? ExtractVideoId(string? url)
    {
        if (string.IsNullOrWhiteSpace(url) || !Uri.TryCreate(url, UriKind.Absolute, out var uri))
        {
            return null;
        }

        var host = uri.Host.StartsWith("www.", StringComparison.OrdinalIgnoreCase) ? uri.Host[4..] : uri.Host;

        if (host.Equals("youtu.be", StringComparison.OrdinalIgnoreCase))
        {
            var id = uri.AbsolutePath.Trim('/');
            return string.IsNullOrWhiteSpace(id) ? null : id;
        }

        if (!host.Equals("youtube.com", StringComparison.OrdinalIgnoreCase) && !host.Equals("m.youtube.com", StringComparison.OrdinalIgnoreCase))
        {
            return null;
        }

        if (uri.AbsolutePath.StartsWith("/embed/", StringComparison.OrdinalIgnoreCase))
        {
            var id = uri.AbsolutePath["/embed/".Length..].Trim('/');
            return string.IsNullOrWhiteSpace(id) ? null : id;
        }

        return GetQueryParam(uri.Query, "v");
    }

    public static string ThumbnailUrl(string videoId) => $"https://i.ytimg.com/vi/{videoId}/hqdefault.jpg";

    public static string EmbedUrl(string videoId) => $"https://www.youtube-nocookie.com/embed/{videoId}?autoplay=1&rel=0";

    private static string? GetQueryParam(string query, string key)
    {
        foreach (var pair in query.TrimStart('?').Split('&', StringSplitOptions.RemoveEmptyEntries))
        {
            var parts = pair.Split('=', 2);
            if (parts.Length == 2 && Uri.UnescapeDataString(parts[0]) == key)
            {
                return Uri.UnescapeDataString(parts[1]);
            }
        }

        return null;
    }
}
