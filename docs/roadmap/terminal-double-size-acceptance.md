# Double-size terminal acceptance

The repeatable client scenario lives in
`fabric/src/clientGametest/java/li/cil/oc2/fabric/gametest/FabricDoubleSizeTerminalTest.java`.
It reuses the equipped Linux computer fixture from `FabricClientAcceptanceTest`.

Run from the repository root with Java 25, Xvfb and a software-capable OpenGL driver:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=true \
  xvfb-run -a -s '-screen 0 1280x720x24' \
  timeout --signal=TERM --kill-after=30s 20m \
  ./gradlew :fabric:26.1.2:runClientGameTest \
  -Pfabric.clientTests=true -Pfabric.terminalTests=true --console=plain
```

Replace `26.1.2` with `26.2` for the second modern render pipeline. Both properties
are required; without `fabric.terminalTests`, the existing general client acceptance
runs instead. Sound is disabled both through OpenAL and Minecraft's master volume.
The scenario creates a disposable world and does not use a personal save.

## Checks

- Boot bundled Linux, log in as root and print ESC #3/#4/#6/#5 through the guest UART.
- Capture paired double-height text, double-width text, colors, underline,
  strikethrough, DEC line drawing and a normal-width line in the world and GUI.
- Move the camera close to the computer screen and position a steady block cursor
  in the last visible column of a doubled row.
- Inspect the already-created block renderer's row cache, change only that row's
  attribute from DWL to DHL top through the client parser, assert unchanged cells,
  a dirty row, replacement of its cached mesh and changed vertex bytes after rendering. This deliberately
  isolates local invalidation: applying a network diff marks all rows dirty and
  would mask a missing attribute-specific dirty mark. Test-only reflection avoids
  exposing private cache fields in the production API.

Screenshots are written to
`fabric/versions/<version>/build/run/clientGameTest/screenshots/`.
The GUI and world crops are compared against reviewed PNGs under
`fabric/src/clientGametest/resources/terminal-pixels/`. A normal run requires the reference and
allows zero RGB differences. Actual crops and magenta diffs are written beside screenshots on failure.
The window is fixed at 854x480, GUI scale 2, FOV 30, gamma 0.0; time is frozen at noon and weather is clear.
This avoids the one-channel lighting drift reproduced before the fixture was fixed.

The non-client erase/scroll edge cases live in `DoubleSizeEraseScrollTest`: ECH, EL 0/1/2, ED 0/1/2,
main/alternative buffers, forward/reverse index, SU/SD and IL/DL within a restricted scroll region,
attribute movement, freshly exposed single-width rows and unaffected outside rows.
ED fully erased rows reset to SWL, including the cursor row when the erase covers its entire visible width;
the newly exposed half receives the current erase background, and partially erased rows keep their size. This follows [VT510 ED](https://vt100.net/docs/vt510-rm/ED.html).
`DoubleSizeCursorTest` additionally checks actual steady-block cursor vertices at the last visible cell
for SWL/DWL/DHL top/bottom, main and alternative buffers, on 26.x.
Run the terminal suites with:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./gradlew \
  :1.21.1:test :26.1.2:test :26.2:test \
  --tests '*vm.terminal*' --console=plain
```

## Acceptance on 2026-11-02

Both Fabric 26.1.2 and 26.2 client scenarios passed in Xvfb with sound disabled.
The GUI and close world screenshots were inspected: the last-column steady block
cursor occupies the rightmost doubled cell, and changing DWL to DHL top changes
both the cached mesh identity and its vertex bytes without changing text cells.
The GUI capture restores DWL after the isolated cache probe, so the normal pattern
remains readable. The six reviewed references cover both modern Fabric pipelines and the legacy
1.21.1 GUI/retained-world GL paths; normal verification passed with zero differing pixels.
They do not establish NeoForge-specific client integration.

Two ED regressions were reproduced before their fixes: ED 0/1 failed to reset
fully erased line rendition (four failures across the main/alternative buffers),
and ED 1 at the final doubled column left the newly exposed half with the old
background (two failures). The corresponding tests remain in the source tree.

After the vttest/mode/decoder follow-up, terminal unit suites passed: 372 on NeoForge 1.21.1,
384 on NeoForge 26.1.2 and 384 on NeoForge 26.2 (1140 total, no failures, errors or skips).
The modern totals include eight exact block-cursor geometry cases per version;
the erase/scroll suite contains 18 cases per version.
Checkstyle main/test and PMD main also passed on all three nodes, with zero reported findings.

## All three pixel paths

```sh
bash scripts/terminal/verify-client-pixels.sh
```

The script runs both modern real-world guest scenarios, then the legacy test client.
For an individual legacy run:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=true \
  xvfb-run -a -s '-screen 0 1280x720x24' \
  timeout --signal=TERM --kill-after=30s 10m \
  ./gradlew :fabric:1.21.1:runTerminalPixelTest -Pfabric.legacyTerminalTests=true --console=plain
```

The legacy client predates Fabric client GameTests. It renders the same deterministic pattern
through the production GUI and retained VertexBuffer world branches in a dedicated screen.
It writes a fresh success marker only after both comparisons; the task removes old markers and
rejects a client that exits without completing. This is a real GL render check, not a placed
legacy block/camera/depth integration test. Its screenshots live in
`fabric/versions/1.21.1/build/run/terminalPixels/`.

`OC2R_TERMINAL_PIXEL_DRAFTS=true` explicitly captures candidates without comparing them. The
verification script rejects this mode; candidate generation never overwrites reviewed resources.
The PNGs, exact profile, font hashes and image hashes are retained with the test resources.
The first normal modern check correctly failed on 556 dark pixels with a one-unit blue-channel
lighting change. After fixing noon/weather/gamma, both modern checks passed without any tolerance.

## Historical screenshot reference

The historical audit names `vt420.png`, but the image is unavailable. The supplied local
`ref/vttest` checkout is the same pinned upstream source used by the repeatable capture tool.
On 2026-11-02 the screenshot-only acceptance note was superseded by those independent cell
references and reviewed OC2R pixel goldens. No direct comparison with the missing image is claimed;
its absence is not a gate for PR-B. The legacy placed-block integration limitation above remains explicit.

PMD follow-up also removed six findings per node: the decoder no longer decrements inside a condition
or overwrites a reset before reading it; DEC line-size application is a separate helper, and both
render loops keep the visible-column bound outside the initializer. Unit and pixel checks were repeated
against those source changes without updating reference images.
