#!/usr/bin/env bash
set -euo pipefail
compiler_root=${1:?Usage: build-guest-tools.sh ONYX_COMPILER_DIRECTORY [OUTPUT_DIRECTORY]}
output_dir=${2:-/tmp/oc2r-guest-tools}
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
mkdir -p "$output_dir"
for tool in netcfg udpdemo tcpdemo tcpserve udpserve tcpstream; do
  "$compiler_root/onyxcc" --ring1 -I "$compiler_root/include/sys" -I "$repo_root/scripts/demo/guest" \
    -o "$output_dir/$tool.onx" "$repo_root/scripts/demo/guest/$tool.c"
done
