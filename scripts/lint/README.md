# Lint tooling (local, not part of the build)

Helper scripts for the Checkstyle `CustomImportOrder` violations that the
multiversion layout (Stonecutter) creates. The Gradle build stays the
authority; these scripts only shorten the feedback loop.

| Script | Purpose |
|---|---|
| `cs-classpath.sh [node]` | Resolves the classpath the local Checkstyle runner needs (the Checkstyle tool plus the compile classpath of the Gradle node) and writes it to `build/cs-scan/cs_cp.txt`. The cache lives inside the repository, so the script works on every checkout. Run it once after a change of the Minecraft version or of the Checkstyle version. |
| `cs-scan.sh [--strict] <label> <dir> [<dir> ...]` | Runs Checkstyle 10.21.0 with `config/checkstyle/checkstyle.xml` over a directory without compiling. Writes `build/cs-scan/<label>.xml` and prints a summary. Same tool version and config as Gradle, so the numbers match the Gradle report exactly. Without `--strict` the script always exits with 0 (it is a report, not a gate); with `--strict` it exits with 1 when a violation was found, which makes it usable in a CI step. |
| `remaining.py --report <xml>...` | Lists the files that still violate a rule, per variant report. |
| `variant-view.py --version 26.1.2 <file>` | Resolves a shared-tree file for one Minecraft version (conditionals + Stonecutter replacements) so a single file can be checked without Gradle. `--check <generated-tree>` compares the result with the Gradle-generated tree. |
| `fix-import-order.py` | Sorts the import blocks of the shared tree so that every variant passes the rule. Handles files whose import region has no `if/else` branch pair; the rest are reported as skipped. |
| `analyse-imports.py` | Prints, per file and per variant, the import order the variant sees and the order Checkstyle expects, so a reordering can be derived instead of guessed. |
| `classify-import-violations.py --report <xml> --variant <dir>` | Splits the remaining import-order violations of a report into the ones that sit inside a Stonecutter conditional block (they need a different position per version) and the plain ones (sortable in place). |
| `check-file.sh <shared-file>` | Checks one shared-tree file against the lint rules of every version: resolves it for 1.21.1, 26.1.2 and 26.2, compares the resolution with the generated tree and runs Checkstyle on each. |
| `spotbugs-report.py <report.xml>...` | Reports the SpotBugs findings that the checked-in baseline does not cover (the actionable list), the duplicate `instanceHash` entries inside the baseline and the baseline entries that no longer occur. It never writes to the baseline: the project fixes a finding or suppresses it at its site with a justification (see commit `f5a4a17b`). |
| `import_order.py` | Shared helper module (not a tool): the Checkstyle import comparator and the import parser that skips `/* ... */` blocks. `fix-import-order.py` and `analyse-imports.py` use it, so the tool that reports and the tool that fixes agree on what "sorted" means. |

## The rule

`config/checkstyle/checkstyle.xml` configures

```xml
<module name="CustomImportOrder">
  <property name="sortImportsInGroupAlphabetically" value="true"/>
  <property name="separateLineBetweenGroups" value="true"/>
  <property name="customImportOrderRules" value="STATIC###THIRD_PARTY_PACKAGE"/>
</module>
```

* Two groups: `STATIC` (static imports) first, then `THIRD_PARTY_PACKAGE`.
  `java.*` and `javax.*` also land in `THIRD_PARTY_PACKAGE` because
  `STANDARD_JAVA_PACKAGE` is not configured, so there is exactly one big
  alphabetical group after the static imports.
* Exactly one empty line between the two groups, no empty line inside a group.
* The comparison is Checkstyle's `compareImports`: split the path on `.`,
  compare the segments pairwise (case sensitive), shorter path first when all
  common segments are equal. It is **not** a plain string comparison:
  `net.minecraft.client.renderer.item.ItemStackRenderState` sorts **after**
  `net.minecraft.client.renderer.MultiBufferSource`? No: segment four is
  `item` against `MultiBufferSource`, and `M` (77) is smaller than `i` (105),
  so `MultiBufferSource` comes first.

## The multiversion problem

The shared tree (`src/main/java`) is the VCS state of Minecraft 1.21.1. The
26.x nodes lint the Stonecutter-resolved trees under
`versions/<v>/build/generated/stonecutter/main/java`. A conditional block
carries a different set of imports per version, and the Stonecutter
replacements rename some of them:

* `net.minecraft.resources.ResourceLocation` -> `net.minecraft.resources.Identifier`
* `net.minecraft.client.gui.GuiGraphics` -> `net.minecraft.client.gui.GuiGraphicsExtractor`

Both renames change the alphabetical position of the import, so an import
block that is sorted in one version can be unsorted in another.

### Conditional block syntax

```java
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
```

* `//? if COND {` opens a block, `//?}` closes it.
* The block is **commented out as a whole** when the condition does not hold:
  the `/*` sits on the first content line and `*///?}` is the closer.
* When the condition holds, the content is plain and the closer is `//?}`.
* `*///?} else {` starts the `else` branch (the negated condition).
* The shared tree is the 1.21.1 rendering, so a `>=26.1` block is commented
  out there and plain in the 26.x trees.

The imports inside a commented-out block are not imports for the linted
version, so every tool of this directory skips them (see
`import_order.visible_imports`).

## Rules for editing

1. Never change the semantics of a conditional block: the same import must
   stay in the same branch, only its position may change.
2. When a block's imports need different positions in different versions,
   split the block into several blocks with the same condition. A block is
   commented out as a whole, so every part needs its own `/*` and `*///?}`.
3. An `if/else` pair must stay adjacent: the `else` branch has to follow its
   `if` branch directly. Such files are not handled by `fix-import-order.py`
   and need manual work.
4. `net.minecraft.resources.ResourceKey` and `net.minecraft.resources.ResourceLocation`
   cannot be sorted in both versions: 1.21.1 wants `ResourceKey` first,
   26.x wants `Identifier` first. There is no order that satisfies both, and
   the Checkstyle rule cannot be met for these two imports in one file.
   `config/checkstyle/checkstyle.xml` suppresses exactly that one import (a
   `SuppressionXpathSingleFilter`), so the rule keeps checking every other
   import of the file.

## Feedback loop

```bash
# 0. once per checkout: resolve the classpath of the local Checkstyle runner
scripts/lint/cs-classpath.sh

# 1. resolve a file for a version and lint it
python3 scripts/lint/variant-view.py --version 26.1.2 src/main/java/.../Foo.java > /tmp/foo-2612.java
bash scripts/lint/cs-scan.sh tmp2612 /tmp/foo-2612.java

# 2. after editing, regenerate the 26.x trees and rescan everything
./gradlew :26.1.2:stonecutterGenerate :26.2:stonecutterGenerate
bash scripts/lint/cs-scan.sh neo1211 src/main/java
bash scripts/lint/cs-scan.sh neo2612 versions/26.1.2/build/generated/stonecutter/main/java
bash scripts/lint/cs-scan.sh neo262 versions/26.2/build/generated/stonecutter/main/java

# 3. as a gate: exit with 1 when a violation was found
bash scripts/lint/cs-scan.sh --strict neo1211 src/main/java
```

`cs-scan.sh` mirrors the Gradle exclusion of the `gametest` package and
reproduces the Gradle numbers exactly.

The import-order suppression of `config/checkstyle/checkstyle.xml` is bound to
the source trees of the project, so it does not apply to the copies that
`check-file.sh` writes into `build/cs-scan/`: that script therefore still
reports the one import that the build suppresses. The Gradle report is the
authority.
