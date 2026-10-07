using Pianikova.Web;
using Pure.DI.MS;

var builder = WebAssemblyHostBuilder.CreateDefault(args);
var httpClient = new HttpClient { BaseAddress = new Uri(builder.HostEnvironment.BaseAddress) };
var composition = new Composition(httpClient);

builder.ConfigureContainer(composition);
builder.RootComponents.Add<App>("#app");
builder.RootComponents.Add<HeadOutlet>("head::after");
builder.Services.AddScoped(_ => httpClient);
builder.Services.AddLocalization(options => options.ResourcesPath = "Resources");
builder.Services.AddScoped<GitHubAuthState>();
builder.Services.AddScoped<GitHubApiClient>();

var host = builder.Build();
SiteSettings? settings;
try
{
    settings = await httpClient.GetFromJsonAsync<SiteSettings>("content/settings/site.json");
}
catch (Exception exception) when (exception is HttpRequestException or JsonException or TaskCanceledException)
{
    // The page can still start and show its own retry action if content is unavailable.
    settings = null;
}
settings ??= new SiteSettings(2, "en", ["en", "ru"]);
var js = host.Services.GetRequiredService<IJSRuntime>();
var language = await js.InvokeAsync<string>("pianikovaLanguage.resolve", settings.AvailableLanguages, settings.DefaultLanguage);
var culture = CultureInfo.GetCultureInfo(language == "ru" ? "ru-RU" : "en-US");
CultureInfo.DefaultThreadCurrentCulture = culture;
CultureInfo.DefaultThreadCurrentUICulture = culture;

await host.RunAsync();
