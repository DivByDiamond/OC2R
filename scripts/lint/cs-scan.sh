#!/usr/bin/env bash
# Local Checkstyle runner: same config and tool version as the Gradle build,
# but without compiling, so the import-order feedback loop stays fast.
#
# The Gradle build excludes the gametest package from the lint source sets
# (`exclude("**/gametest/**")` in build.gradle.kts). The local run mirrors that
# by deriving a config with one extra BeforeExecutionExclusionFileFilter, so
# both runs see exactly the same files.
#
# Usage: scripts/lint/cs-scan.sh <label> <source-dir> [<source-dir> ...]
# Writes build/cs-scan/<label>.xml and prints a short summary.
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

label="${1:?usage: cs-scan.sh <label> <dir> [dir ...]}"
shift

cp_file="${CS_CLASSPATH_FILE:-/tmp/opencode/cs_cp.txt}"
if [[ ! -s "$cp_file" ]]; then
    echo "ERROR: classpath cache $cp_file missing; regenerate with:" >&2
    echo "  ./gradlew -I /tmp/opencode/cs.init.gradle :26.1.2:csClasspath" >&2
    exit 1
fi
classpath="$(cat "$cp_file")"

out_dir="build/cs-scan"
mkdir -p "$out_dir"
out="$out_dir/$label.xml"
local_config="$out_dir/checkstyle-local.xml"

python3 - config/checkstyle/checkstyle.xml "$local_config" <<'EOF'
import sys
src = open(sys.argv[1], encoding='utf-8').read()
marker = '<module name="Checker">'
inject = (
    marker + '\n'
    '  <module name="BeforeExecutionExclusionFileFilter">\n'
    '    <property name="fileNamePattern" value=".*[/\\\\]gametest[/\\\\].*"/>\n'
    '  </module>'
)
assert marker in src, 'Checker module not found in the checkstyle config'
open(sys.argv[2], 'w', encoding='utf-8').write(src.replace(marker, inject, 1))
EOF

java -Duser.language=en -Duser.country=US -cp "$classpath" com.puppycrawl.tools.checkstyle.Main \
    -c "$local_config" \
    -f xml -o "$out" "$@" || true

python3 - "$out" <<'EOF'
import sys, xml.etree.ElementTree as ET
t = ET.parse(sys.argv[1]).getroot()
errs = t.findall('.//error')
files = t.findall('.//file')
print(f"{sys.argv[1]}: files={len(files)} errors={len(errs)}")
by = {}
for e in errs:
    key = e.get('source', '').split('/')[-1].replace('Check', '')
    by[key] = by.get(key, 0) + 1
for k, v in sorted(by.items(), key=lambda kv: -kv[1]):
    print(f"  {v:5d}  {k}")
EOF
