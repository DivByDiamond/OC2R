#!/usr/bin/env bash
# Build the classpath the local Checkstyle runner needs.
#
# `cs-scan.sh` runs Checkstyle directly (without compiling the project), so it
# needs the Checkstyle tool itself plus the compile classpath of the node whose
# sources are scanned. Both are resolved by Gradle, so the classpath is written
# into the repository (build/cs-scan/, which is gitignored) instead of into a
# file outside of it -- otherwise the script only works on the machine that
# created the cache.
#
# Usage: scripts/lint/cs-classpath.sh [node]
#   node  the Gradle node to take the compile classpath from (default :26.1.2)
#
# The result is build/cs-scan/cs_cp.txt; cs-scan.sh picks it up from there.
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

node="${1:-:26.1.2}"
out_dir="build/cs-scan"
init_script="$out_dir/cs.init.gradle"
classpath_file="$out_dir/cs_cp.txt"

mkdir -p "$out_dir"

cat > "$init_script" <<'EOF'
// Init script for scripts/lint/cs-classpath.sh: writes the classpath that the
// local Checkstyle runner needs (the Checkstyle tool plus the compile
// classpath of the node) into build/cs-scan/cs_cp.txt.
allprojects {
    tasks.register("csClasspath") {
        doLast {
            def parts = []
            ["checkstyle", "compileClasspath"].each { name ->
                def configuration = configurations.findByName(name)
                if (configuration != null) {
                    parts.addAll(configuration.files)
                }
            }
            // The Checkstyle command line (com.puppycrawl.tools.checkstyle.Main) needs
            // commons-logging; the Gradle plugin does not, because it calls the Checkstyle
            // API directly. The artifact is part of the project's dependency verification.
            def logging = configurations.detachedConfiguration(
                dependencies.create("commons-logging:commons-logging:1.2"))
            parts.addAll(logging.files)
            def target = new File(rootProject.projectDir, "build/cs-scan/cs_cp.txt")
            target.parentFile.mkdirs()
            target.text = parts.collect { it.absolutePath }.join(File.pathSeparator)
            println "classpath written to ${target}"
        }
    }
}
EOF

./gradlew -I "$init_script" "$node:csClasspath" --console=plain -q

if [[ ! -s "$classpath_file" ]]; then
    echo "ERROR: $classpath_file was not written" >&2
    exit 1
fi

echo "classpath cache: $classpath_file ($(tr ':' '\n' < "$classpath_file" | wc -l) entries)"
