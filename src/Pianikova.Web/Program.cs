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
var settingsUrl = httpClient.BaseAddress?.IsLoopback == true
    ? "content/settings/site.json"
    : $"https://raw.githubusercontent.com/{GitHubRepository.FullName}/{GitHubRepository.Branch}/content/settings/site.json";
var settings = await httpClient.GetFromJsonAsync<SiteSettings>(settingsUrl)
               ?? new SiteSettings(2, "en", ["en", "ru"]);
var js = host.Services.GetRequiredService<IJSRuntime>();
var language = await js.InvokeAsync<string>("pianikovaLanguage.resolve", settings.AvailableLanguages, settings.DefaultLanguage);
var culture = CultureInfo.GetCultureInfo(language == "ru" ? "ru-RU" : "en-US");
CultureInfo.DefaultThreadCurrentCulture = culture;
CultureInfo.DefaultThreadCurrentUICulture = culture;

await host.RunAsync();
