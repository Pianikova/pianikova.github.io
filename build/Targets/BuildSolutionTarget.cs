namespace Build.Targets;

internal sealed class BuildSolutionTarget(BuildPaths paths, ProcessRunner runner)
{
    public Task<int> RunAsync(CancellationToken cancellationToken) =>
        runner.RunAsync(["build", Path.Combine(paths.SolutionDirectory, "Pianikova.slnx")], cancellationToken);
}
