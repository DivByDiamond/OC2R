#!/usr/bin/env bash
# Real RV64 userspace integration without starting Minecraft or Gradle.
set -euo pipefail
if [ "$#" -ne 5 ]; then
  echo "Usage: $0 <Sedna dependency classpath> <firmware.bin> <kernel.bin> <onyxfs.img> <output-directory>" >&2
  exit 2
fi
sedna_classpath=$1
firmware=$(realpath "$2")
kernel_image=$(realpath "$3")
root_image=$(realpath "$4")
output_dir=$(realpath -m "$5")
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
mkdir -p "$output_dir"
# The endpoints of the demo (the LAN pair of the two guests) live in the shared
# configuration; sourcing it exports the OC2R_DEMO_* variables into the
# environment of the harness below.
# shellcheck source=scripts/demo/demo-endpoints.sh
. "$repo_root/scripts/demo/demo-endpoints.sh"
javac -cp "$sedna_classpath" -d "$output_dir" "$repo_root/scripts/demo/qa/NetworkPair.java"
java -Xmx1G -Dqa.output="$output_dir" -cp "$output_dir:$sedna_classpath" NetworkPair "$firmware" "$kernel_image" "$root_image"
