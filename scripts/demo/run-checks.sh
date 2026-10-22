#!/usr/bin/env bash
# Local and CI entry point for the demo source checks.
#
# The same script runs locally and in the `demo-scripts` CI job, so a green CI
# run means exactly what a local run means:
#
#   bash scripts/demo/run-checks.sh all        # every check
#   bash scripts/demo/run-checks.sh shell      # bash -n over the demo shell scripts
#   bash scripts/demo/run-checks.sh c          # syntax-only compile of the C sources
#   bash scripts/demo/run-checks.sh python     # the guest RPC test
#   bash scripts/demo/run-checks.sh java       # compile the demo Java harnesses
#   bash scripts/demo/run-checks.sh endpoints  # the hardcoded-endpoint gate
#
# The checks never start Minecraft, never talk to the network and never need
# the Onyx toolchain: they parse, compile and unit-test the demo sources with
# the host toolchain (bash, a C compiler, python3 and javac).
set -euo pipefail

demo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$demo_dir/../.." && pwd)

# The guest tools are built by OnyxCC against the Onyx headers. CI has no
# Onyx toolchain, so the syntax check uses the stubs in tests/onyx-stubs.
stubs="$demo_dir/tests/onyx-stubs"

# A pinned Gson release for the one demo harness that needs it
# (RpcTransportProbe parses the RPC replies with Gson's JsonParser).
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"

check_shell() {
    local script
    mapfile -t scripts < <(find "$demo_dir" -type f -name '*.sh' | sort)
    if [ "${#scripts[@]}" -eq 0 ]; then
        echo "run-checks: no shell scripts found under $demo_dir" >&2
        return 1
    fi
    for script in "${scripts[@]}"; do
        bash -n "$script"
        echo "run-checks: bash -n ok: ${script#"$repo_root"/}"
    done
}

check_c() {
    local compiler=""
    local candidate
    for candidate in cc gcc clang; do
        if command -v "$candidate" >/dev/null 2>&1; then
            compiler=$candidate
            break
        fi
    done
    if [ -z "$compiler" ]; then
        echo "run-checks: no C compiler (cc, gcc, clang) found -- cannot check the C sources" >&2
        return 1
    fi
    echo "run-checks: C compiler: $("$compiler" --version | head -1)"
    # The guest tools: OnyxCC targets, parsed with the Onyx header stubs.
    "$compiler" -std=gnu11 -fsyntax-only -Wall \
        -I "$stubs" -I "$demo_dir/guest" \
        "$demo_dir"/guest/*.c
    echo "run-checks: guest C sources parse"
    # The host-portable sources: the RPC library, the guest control tool and
    # the test fixtures. They are compiled for the host as well.
    "$compiler" -std=gnu11 -fsyntax-only -Wall \
        -I "$demo_dir/guest" \
        "$demo_dir"/guest/oc2rpc.c "$demo_dir"/guest/oc2ctl.c \
        "$demo_dir"/tests/rpc_host_test.c "$demo_dir"/tests/inventory_precondition_mock.c
    "$compiler" -std=gnu11 -fsyntax-only -Wall \
        -I "$stubs" \
        "$demo_dir"/tests/ohttp_peer_eof.c \
        "$demo_dir"/tests/onyxcc_large_member.c \
        "$demo_dir"/tests/onyx_stdio_first_puts.c
    echo "run-checks: host C sources parse"
}

check_python() {
    if ! command -v python3 >/dev/null 2>&1; then
        echo "run-checks: python3 not found -- cannot run the guest RPC test" >&2
        return 1
    fi
    if ! command -v cc >/dev/null 2>&1; then
        echo "run-checks: cc not found -- the guest RPC test compiles the C sources with cc" >&2
        return 1
    fi
    # The test compiles the guest RPC library for the host and drives it over a
    # pseudo terminal; it needs a C compiler but neither Minecraft nor network.
    python3 "$demo_dir/tests/test_rpc.py"
}

check_java() {
    if ! command -v javac >/dev/null 2>&1; then
        echo "run-checks: javac not found -- cannot compile the demo Java harnesses" >&2
        return 1
    fi
    local sedna_version sedna_jar classpath output
    sedna_version=$(sed -n 's/^sedna_version=//p' "$repo_root/gradle.properties" | head -1)
    sedna_jar="$repo_root/libs/li/cil/sedna/sedna/$sedna_version/sedna-$sedna_version.jar"
    if [ ! -f "$sedna_jar" ]; then
        echo "run-checks: missing $sedna_jar -- run scripts/download-libs.sh first" >&2
        return 1
    fi
    output=$(mktemp -d "${TMPDIR:-/tmp}/oc2r-demo-javac.XXXXXX")
    classpath="$sedna_jar"
    javac -Xlint:all,-classfile -d "$output" -cp "$classpath" "$demo_dir/qa/NetworkPair.java"
    echo "run-checks: NetworkPair.java compiles against the Sedna runtime"
    # RpcTransportProbe additionally needs Gson, which Minecraft provides at
    # runtime. Use the copy from the Gradle cache when it is there, otherwise
    # fetch the pinned release; without it the harness is only skipped.
    local gson_jar="" gson_cache
    gson_cache="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/com.google.code.gson/gson"
    # The Gradle cache does not exist in a job that never runs Gradle (the demo CI job does not),
    # and `find` on a missing directory exits non-zero, which `set -e` would turn into a failure.
    if [ -d "$gson_cache" ]; then
        gson_jar=$(find "$gson_cache" -name 'gson-*.jar' ! -name '*-sources.jar' | sort -V | tail -1)
    fi
    if [ -z "$gson_jar" ] && command -v curl >/dev/null 2>&1; then
        if curl -fsSL --retry 3 --retry-delay 2 --connect-timeout 10 \
            -o "$output/gson.jar" "$GSON_URL"; then
            gson_jar="$output/gson.jar"
        fi
    fi
    if [ -z "$gson_jar" ]; then
        echo "run-checks: WARNING -- Gson is not available, skipping RpcTransportProbe.java" >&2
        rm -rf "$output"
        return 0
    fi
    javac -Xlint:all,-classfile -d "$output" -cp "$classpath:$gson_jar" "$demo_dir/tests/RpcTransportProbe.java"
    echo "run-checks: RpcTransportProbe.java compiles against the Sedna runtime and Gson"
    rm -rf "$output"
}

check_endpoints() {
    bash "$demo_dir/check-endpoints.sh"
}

case "${1:-all}" in
    shell) check_shell ;;
    c) check_c ;;
    python) check_python ;;
    java) check_java ;;
    endpoints) check_endpoints ;;
    all)
        check_shell
        check_c
        check_python
        check_java
        check_endpoints
        ;;
    *)
        echo "Usage: $0 [all|shell|c|python|java|endpoints]" >&2
        exit 2
        ;;
esac
