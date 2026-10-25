#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

# Keep every task qualified: the root project is Stonecutter's controller.
# This is the original four-target baseline; verify-fabric-26.sh adds modern Fabric.
# Run datagen in its own invocation: processResources is also a prerequisite of
# datagen and must run again afterwards to package the newly generated files.
./gradlew \
    :26.1.2:runClientData :26.1.2:runData \
    :26.2:runClientData :26.2:runData \
    "$@"
python3 scripts/verify-datagen.py

./gradlew \
    :1.21.1:assemble :1.21.1:test :core:1.21.1:test \
    :26.1.2:assemble :26.1.2:test :core:26.1.2:test \
    :26.2:assemble :26.2:test :core:26.2:test \
    :fabric:1.21.1:assemble \
    :1.21.1:gameTest \
    :26.1.2:gameTest \
    :26.2:gameTest \
    "$@"

# Fabric's launcher has no reliable upper bound when a server tick stalls in a
# GameTest batch. Give it a hard limit so CI/local verification reports a failure
# instead of leaving a Gradle daemon and GameTestServer alive indefinitely.
if ! command -v timeout >/dev/null 2>&1; then
    echo "ERROR: GNU timeout is required to bound the Fabric GameTest run" >&2
    exit 1
fi
bash scripts/run-fabric-gametest.sh 1.21.1 "$@"
