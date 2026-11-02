#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
if [[ ${OC2R_TERMINAL_PIXEL_DRAFTS:-false} == true ]]; then
  echo 'Verification requires reviewed goldens; unset OC2R_TERMINAL_PIXEL_DRAFTS.' >&2
  exit 2
fi
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"
export LIBGL_ALWAYS_SOFTWARE=true
export ALSOFT_DRIVERS=null
xvfb-run -a -s '-screen 0 1280x720x24' timeout --signal=TERM --kill-after=30s 20m \
  ./gradlew :fabric:26.1.2:runClientGameTest :fabric:26.2:runClientGameTest \
  -Pfabric.clientTests=true -Pfabric.terminalTests=true --console=plain
xvfb-run -a -s '-screen 0 1280x720x24' timeout --signal=TERM --kill-after=30s 10m \
  ./gradlew :fabric:1.21.1:runTerminalPixelTest -Pfabric.legacyTerminalTests=true --console=plain
