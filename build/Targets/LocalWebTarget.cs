using System.Diagnostics;

namespace Build.Targets;

internal sealed class LocalWebTarget(BuildPaths paths, ProcessRunner runner)
{
    public async Task<int> RunAsync(bool launchBrowser, CancellationToken cancellationToken)
    {
        SyncContent();

        if (launchBrowser)
        {
            _ = Task.Run(async () =>
            {
                await Task.Delay(2500, cancellationToken);
                Process.Start(new ProcessStartInfo("http://localhost:5188") { UseShellExecute = true });
            }, cancellationToken);
        }

        Console.WriteLine("Local content source: content/settings/site.json");
        Console.WriteLine("Editor: http://localhost:5188/admin (sign in with GitHub — edits go straight to the real repo)");

        return await runner.RunAsync(["run", "--project", paths.WebProject, "--urls", "http://localhost:5188"], cancellationToken);
    }

    private void SyncContent()
    {
        var source = Path.Combine(paths.SolutionDirectory, "content");
        var destination = Path.Combine(paths.SolutionDirectory, "src", "Pianikova.Web", "wwwroot", "content");
        Directory.CreateDirectory(destination);
        foreach (var sourcePath in Directory.EnumerateFiles(source, "*", SearchOption.AllDirectories))
        {
            var relativePath = Path.GetRelativePath(source, sourcePath);
            var destinationPath = Path.Combine(destination, relativePath);
            Directory.CreateDirectory(Path.GetDirectoryName(destinationPath)!);
            File.Copy(sourcePath, destinationPath, true);
        }
    }
}
