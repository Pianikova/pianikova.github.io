namespace Build.Targets;

internal sealed class WebTarget(BuildPaths paths, ProcessRunner runner)
{
    public async Task<int> RunAsync(CancellationToken cancellationToken)
    {
        Console.WriteLine("Publishing the GitHub Pages application...");
        var result = await runner.RunAsync([
            "publish", paths.WebProject,
            "--configuration", "Release",
            "--output", paths.WebOutput,
            "-p:PianikovaProduction=true"
        ], cancellationToken);
        if (result != 0) return result;

        var wwwroot = Path.Combine(paths.WebOutput, "wwwroot");
        await File.WriteAllTextAsync(Path.Combine(wwwroot, ".nojekyll"), string.Empty, cancellationToken);
        await File.WriteAllTextAsync(Path.Combine(wwwroot, "404.html"), RedirectPage, cancellationToken);
        Console.WriteLine($"GitHub Pages artifact: {wwwroot}");
        return 0;
    }

    // GitHub Pages has no server-side routing, so a direct load/refresh of a client-side
    // route (e.g. /admin) 404s. Stash the intended path and bounce to "/"; index.html
    // restores it via history.replaceState before Blazor's router starts.
    private const string RedirectPage = """
        <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Julia Pianikova</title>
        <script>
            sessionStorage.setItem('spa-redirect-path', location.pathname + location.search + location.hash);
            location.replace('/');
        </script>
        </head><body></body></html>
        """;
}
