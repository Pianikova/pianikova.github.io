using Pure.DI.MS;

namespace Pianikova.Web;

internal partial class Composition : ServiceProviderFactory<Composition>
{
    [Conditional("DI")]
    private static void SetupDI() =>
        DI.Setup()
            .Hint(Hint.ThreadSafe, "Off")
            .Arg<HttpClient>("httpClient")
            .Root<ISiteContentSource>()
            .Singleton<SiteContentSource>();
}
