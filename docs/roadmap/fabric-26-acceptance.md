# Fabric 26.x acceptance

Work branch: `feat/fabric-26x`, based on `595774fc`.

This page tracks the Fabric 26.1.2/26.2 port. Implementation and acceptance are
separate: a compiling target is not yet a verified release target.

## Build layout

- Gradle 9.7.0 and Loom 1.18.2 require a Java 25 Gradle daemon. Keep Java 21
  installed for Minecraft 1.21.1 compilation and tooling.
- Stonecutter retains Minecraft 1.21.1 as the VCS source version. Fabric now has
  nodes for 1.21.1, 26.1.2 and 26.2. The legacy node uses remapping Loom; 26.x
  uses the non-remapping plugin and official names.
- Modern Fabric compiles the matching processed shared/core sources, while
  keeping its own platform implementations. It reuses the matching vanilla
  resource conversion without packaging NeoForge metadata or service entries.
- Fabric 26.1.2 uses API 0.155.3; 26.2 uses API 0.161.0. Both use Loader 0.19.5
  and Team Reborn Energy 5.0.0. Exact artifacts and hashes are tracked.

## Acceptance checklist

- [x] Versioned Gradle nodes and modern dependencies.
- [x] Shared vanilla NBT conversion and block-entity construction seams.
- [x] Fabric server bridge/mixin implementation.
- [x] Client GUI, models, robot and projector implementation.
- [x] Compile and assemble all six supported targets.
- [x] Run every registered server GameTest on all three Fabric versions.
- [x] Run the existing NeoForge unit/GameTest regression matrix.
- [x] Run modern Fabric client acceptance on both targets.
- [x] Pass relevant static analysis and workflow validation.
- [x] Inspect packaged metadata, resources and dependency contents.

## Verification commands

Run Gradle on Java 25 with Java 21 available as a toolchain:

```bash
bash scripts/verify-fabric-26.sh --console=plain
bash scripts/verify-stage3.sh --console=plain
```

The dedicated server test task deletes stale JUnit output, checks the world
session lock before clearing its disposable world, and requires exactly the
number of annotated test methods in the compiled test classes. A missing,
empty, partial, skipped or failing report fails the task. Reports live under
`fabric/versions/<version>/build/test-results/gameTest/results.xml`.

Client acceptance is opt-in (`-Pfabric.clientTests=true`) so dedicated server
verification does not compile client-only test dependencies. It uses its own
world and real bundled Linux guest output. Preserve its screenshots and logs
as local verification artifacts; do not ship acceptance classes in mod JARs.

## CI and publishing

The Fabric 26.x CI matrix covers both versions separately, including assembly,
static analysis, server GameTests and client acceptance. Java 25 runs Gradle;
Java 21 remains installed for legacy tooling. JUnit reports and diagnostic logs
are separate artifacts. The aggregate `ci` check includes the new matrix.

Release assembly requires all six artifacts before uploading. CurseForge uses
the existing Fabric project variable for all Fabric game versions; Modrinth
receives separate game-version/loader entries. No release is created by local
acceptance work.

## Related work

The [upstream review](upstream-2026-10-23.md) covers the reference update to
`6e94a86`. Its lifecycle/proxy candidates and Sedna 5.3.0 are kept separate from
this port.

## Recorded results — 2026-10-23

All six assemblies passed. NeoForge main unit suites passed 543 tests on
1.21.1 and 544 each on 26.1.2/26.2; core passed 17 per version (1,682 total,
zero failures/errors/skips). The two added modern unit cases exercise the
lossless whole-map NBT bridge.

Both modern Fabric dedicated servers passed all **67 OC2R tests** each, plus
vanilla's `minecraft:always_pass`, and completed shutdown naturally. Fresh
JUnit reports contain no failures/errors/skips. Repeated local verification
used explicitly configured disposable worlds under `/dev/shm`; the default
on-disk run location remains unchanged. The first on-disk attempt had a test
fixture accessor error and was stopped during slow region-file flushing; it
is not an accepted run. Its corrected repeat is the evidence above.

## Recorded final verification — 2026-10-24

Fabric server GameTests passed on all three targets: 66 on 1.21.1 and 67 each
on 26.1.2/26.2, with no failures, errors or skips. Modern reports also include
vanilla's `minecraft:always_pass`. NeoForge regression passed 1,682 unit tests
and 118 GameTests across its three versions, with no failures or errors.

Modern client acceptance passed on both versions. Each run booted the bundled
Linux guest, rendered the GUI/model/terminal/robot/projector paths, saved and
restored the VM, and produced 15 screenshots. The monitor-distance fixture now
waits for both a red upper pixel and a white lower pixel before capture, so it
does not accept a partially updated framebuffer. At a fixed two-block pose,
three repeated display crops were identical on 26.1.2 and differed by at most
four pixels on 26.2 (23,104 pixels compared). Captures show the full checker on
both versions, with no fade.

All six release assemblies passed; after the monitor sampler change, both
modern jars were rebuilt and passed the artifact-content checker. Checkstyle,
PMD and SpotBugs passed for the three Fabric targets. The changed monitor
renderer compiled on all three versions. All modified workflow files parsed as
YAML. Client acceptance used a private Xvfb display with sound disabled; logs
and screenshots remain under `/tmp/oc2r-fabric26-client-*` and the local
`fabric/versions/*/build/run/clientGameTest/screenshots` directories.
