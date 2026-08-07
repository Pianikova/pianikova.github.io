namespace Pianikova.Web.Data;

internal sealed class GitHubAuthState(IJSRuntime js)
{
    // Same Cloudflare Worker that used to serve Decap CMS's OAuth handshake
    // (cloudflare/decap-oauth) — the worker itself is generic, only the opener changed.
    private const string AuthUrl = "https://pianikova-cms-oauth.nikolay-pyanikov.workers.dev/auth";

    public string? Token { get; private set; }
    public string? Username { get; private set; }
    public bool IsAuthenticated => Token is not null;

    public async Task InitializeAsync()
    {
        if (Token is not null)
        {
            return;
        }

        Token = await js.InvokeAsync<string?>("pianikovaAdmin.getToken");
        Username = await js.InvokeAsync<string?>("pianikovaAdmin.getUsername");
    }

    public async Task LoginAsync(GitHubApiClient api)
    {
        Token = await js.InvokeAsync<string>("pianikovaAdmin.login", AuthUrl);
        await js.InvokeVoidAsync("pianikovaAdmin.setToken", Token);

        var user = await api.GetAuthenticatedUserAsync();
        Username = user.Login;
        await js.InvokeVoidAsync("pianikovaAdmin.setUsername", Username);
    }

    public async Task LogoutAsync()
    {
        Token = null;
        Username = null;
        await js.InvokeVoidAsync("pianikovaAdmin.clearToken");
    }
}
