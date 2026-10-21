# Lint tooling (local, not part of the build)

Helper scripts for the Checkstyle `CustomImportOrder` violations that the
multiversion layout (Stonecutter) creates. The Gradle build stays the
authority; these scripts only shorten the feedback loop.

| Script | Purpose |
|---|---|
| `cs-scan.sh <label> <dir>` | Runs Checkstyle 10.21.0 with `config/checkstyle/checkstyle.xml` over a directory without compiling. Writes `build/cs-scan/<label>.xml` and prints a summary. Same tool version and config as Gradle, so the numbers match the Gradle report exactly. |
| `remaining.py --report <xml>...` | Lists the files that still violate a rule, per variant report. |
| `variant-view.py --version 26.1.2 <file>` | Resolves a shared-tree file for one Minecraft version (conditionals + Stonecutter replacements) so a single file can be checked without Gradle. `--check <generated-tree>` compares the result with the Gradle-generated tree. |
| `fix-import-order.py` | Sorts the import blocks of the shared tree so that every variant passes the rule. Handles files whose import region has no `if/else` branch pair; the rest are reported as skipped. |

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

## Feedback loop

```bash
# 1. resolve a file for a version and lint it
python3 scripts/lint/variant-view.py --version 26.1.2 src/main/java/.../Foo.java > /tmp/foo-2612.java
bash scripts/lint/cs-scan.sh tmp2612 /tmp/foo-2612.java

# 2. after editing, regenerate the 26.x trees and rescan everything
./gradlew :26.1.2:stonecutterGenerate :26.2:stonecutterGenerate
bash scripts/lint/cs-scan.sh neo1211 src/main/java
bash scripts/lint/cs-scan.sh neo2612 versions/26.1.2/build/generated/stonecutter/main/java
bash scripts/lint/cs-scan.sh neo262 versions/26.2/build/generated/stonecutter/main/java
```

`cs-scan.sh` mirrors the Gradle exclusion of the `gametest` package and
reproduces the Gradle numbers exactly.
