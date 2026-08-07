namespace Pianikova.Web.Data;

internal sealed class GitHubApiClient : IDisposable
{
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web);

    private readonly HttpClient _http = new() { BaseAddress = new Uri("https://api.github.com/") };
    private readonly GitHubAuthState _auth;

    public GitHubApiClient(GitHubAuthState auth)
    {
        _auth = auth;
        _http.DefaultRequestHeaders.UserAgent.ParseAdd("Pianikova-Admin");
        _http.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/vnd.github+json"));
    }

    public async Task<GitHubUser> GetAuthenticatedUserAsync(CancellationToken cancellationToken = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, "user");
        using var response = await SendAsync(request, cancellationToken);
        return await ReadJsonAsync<GitHubUser>(response, cancellationToken);
    }

    public async Task<T> GetFileAsync<T>(string path, CancellationToken cancellationToken = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, ContentsUrl(path));
        using var response = await SendAsync(request, cancellationToken);
        var file = await ReadJsonAsync<GitHubFileResponse>(response, cancellationToken);
        var json = Encoding.UTF8.GetString(Convert.FromBase64String(file.Content.Replace("\n", "")));
        return JsonSerializer.Deserialize<T>(json, SerializerOptions)
               ?? throw new InvalidOperationException($"Content file {path} is empty.");
    }

    public async Task PutJsonAsync<T>(string path, T content, string message, CancellationToken cancellationToken = default)
    {
        var json = JsonSerializer.Serialize(content, SerializerOptions);
        await PutBytesAsync(path, Encoding.UTF8.GetBytes(json), message, cancellationToken);
    }

    public Task PutMediaAsync(string path, byte[] bytes, string message, CancellationToken cancellationToken = default) =>
        PutBytesAsync(path, bytes, message, cancellationToken);

    private async Task PutBytesAsync(string path, byte[] bytes, string message, CancellationToken cancellationToken)
    {
        var sha = await TryGetShaAsync(path, cancellationToken);
        using var request = new HttpRequestMessage(HttpMethod.Put, ContentsUrl(path));
        var body = new GitHubPutRequest(message, Convert.ToBase64String(bytes), sha, GitHubRepository.Branch);
        request.Content = JsonContent.Create(body, options: SerializerOptions);
        using var response = await SendAsync(request, cancellationToken);
        _ = await ReadJsonAsync<GitHubPutResponse>(response, cancellationToken);
    }

    private async Task<string?> TryGetShaAsync(string path, CancellationToken cancellationToken)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, ContentsUrl(path));
        ApplyAuth(request);
        using var response = await _http.SendAsync(request, cancellationToken);
        if (response.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }

        await EnsureSuccessAsync(response);
        var file = await response.Content.ReadFromJsonAsync<GitHubFileResponse>(SerializerOptions, cancellationToken);
        return file?.Sha;
    }

    private async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        ApplyAuth(request);
        var response = await _http.SendAsync(request, cancellationToken);
        await EnsureSuccessAsync(response);
        return response;
    }

    private void ApplyAuth(HttpRequestMessage request)
    {
        var token = _auth.Token ?? throw new InvalidOperationException("Вы не вошли через GitHub.");
        request.Headers.Authorization = new AuthenticationHeaderValue("token", token);
    }

    private static async Task<T> ReadJsonAsync<T>(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        var result = await response.Content.ReadFromJsonAsync<T>(SerializerOptions, cancellationToken);
        return result ?? throw new InvalidOperationException("GitHub вернул пустой ответ.");
    }

    private static async Task EnsureSuccessAsync(HttpResponseMessage response)
    {
        if (response.IsSuccessStatusCode)
        {
            return;
        }

        if (response.StatusCode == HttpStatusCode.Conflict)
        {
            throw new InvalidOperationException("Файл изменился на GitHub с момента загрузки. Обновите страницу и повторите.");
        }

        var body = await response.Content.ReadAsStringAsync();
        throw new InvalidOperationException($"Ошибка GitHub API ({(int)response.StatusCode}): {body}");
    }

    private static string ContentsUrl(string path) =>
        $"repos/{GitHubRepository.FullName}/contents/{path}?ref={GitHubRepository.Branch}";

    public void Dispose() => _http.Dispose();
}

internal sealed record GitHubUser(string Login);
internal sealed record GitHubFileResponse(string Sha, string Content);
internal sealed record GitHubPutRequest(string Message, string Content, string? Sha, string Branch);
internal sealed record GitHubPutResponse(GitHubPutContentInfo Content);
internal sealed record GitHubPutContentInfo(string Sha);
