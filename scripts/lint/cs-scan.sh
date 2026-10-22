#!/usr/bin/env bash
# Local Checkstyle runner: same config and tool version as the Gradle build,
# but without compiling, so the import-order feedback loop stays fast.
#
# The Gradle build excludes the gametest package from the lint source sets
# (`exclude("**/gametest/**")` in build.gradle.kts). The local run mirrors that
# by deriving a config with one extra BeforeExecutionExclusionFileFilter, so
# both runs see exactly the same files.
#
# The classpath comes from build/cs-scan/cs_cp.txt, which scripts/lint/cs-classpath.sh
# writes; it is resolved by Gradle and kept inside the repository, so the script
# works on every checkout.
#
# Usage: scripts/lint/cs-scan.sh [--strict] <label> <source-dir> [<source-dir> ...]
# Writes build/cs-scan/<label>.xml and prints a summary.
# Without --strict the script always succeeds (it is a report, not a gate);
# with --strict it exits with 1 when Checkstyle found a violation.
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

strict=0
if [[ "${1:-}" == "--strict" ]]; then
    strict=1
    shift
fi

label="${1:?usage: cs-scan.sh [--strict] <label> <dir> [dir ...]}"
shift

out_dir="build/cs-scan"
classpath_file="${CS_CLASSPATH_FILE:-$out_dir/cs_cp.txt}"
if [[ ! -s "$classpath_file" ]]; then
    echo "ERROR: classpath cache $classpath_file missing; regenerate it with:" >&2
    echo "  scripts/lint/cs-classpath.sh" >&2
    exit 1
fi
classpath="$(cat "$classpath_file")"

mkdir -p "$out_dir"
out="$out_dir/$label.xml"
count_file="$out_dir/$label.count"
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

# Checkstyle exits with the number of violations; the report is written either way.
java_status=0
java -Duser.language=en -Duser.country=US -cp "$classpath" com.puppycrawl.tools.checkstyle.Main \
    -c "$local_config" \
    -f xml -o "$out" "$@" || java_status=$?

python3 - "$out" "$count_file" <<'EOF'
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
with open(sys.argv[2], 'w', encoding='utf-8') as handle:
    handle.write(str(len(errs)))
EOF

errors="$(cat "$count_file")"
if [[ "$strict" -eq 1 && "$errors" -gt 0 ]]; then
    echo "strict mode: $errors violation(s) found" >&2
    exit 1
fi
if [[ "$errors" -gt 0 && "$java_status" -eq 0 ]]; then
    echo "note: Checkstyle exited with $java_status but reported $errors violation(s)" >&2
fi
exit 0
