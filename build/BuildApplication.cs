using Build.Targets;

namespace Build;

internal sealed class BuildApplication(
    string[] args,
    BuildSolutionTarget buildSolution,
    WebTarget web,
    LocalWebTarget localWeb,
    ContentValidationTarget contentValidation,
    TelegramHistoryTarget telegramHistory,
    TelegramJournalTarget telegramJournal,
    CancellationToken cancellationToken)
{
    public async Task<int> RunAsync()
    {
        var target = args.FirstOrDefault()?.ToLowerInvariant();
        if (target is "content-check") return contentValidation.Run();
        if (target is "telegram-history")
        {
            var before = contentValidation.Run();
            if (before != 0) return before;
            var import = await telegramHistory.RunAsync(cancellationToken);
            return import == 0 ? contentValidation.Run() : import;
        }
        if (target is "telegram-history-export")
        {
            var before = contentValidation.Run();
            if (before != 0) return before;
            var import = await telegramHistory.RunExportAsync(cancellationToken);
            return import == 0 ? contentValidation.Run() : import;
        }
        if (target is "telegram-journal")
        {
            var before = contentValidation.Run(validateJournalIndexes: false);
            if (before != 0) return before;
            var export = await telegramJournal.RunAsync(args.Skip(1).ToArray(), cancellationToken);
            return export == 0 ? contentValidation.Run() : export;
        }
        if (target is not ("build" or "web" or "local-web")) return await HelpAsync();

        var validationResult = contentValidation.Run();
        if (validationResult != 0) return validationResult;

        return target switch
        {
            "build" => await buildSolution.RunAsync(cancellationToken),
            "web" => await web.RunAsync(cancellationToken),
            "local-web" => await localWeb.RunAsync(!args.Contains("--no-browser", StringComparer.OrdinalIgnoreCase), cancellationToken),
            _ => 1
        };
    }

    private static Task<int> HelpAsync()
    {
        Console.WriteLine("Pianikova build targets:");
        Console.WriteLine("  build                  Build the solution");
        Console.WriteLine("  content-check          Validate all CMS content");
        Console.WriteLine("  web                    Publish GitHub Pages artifact");
        Console.WriteLine("  local-web [--no-browser]  Run the site locally (editor at /admin)");
        Console.WriteLine("  telegram-history       Import channel posts for a date range into content/telegram/posts.json");
        Console.WriteLine("  telegram-history-export  Import a Telegram Desktop channel JSON export without API credentials");
        Console.WriteLine("  telegram-journal [result.json] [--reindex] [--rebuild all|ID,ID]");
        Console.WriteLine("                         Build the journal in content/telegram/journal from a Telegram Desktop export");
        return Task.FromResult(0);
    }
}
