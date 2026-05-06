@echo off
if not exist "%USERPROFILE%\.m2\settings.xml" (
    echo [mirror] ERROR: %USERPROFILE%\.m2\settings.xml not found.
    echo          Create it from mirroring\settings.example.xml and put your Artifactory credentials.
    exit /b 1
)
set MAVEN_OPTS=--add-opens=java.base/java.net=ALL-UNNAMED
mvn -f mirroring\mirror-p2.pom clean package
if errorlevel 1 exit /b %errorlevel%
echo.
echo [mirror] Done. Mirrored p2 repo from Artifactory to mirroring\target\repo\
echo          and packaged it as mirroring\target\com.e1c.edt.ai.repository-^<version^>.zip
