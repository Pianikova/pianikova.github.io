#!/usr/bin/env bash
set -e
mvn org.codehaus.gmavenplus:gmavenplus-plugin:1.13.1:execute@generate-readme -N
echo
echo "[readme] Done. Regenerated README.md from README_TEMPLATE.md"
