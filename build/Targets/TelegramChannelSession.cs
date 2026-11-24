using System.Globalization;
using System.Text;
using TL;

namespace Build.Targets;

internal sealed class TelegramChannelSession(bool allowPrompts, bool useEnvironment = true) : IDisposable
{
    private readonly HashSet<string> _secrets = new(StringComparer.Ordinal);

    public WTelegram.Client Client { get; private set; } = null!;

    public async Task<Channel> ConnectAsync(CancellationToken cancellationToken)
    {
        var apiId = useEnvironment ? null : ReadApiId();
        var apiHash = ReadSetting("PIANIKOVA_TELEGRAM_API_HASH", "Telegram api_hash: ", secret: true);
        var phone = useEnvironment ? null : ReadPhone();
        var directory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Pianikova");
        Directory.CreateDirectory(directory);
        var sessionPath = Path.Combine(directory, "telegram-history.session");
        if (!allowPrompts && !File.Exists(sessionPath))
            throw new InvalidOperationException("Telegram session is missing. Run telegram-journal-sync in an interactive terminal first.");

        string? Config(string name) => name switch
        {
            "api_id" => apiId ?? ReadApiId(),
            "api_hash" => apiHash,
            // A saved journal session identifies its user. Background runs never start a new login.
            "user_id" when useEnvironment => "-1",
            "phone_number" => phone ?? (allowPrompts ? ReadPhone() : throw SignInRequired()),
            "verification_code" => ReadChallenge("Telegram login code: "),
            "email" => ReadChallenge("Telegram email (if requested): ", secret: false),
            "email_verification_code" => ReadChallenge("Telegram email code: "),
            "password" => ReadChallenge("Telegram two-step password: "),
            "session_pathname" => sessionPath,
            "first_name" or "last_name" => throw new InvalidOperationException("This target only supports existing Telegram accounts."),
            _ => null
        };

        cancellationToken.ThrowIfCancellationRequested();
        WTelegram.Helpers.Log = (_, _) => { };
        Client = new WTelegram.Client(Config)
        {
            TcpHandler = (host, port) => TelegramTcpConnection.OpenAsync(host, port, cancellationToken)
        };
        try
        {
            await Client.LoginUserIfNeeded(reloginOnFailedResume: allowPrompts);
        }
        catch (RpcException exception) when (!allowPrompts && exception.Code == 401)
        {
            throw SignInRequired();
        }
        cancellationToken.ThrowIfCancellationRequested();

        var resolved = await Client.Contacts_ResolveUsername(JournalExport.ChannelUsername);
        if (resolved.Chat is not Channel channel || channel.id != JournalExport.ChannelId)
            throw new InvalidOperationException("Telegram resolved a different channel. Import stopped.");
        return channel;
    }

    public string Redact(string message)
    {
        foreach (var secret in _secrets.OrderByDescending(value => value.Length))
            message = message.Replace(secret, "[redacted]", StringComparison.Ordinal);
        return message;
    }

    public void Dispose() => Client?.Dispose();

    public static DateTime AsUtc(DateTime value) => value.Kind switch
    {
        DateTimeKind.Utc => value,
        DateTimeKind.Local => value.ToUniversalTime(),
        _ => DateTime.SpecifyKind(value, DateTimeKind.Utc)
    };

    private string ReadApiId()
    {
        var value = ReadSetting("PIANIKOVA_TELEGRAM_API_ID", "Telegram api_id: ");
        if (!int.TryParse(value, NumberStyles.None, CultureInfo.InvariantCulture, out var id) || id <= 0)
            throw new InvalidOperationException("api_id must be a positive number.");
        return id.ToString(CultureInfo.InvariantCulture);
    }

    private string ReadPhone() => ReadSetting("PIANIKOVA_TELEGRAM_PHONE", "Telegram phone number (international format): ", redact: true);

    private string ReadSetting(string variable, string prompt, bool secret = false, bool redact = false)
    {
        var value = useEnvironment ? Environment.GetEnvironmentVariable(variable)?.Trim() : null;
        if (string.IsNullOrWhiteSpace(value) && allowPrompts) value = ReadValue(prompt, secret);
        if (string.IsNullOrWhiteSpace(value))
            throw new InvalidOperationException($"{variable} is required. Check the environment or sign in interactively.");
        if (secret || redact) _secrets.Add(value);
        return value;
    }

    private string ReadChallenge(string prompt, bool secret = true)
    {
        if (!allowPrompts) throw SignInRequired();
        var value = ReadValue(prompt, secret);
        if (value.Length > 0) _secrets.Add(value);
        return value;
    }

    private static InvalidOperationException SignInRequired() =>
        new("Telegram requires sign-in. Run telegram-journal-sync in an interactive terminal to renew the session.");

    private static string ReadValue(string prompt, bool secret)
    {
        Console.Write(prompt);
        if (!secret) return Console.ReadLine()?.Trim() ?? string.Empty;

        var value = new StringBuilder();
        while (true)
        {
            var key = Console.ReadKey(intercept: true);
            if (key.Key == ConsoleKey.Enter) { Console.WriteLine(); return value.ToString(); }
            if (key.Key == ConsoleKey.Backspace) { if (value.Length > 0) value.Length--; continue; }
            if (!char.IsControl(key.KeyChar)) value.Append(key.KeyChar);
        }
    }
}
