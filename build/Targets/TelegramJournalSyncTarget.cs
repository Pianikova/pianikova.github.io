using TL;

namespace Build.Targets;

internal sealed class TelegramJournalSyncTarget(JournalStore store, TelegramJournalTarget journal)
{
    public async Task<int> RunAsync(IReadOnlyList<string> args, CancellationToken cancellationToken)
    {
        if (args.Count > 1 || args.Count == 1 && args[0] != "--no-prompt")
        {
            Console.Error.WriteLine("Usage: telegram-journal-sync [--no-prompt]");
            return 1;
        }

        using var session = new TelegramChannelSession(args.Count == 0 && !Console.IsInputRedirected);
        try
        {
            var channel = await session.ConnectAsync(cancellationToken);
            var settings = store.ReadSettings();
            var posts = store.ReadPosts();
            var known = posts.SelectMany(post => post.Meta.Messages.Append(post.Meta.Id)).ToHashSet();

            Console.WriteLine($"Читаю историю @{JournalExport.ChannelUsername}.");
            var startedAt = DateTime.UtcNow;
            var history = await ReadHistoryAsync(session.Client, channel, cancellationToken);
            var pending = PendingMessages(history, known, startedAt);
            if (pending.Count > 0) Console.WriteLine($"Незавершённый выпуск будет обработан при следующем запуске. Сообщений: {pending.Count}.");

            using var media = new TelegramJournalMedia(session.Client, settings.PublishedAudio, cancellationToken);
            var messages = new List<ExportMessage>(history.Count - pending.Count);
            foreach (var message in history)
            {
                cancellationToken.ThrowIfCancellationRequested();
                if (pending.Contains(message.ID)) continue;
                var post = ToJournalMessage(message);
                if (!known.Contains(post.Id) && !post.Forwarded)
                    post = await media.AddAsync(message, post);
                messages.Add(post);
            }

            journal.ImportFromChannel(messages, media.DirectoryPath, settings, posts, cancellationToken);
            return 0;
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            Console.Error.WriteLine("Синхронизация журнала отменена.");
            return 1;
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine($"Синхронизация журнала завершилась ошибкой. {session.Redact(exception.Message)}");
            return 1;
        }
    }

    private static async Task<IReadOnlyList<Message>> ReadHistoryAsync(WTelegram.Client client, Channel channel, CancellationToken cancellationToken)
    {
        var byId = new Dictionary<int, Message>();
        var offset = 0;
        while (true)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var page = await client.Messages_GetHistory(channel, offset_id: offset, limit: 100);
            if (page.Messages.Length == 0) break;
            foreach (var message in page.Messages.OfType<Message>())
                byId.TryAdd(message.ID, message);

            var nextOffset = page.Messages.Where(message => message.ID > 0).Select(message => message.ID).DefaultIfEmpty().Min();
            if (nextOffset == 0 || offset != 0 && nextOffset >= offset)
                throw new InvalidOperationException("Telegram вернул страницу истории без продвижения по ID.");
            offset = nextOffset;
        }
        return byId.Values.OrderBy(message => message.ID).ToArray();
    }

    private static HashSet<int> PendingMessages(IReadOnlyList<Message> history, HashSet<long> known, DateTime now)
    {
        var pending = new HashSet<int>();
        var nextDate = now;
        for (var index = history.Count - 1; index >= 0; index--)
        {
            var message = history[index];
            if (known.Contains(message.ID) || message.fwd_from is not null) break;
            var date = TelegramChannelSession.AsUtc(message.date);
            if (nextDate - date > TelegramJournalTarget.IssueGap) break;
            pending.Add(message.ID);
            nextDate = date;
        }
        return pending;
    }

    private static ExportMessage ToJournalMessage(Message message) => new(
        message.ID,
        TimeZoneInfo.ConvertTime(new DateTimeOffset(TelegramChannelSession.AsUtc(message.date)), JournalExport.MoscowTime),
        message.fwd_from is not null,
        MediaType: null, Photo: null, File: null, Thumbnail: null, Width: null, Height: null,
        Duration: null, Performer: null, Title: null, FileName: null, MediaSpoiler: false, Poll: null,
        Segments: TelegramJournalText.Read(message.message ?? string.Empty, message.entities ?? []),
        Reactions: message.reactions?.results.Sum(reaction => reaction.count) ?? 0);
}
