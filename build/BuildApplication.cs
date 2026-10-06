using Build.Targets;

namespace Build;

internal sealed class BuildApplication(
    string[] args,
    BuildSolutionTarget buildSolution,
    WebTarget web,
    LocalWebTarget localWeb,
    ContentValidationTarget contentValidation,
    TelegramHistoryTarget telegramHistory,
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
        return Task.FromResult(0);
    }
}
