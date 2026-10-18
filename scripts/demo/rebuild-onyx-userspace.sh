#!/usr/bin/env bash
# The compiler must have both shipped OnyxCC/libonyxc patches applied.
set -euo pipefail
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <isolated OnyxApps checkout> <fixed onyxcc executable> <guest tools output directory>" >&2
    exit 2
fi
apps_checkout=$(realpath "$1")
compiler_path=$(realpath "$2")
tools_output=$(realpath -m "$3")
repo_root=$(cd "$(dirname "$0")/../.." && pwd)
mkdir -p "$tools_output"
# Force rebuilding: the upstream Makefile does not track compiler/libc changes.
make -B -C "$apps_checkout" ONYXCC="$compiler_path"
# The kernel permits creation of new files only to root-ring applications.
# Fetching a response must be able to create its output file.
"$compiler_path" --ring1 -I "$apps_checkout/apps/ohttp" -o "$apps_checkout/build/ohttp.onx" \
    "$apps_checkout/apps/ohttp/http/response.c" "$apps_checkout/apps/ohttp/net/connect.c" \
    "$apps_checkout/apps/ohttp/net/request.c" "$apps_checkout/apps/ohttp/main.c"
for tool in netcfg udpdemo udpserve tcpdemo tcpserve tcpstream; do
    "$compiler_path" --ring1 -I "$(dirname "$compiler_path")/include/sys" -I "$repo_root/scripts/demo/guest" \
        -o "$tools_output/$tool.onx" "$repo_root/scripts/demo/guest/$tool.c"
done
"$compiler_path" --ring1 -I "$(dirname "$compiler_path")/include/sys" -I "$repo_root/scripts/demo/guest" \
    -o "$tools_output/oc2ctl.onx" "$repo_root/scripts/demo/guest/oc2ctl.c" \
    "$repo_root/scripts/demo/guest/oc2rpc.c"
