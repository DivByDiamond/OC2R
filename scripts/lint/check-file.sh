#!/usr/bin/env bash
# Check one shared-tree file against the lint rules of every version.
#
# Usage: check-file.sh src/main/java/.../Foo.java
#
# Resolves the file for 1.21.1 (the shared tree itself), 26.1.2 and 26.2 and
# runs Checkstyle on each resolution. When the Gradle-generated tree is
# present, the resolver output is compared with it first: a mismatch means the
# file uses a conditional form the resolver does not model, and its result
# cannot be trusted.
set -uo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

file="${1:?usage: check-file.sh <shared-file>}"
rel="${file#src/main/java/}"

echo "=== $file"
status=0
for version in 1.21.1 26.1.2 26.2; do
    # keep the relative path so that path based Checkstyle suppressions match
    out="build/cs-scan/single/$rel.$version"
    mkdir -p "$(dirname "$out")"

    if [[ "$version" == "1.21.1" ]]; then
        cp "$file" "$out.java"
    else
        python3 scripts/lint/variant-view.py --version "$version" "$file" > "$out.java"
        tree="versions/$version/build/generated/stonecutter/main/java/$rel"
        if [[ -f "$tree" ]]; then
            if ! diff -q <(sed -e '$a\' "$tree") <(sed -e '$a\' "$out.java") >/dev/null; then
                echo "  [$version] NOTE: the resolver output differs from the generated tree;"
                echo "         the file uses a conditional form the resolver does not model."
            fi
        fi
    fi

    result="$(bash scripts/lint/cs-scan.sh "single-$(echo "$rel" | tr '/' '_')-$version" "$out.java" 2>&1 | grep -E "errors=" || true)"
    echo "  [$version] $result"
    if [[ "$result" != *"errors=0"* ]]; then
        status=1
        python3 - "build/cs-scan/single-$(echo "$rel" | tr '/' '_')-$version.xml" <<'EOF'
import sys
import xml.etree.ElementTree as ET
tree = ET.parse(sys.argv[1]).getroot()
for entry in tree.findall('.//file'):
    for error in entry.findall('.//error'):
        print(f"      line {error.get('line')}: {error.get('message')}")
EOF
    fi
done
exit $status
