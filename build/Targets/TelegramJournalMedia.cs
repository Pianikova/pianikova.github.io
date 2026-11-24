using System.Globalization;
using TL;

namespace Build.Targets;

internal sealed class TelegramJournalMedia(
    WTelegram.Client client, IReadOnlyCollection<long> publishedAudio, CancellationToken cancellationToken) : IDisposable
{
    private readonly DirectoryInfo _directory = Directory.CreateTempSubdirectory("pianikova-telegram-");

    public string DirectoryPath => _directory.FullName;

    public async Task<ExportMessage> AddAsync(Message message, ExportMessage post)
    {
        switch (message.media)
        {
            case MessageMediaPhoto { photo: Photo photo } media:
            {
                var size = LargestImage(photo.sizes);
                if (size is null) return post;
                var file = await DownloadAsync(post.Id, ".jpg", stream => client.DownloadFileAsync(photo, stream, size, CheckCancellation));
                return post with { Photo = file, MediaSpoiler = media.flags.HasFlag(MessageMediaPhoto.Flags.spoiler) };
            }
            case MessageMediaDocument { document: Document document }:
                return await AddDocumentAsync(document, post);
            case MessageMediaPoll { poll: { } poll }:
                return post with { Poll = new ExportPoll(poll.question.text, poll.answers.OfType<PollAnswer>().Select(answer => answer.text.text).ToArray()) };
            default:
                return post;
        }
    }

    public void Dispose() => _directory.Delete(recursive: true);

    private async Task<ExportMessage> AddDocumentAsync(Document document, ExportMessage post)
    {
        if (document.GetAttribute<DocumentAttributeSticker>() is not null ||
            document.GetAttribute<DocumentAttributeCustomEmoji>() is not null)
            return post with { MediaType = "sticker" };

        if (document.GetAttribute<DocumentAttributeAudio>() is { } audio)
        {
            var voice = audio.flags.HasFlag(DocumentAttributeAudio.Flags.voice);
            var file = publishedAudio.Contains(post.Id)
                ? await DownloadAsync(post.Id, AudioExtension(document), stream => client.DownloadFileAsync(document, stream, progress: CheckCancellation))
                : null;
            return post with
            {
                MediaType = voice ? "voice_message" : "audio_file", File = file, FileName = document.Filename,
                Duration = audio.duration, Performer = audio.performer, Title = audio.title
            };
        }

        var video = document.GetAttribute<DocumentAttributeVideo>();
        var animated = document.GetAttribute<DocumentAttributeAnimated>() is not null || document.mime_type == "image/gif";
        if (video is not null || animated)
        {
            var thumbnail = LargestImage(document.thumbs);
            var file = thumbnail is null ? null
                : await DownloadAsync(post.Id, ".jpg", stream => client.DownloadFileAsync(document, stream, thumbnail, CheckCancellation));
            var type = video?.flags.HasFlag(DocumentAttributeVideo.Flags.round_message) == true ? "video_message"
                : animated ? "animation" : "video_file";
            return post with { MediaType = type, Thumbnail = file, Duration = (int?)video?.duration };
        }

        return post with { File = post.Id.ToString(CultureInfo.InvariantCulture), FileName = document.Filename };
    }

    private async Task<string> DownloadAsync(long messageId, string extension, Func<Stream, Task> download)
    {
        cancellationToken.ThrowIfCancellationRequested();
        var name = messageId.ToString(CultureInfo.InvariantCulture) + extension;
        await using var stream = File.Create(Path.Combine(DirectoryPath, name));
        await download(stream);
        cancellationToken.ThrowIfCancellationRequested();
        if (stream.Length == 0) throw new IOException($"Telegram вернул пустой медиафайл для сообщения {messageId}.");
        return name;
    }

    private void CheckCancellation(long _, long __) => cancellationToken.ThrowIfCancellationRequested();

    private static PhotoSizeBase? LargestImage(IEnumerable<PhotoSizeBase>? sizes) => sizes?
        .Where(size => size is PhotoSize or PhotoSizeProgressive or PhotoCachedSize)
        .MaxBy(size => (long)size.Width * size.Height);

    private static string AudioExtension(Document document)
    {
        var extension = Path.GetExtension(document.Filename);
        if (extension is { Length: >= 2 and <= 8 } && extension.Skip(1).All(char.IsAsciiLetterOrDigit))
            return extension.ToLowerInvariant();
        return document.mime_type switch
        {
            "audio/mpeg" => ".mp3",
            "audio/mp4" => ".m4a",
            "audio/ogg" or "application/ogg" => ".ogg",
            "audio/wav" or "audio/x-wav" => ".wav",
            "audio/flac" => ".flac",
            "audio/aac" => ".aac",
            _ => ".bin"
        };
    }
}
