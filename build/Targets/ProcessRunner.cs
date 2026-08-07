using System.Diagnostics;

namespace Build.Targets;

internal sealed class ProcessRunner(BuildPaths paths)
{
    public async Task<int> RunAsync(IEnumerable<string> arguments, CancellationToken cancellationToken)
    {
        var startInfo = new ProcessStartInfo("dotnet")
        {
            WorkingDirectory = paths.SolutionDirectory,
            UseShellExecute = false
        };
        foreach (var argument in arguments) startInfo.ArgumentList.Add(argument);

        using var process = Process.Start(startInfo) ?? throw new InvalidOperationException("Could not start dotnet.");
        await process.WaitForExitAsync(cancellationToken);
        return process.ExitCode;
    }
}
