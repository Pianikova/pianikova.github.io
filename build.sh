#!/usr/bin/env bash
set -e
mvn --batch-mode -P g5-v8-dt -P find-bugs,SDK -DbuildNumberPath=/var/lib/nfs-cistore/build/buildNumbers clean verify -Dtycho.localArtifacts=ignore -Dbaseline.skip
echo
echo "[build] Done. Built and verified all modules; artifacts are in repositories/*/target/*.zip"
