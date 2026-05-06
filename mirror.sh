#!/usr/bin/env bash
set -e
if [ ! -f "$HOME/.m2/settings.xml" ]; then
    echo "[mirror] ERROR: $HOME/.m2/settings.xml not found."
    echo "         Create it from mirroring/settings.example.xml and put your Artifactory credentials."
    exit 1
fi
export MAVEN_OPTS="--add-opens=java.base/java.net=ALL-UNNAMED"
mvn -f mirroring/mirror-p2.pom clean package
echo
echo "[mirror] Done. Mirrored p2 repo from Artifactory to mirroring/target/repo/"
echo "         and packaged it as mirroring/target/com.e1c.edt.ai.repository-<version>.zip"
