#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

# Keep every task qualified: the root project is Stonecutter's controller.
# Fabric currently supports 1.21.1; the 26.x nodes are NeoForge-only.
# Run datagen in its own invocation: processResources is also a prerequisite of
# datagen and must run again afterwards to package the newly generated files.
./gradlew \
    :26.1.2:runClientData :26.1.2:runData \
    :26.2:runClientData :26.2:runData \
    "$@"
python3 scripts/verify-datagen.py

exec ./gradlew \
    :1.21.1:assemble :1.21.1:test \
    :26.1.2:assemble :26.1.2:test \
    :26.2:assemble :26.2:test \
    :fabric:1.21.1:assemble \
    :1.21.1:gameTest \
    :26.1.2:gameTest \
    :26.2:gameTest \
    :fabric:1.21.1:runGameTest \
    "$@"
