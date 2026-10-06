param(
    [string]$ChannelId = '@gattavasis'
)

$ErrorActionPreference = 'Stop'
$secureToken = Read-Host 'Telegram bot token' -AsSecureString
$botToken = [Net.NetworkCredential]::new('', $secureToken).Password

function Invoke-BotMethod {
    param([string]$Method, [hashtable]$Body = @{})

    for ($attempt = 1; $attempt -le 4; $attempt++) {
        try {
            return Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$botToken/$Method" -Body $Body -TimeoutSec 30
        }
        catch {
            $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
            if ($attempt -eq 4 -or $status -notin @(0, 502, 503, 504)) {
                throw "Telegram $Method failed (HTTP $status). The token was not printed."
            }
            Start-Sleep -Seconds 2
        }
    }
}

try {
    $me = (Invoke-BotMethod -Method 'getMe').result
    $membership = (Invoke-BotMethod -Method 'getChatMember' -Body @{ chat_id = $ChannelId; user_id = $me.id }).result
    Write-Output "bot=@$($me.username) id=$($me.id)"
    Write-Output "channel_id=$ChannelId status=$($membership.status)"
}
finally {
    Remove-Variable botToken, secureToken -ErrorAction SilentlyContinue
}
