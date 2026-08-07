using Build.Targets;
using Pure.DI;
using System.Diagnostics;

namespace Build;

internal partial class Composition
{
    [Conditional("DI")]
    private static void SetupDI() =>
        DI.Setup()
            .Hint(Hint.ThreadSafe, "Off")
            .Root<BuildApplication>(nameof(Root))
            .Arg<string[]>("args")
            .Arg<CancellationToken>("cancellationToken")
            .Singleton<BuildPaths, ProcessRunner, BuildSolutionTarget, WebTarget, LocalWebTarget, ContentValidationTarget>();
}
