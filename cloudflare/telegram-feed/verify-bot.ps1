param(
    [string]$ChannelId = '@gattavasis',
    [string]$ExpectedWebhookUrl = 'https://pianikova-telegram-feed.nikolay-pyanikov.workers.dev/telegram/webhook'
)

$ErrorActionPreference = 'Stop'
$secureToken = Read-Host 'Telegram bot token' -AsSecureString
$botToken = [Net.NetworkCredential]::new('', $secureToken).Password
if ([string]::IsNullOrWhiteSpace($botToken)) {
    throw 'Telegram bot token is empty.'
}

function Invoke-BotMethod {
    param([string]$Method, [hashtable]$Body = @{})

    for ($attempt = 1; $attempt -le 4; $attempt++) {
        try {
            return Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/$Method" -Body $Body -TimeoutSec 30
        }
        catch {
            $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
            if ($attempt -eq 4 -or $status -notin @(0, 502, 503, 504)) {
                $description = 'No error description returned.'
                $errorText = $_.ErrorDetails.Message
                if (-not $errorText -and $_.Exception.Response -is [System.Net.HttpWebResponse]) {
                    try {
                        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                        try { $errorText = $reader.ReadToEnd() }
                        finally { $reader.Dispose() }
                    }
                    catch {
                        # The response body may already have been consumed.
                    }
                }
                if ($errorText) {
                    try {
                        $errorBody = $errorText | ConvertFrom-Json -ErrorAction Stop
                        if ($errorBody.description) {
                            $description = [string]$errorBody.description
                        }
                    }
                    catch {
                        # Keep the generic message if Telegram did not return JSON.
                    }
                }
                throw "Telegram $Method failed (HTTP $status): $($description.Replace($botToken, '[redacted]'))"
            }
            Start-Sleep -Seconds 2
        }
    }
}

try {
    $me = (Invoke-BotMethod -Method 'getMe').result
    Write-Output "bot=@$($me.username) id=$($me.id)"
    $chat = (Invoke-BotMethod -Method 'getChat' -Body @{ chat_id = $ChannelId }).result
    Write-Output "channel_id=$($chat.id) type=$($chat.type)"

    $webhook = (Invoke-BotMethod -Method 'getWebhookInfo').result
    Write-Output "webhook_configured=$([bool]$webhook.url) webhook_matches_worker=$($webhook.url -eq $ExpectedWebhookUrl)"
    Write-Output "allowed_updates=$($webhook.allowed_updates -join ',') pending_updates=$($webhook.pending_update_count)"
    if ($webhook.last_error_message) {
        Write-Output "webhook_last_error=$($webhook.last_error_message.Replace($botToken, '[redacted]'))"
    }

    try {
        $membership = (Invoke-BotMethod -Method 'getChatMember' -Body @{ chat_id = $chat.id; user_id = $me.id }).result
        Write-Output "status=$($membership.status)"
    }
    catch {
        Write-Output "membership_error=$($_.Exception.Message)"
    }
}
finally {
    Remove-Variable botToken, secureToken -ErrorAction SilentlyContinue
}
