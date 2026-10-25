#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
version=${1:?Usage: run-fabric-gametest.sh VERSION [Gradle arguments...]}
shift
case "$version" in
    1.21.1|26.1.2|26.2) ;;
    *) echo "Unsupported Fabric version: $version" >&2; exit 2 ;;
esac

# Container /dev/shm is often only 64 MiB. Prefer it only with at least 2 GiB
# available; otherwise use the runner's temporary storage. mktemp isolates jobs,
# retries and concurrent local runs; Gradle adds the Minecraft version suffix.
base=${RUNNER_TEMP:-${TMPDIR:-/tmp}}
if [[ -d /dev/shm && -w /dev/shm ]]; then
    available=$(df -Pk /dev/shm | awk 'NR == 2 {print $4}') || available=0
    if [[ "$available" =~ ^[0-9]+$ ]] && (( available >= 2097152 )); then
        base=/dev/shm
    fi
fi
run_root=$(mktemp -d "$base/oc2r-fabric-gametest.XXXXXXXX")
preserve_logs() {
    result=$?
    trap - EXIT
    # JUnit stays in build/test-results; keep logs at the standard artifact path.
    for kind in logs crash-reports; do
        destination="fabric/versions/$version/build/run/gameTest/$kind"
        if [[ -d "$run_root/$version/$kind" ]]; then
            if ! mkdir -p "$destination" || ! cp -a "$run_root/$version/$kind/." "$destination/"; then
                if (( result == 0 )); then result=1; fi
            fi
        fi
    done
    # External timeout can finish while a daemon-owned Minecraft child still runs.
    # Match Java FileChannel's POSIX record lock; flock uses a different namespace.
    if python3 - "$run_root/$version/world/session.lock" <<'PYLOCK'
import fcntl
from pathlib import Path
import sys

lock = Path(sys.argv[1])
try:
    with lock.open("r+b") as stream:
        fcntl.lockf(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
except FileNotFoundError:
    pass
except OSError as error:
    print(f"Cannot safely remove GameTest world: {error}", file=sys.stderr)
    sys.exit(1)
PYLOCK
    then
        rm -rf -- "$run_root"
    else
        echo "WARNING: retaining Fabric GameTest world while its lock is held or cannot be checked: $run_root" >&2
        if (( result == 0 )); then result=1; fi
    fi
    exit "$result"
}
trap preserve_logs EXIT
printf 'Fabric %s disposable run directory: %s\n' "$version" "$run_root"
timeout --signal=TERM --kill-after=30s 12m \
    ./gradlew ":fabric:$version:runGameTest" "$@" \
    "-Pfabric.gameTestRunDir=$run_root"
