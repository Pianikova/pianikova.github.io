namespace Build.Targets;

internal sealed class BuildPaths
{
    public BuildPaths()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory is not null && !File.Exists(Path.Combine(directory.FullName, "Pianikova.slnx")))
        {
            directory = directory.Parent;
        }

        SolutionDirectory = directory?.FullName ?? throw new InvalidOperationException("Cannot find Pianikova.slnx.");
    }

    public string SolutionDirectory { get; }
    public string WebProject => Path.Combine(SolutionDirectory, "src", "Pianikova.Web", "Pianikova.Web.csproj");
    public string WebOutput => Path.Combine(SolutionDirectory, "artifacts", "web");
}
