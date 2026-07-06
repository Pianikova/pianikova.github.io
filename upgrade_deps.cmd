@echo off
rem Updates Require-Bundle version range upper bounds from a p2 repository.
rem Usage: upgrade_deps.cmd <path-to-p2-repo> [-DryRun]
if "%~1"=="" (
  echo Usage: %~nx0 ^<path-to-p2-repo^> [-DryRun]
  echo Example: %~nx0 D:\Downloads\repo -DryRun
  exit /b 2
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0upgrade_deps.ps1" %*
if errorlevel 1 exit /b %errorlevel%
