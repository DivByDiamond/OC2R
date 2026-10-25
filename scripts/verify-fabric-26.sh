#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

# Loom 1.18 requires a Java 25 Gradle daemon. Java 21 remains a toolchain for
# legacy targets, so keep both installed when running the complete matrix.
if ! command -v timeout >/dev/null 2>&1; then
    echo "ERROR: GNU timeout is required to bound Fabric GameTest runs" >&2
    exit 1
fi

./gradlew :fabric:26.1.2:assemble :fabric:26.2:assemble "$@"
python3 scripts/verify-fabric-artifacts.py 26.1.2 26.2
for version in 26.1.2 26.2; do
    bash scripts/run-fabric-gametest.sh "$version" "$@"
    timeout --signal=TERM --kill-after=30s 20m \
        ./gradlew ":fabric:$version:runClientGameTest" -Pfabric.clientTests=true "$@"
done
