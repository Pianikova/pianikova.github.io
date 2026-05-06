@echo off
mvn org.codehaus.gmavenplus:gmavenplus-plugin:1.13.1:execute@generate-readme -N
if errorlevel 1 exit /b %errorlevel%
echo.
echo [readme] Done. Regenerated README.md from README_TEMPLATE.md
