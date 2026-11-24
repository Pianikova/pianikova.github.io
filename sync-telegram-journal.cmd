@echo off
setlocal EnableExtensions DisableDelayedExpansion

pushd "%~dp0" || exit /b 1

if not defined PIANIKOVA_TELEGRAM_API_HASH echo Get your Telegram API credentials at https://my.telegram.org/ under API development tools.
dotnet run --project build -- telegram-journal-sync
set "syncExitCode=%ERRORLEVEL%"

popd
echo(
pause
exit /b %syncExitCode%
