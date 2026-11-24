using System.Net.Sockets;
using System.Text;

namespace Build.Targets;

internal static class TelegramTcpConnection
{
    public static async Task<TcpClient> OpenAsync(string host, int port, CancellationToken cancellationToken)
    {
        var target = new UriBuilder(Uri.UriSchemeHttps, host, port).Uri;
        var systemProxy = HttpClient.DefaultProxy;
        var proxy = systemProxy.IsBypassed(target) ? null : systemProxy.GetProxy(target);

        var client = new TcpClient();
        try
        {
            if (proxy?.Scheme == Uri.UriSchemeHttp)
            {
                using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                deadline.CancelAfter(TimeSpan.FromSeconds(15));
                await client.ConnectAsync(proxy.IdnHost, proxy.Port, deadline.Token);
                await OpenTunnelAsync(client.GetStream(), $"{target.Host}:{port}", deadline.Token);
            }
            else
            {
                await client.ConnectAsync(host, port, cancellationToken);
            }
            return client;
        }
        catch (OperationCanceledException exception) when (!cancellationToken.IsCancellationRequested)
        {
            client.Dispose();
            throw new TimeoutException("Истекло время подключения к Telegram через системный HTTP-прокси.", exception);
        }
        catch
        {
            client.Dispose();
            throw;
        }
    }

    private static async Task OpenTunnelAsync(NetworkStream stream, string authority, CancellationToken cancellationToken)
    {
        var request = Encoding.ASCII.GetBytes($"CONNECT {authority} HTTP/1.1\r\nHost: {authority}\r\n\r\n");
        await stream.WriteAsync(request, cancellationToken);

        // Read exactly the proxy headers and leave the underlying stream ready for MTProto.
        var headers = new byte[8192];
        for (var index = 0; index < headers.Length; index++)
        {
            await stream.ReadExactlyAsync(headers.AsMemory(index, 1), cancellationToken);
            if (!headers.AsSpan(0, index + 1).EndsWith("\r\n\r\n"u8)) continue;

            var lineEnd = Array.IndexOf(headers, (byte)'\r', 0, index + 1);
            var status = Encoding.ASCII.GetString(headers, 0, lineEnd).Split(' ', 3, StringSplitOptions.RemoveEmptyEntries);
            if (status.Length >= 2 && status[0].StartsWith("HTTP/", StringComparison.Ordinal) && int.TryParse(status[1], out var code))
            {
                if (code == 200) return;
                throw new IOException($"Системный HTTP-прокси отклонил подключение к Telegram (HTTP {code}).");
            }
            throw new IOException("Системный HTTP-прокси вернул некорректный ответ на CONNECT.");
        }
        throw new IOException("Системный HTTP-прокси вернул слишком длинный ответ на CONNECT.");
    }
}
