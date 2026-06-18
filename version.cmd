@echo off
if "%~2"=="" (
  echo Usage: version.cmd ^<old_version^> ^<new_version^>
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0version.ps1" %*
if errorlevel 1 exit /b %errorlevel%
