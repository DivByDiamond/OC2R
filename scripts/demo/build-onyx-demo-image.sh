#!/usr/bin/env bash
# Build a separate demonstration image; never replace the packaged COW base.
set -euo pipefail
if [ "$#" -lt 3 ] || [ "$#" -gt 4 ]; then
    echo "Usage: $0 <OnyxOS .build/oc2r/manifest.txt> <compiled guest tools directory> <output.img> [rebuilt OnyxApps build directory]" >&2
    exit 2
fi
manifest_path=$(realpath "$1")
guest_tools=$(realpath "$2")
output_path=$(realpath -m "$3")
repo_root=$(cd "$(dirname "$0")/../.." && pwd)
if [ "$output_path" = "$repo_root/src/main/resources/onyxos/onyxfs.img" ]; then
    echo "Refusing to overwrite the immutable base used by existing VM overlays." >&2
    exit 1
fi
: "${ONYX_MKIMAGE:?Set ONYX_MKIMAGE to a built OnyxKernel target/release/mkimage}"
[ -f "$manifest_path" ]
[ -x "$ONYX_MKIMAGE" ]
mkdir -p "$(dirname "$output_path")"
combined_manifest=$(mktemp "${TMPDIR:-/tmp}/oc2r-demo-manifest.XXXXXX")
trap 'rm -f "$combined_manifest"' EXIT
if [ "$#" -eq 4 ]; then
    rebuilt_apps=$(realpath "$4")
    while IFS= read -r manifest_line; do
        case "$manifest_line" in
            file\ */OnyxApps/build/*)
                read -r entry_kind old_host guest_path <<< "$manifest_line"
                rebuilt_host="$rebuilt_apps/$(basename "$old_host")"
                [ -f "$rebuilt_host" ]
                printf 'file %s %s\n' "$rebuilt_host" "$guest_path" >> "$combined_manifest"
                ;;
            *) printf '%s\n' "$manifest_line" >> "$combined_manifest" ;;
        esac
    done < "$manifest_path"
else
    cat "$manifest_path" > "$combined_manifest"
fi
for tool in "$guest_tools"/*.onx; do
    [ -f "$tool" ] || continue
    printf 'file %s /bin/%s\n' "$tool" "$(basename "$tool" .onx)" >> "$combined_manifest"
done
"$ONYX_MKIMAGE" "$combined_manifest" "$output_path"
if [ "$(stat -c %s "$output_path")" -gt 16777216 ]; then
    echo "Demo image exceeds the 16 MiB root capacity." >&2
    exit 1
fi
truncate -s 16M "$output_path"
sha256sum "$output_path"
