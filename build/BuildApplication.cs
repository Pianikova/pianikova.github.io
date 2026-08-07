using Build.Targets;

namespace Build;

internal sealed class BuildApplication(
    string[] args,
    BuildSolutionTarget buildSolution,
    WebTarget web,
    LocalWebTarget localWeb,
    ContentValidationTarget contentValidation,
    CancellationToken cancellationToken)
{
    public async Task<int> RunAsync()
    {
        var target = args.FirstOrDefault()?.ToLowerInvariant();
        if (target is "content-check") return contentValidation.Run();
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
        return Task.FromResult(0);
    }
}
