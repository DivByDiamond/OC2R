# Roadmap: multiloader, CI and dependency updates

Design: [docs/MULTILOADER.md](../MULTILOADER.md).

## 42. Multiloader (NeoForge and Fabric) and multiversion

Plan of 2026-09-15. Trigger: in 2026 Mojang moved to `YY.drop` versioning (26.1 "Tiny Takeover" in March, 26.2 "Chaos Cubed" in June; `1.21.11` is the last release of the old scheme). Both loaders already support 26.1/26.2.
Target matrix: {1.21.1, 26.1, 26.2} x {NeoForge, Fabric}: 6 targets. Coupling to NeoForge: 236 of about 1050 java files import `net.neoforged.*` directly (registries 40, network.handling 42, capabilities 30, energy 16,
FML/bus the rest), so a platform abstraction is required.

Stages:
- [x] **Stage 1: registries** (branch `feat/multiloader-stage1`, implemented, **done on branch, pending merge**): module `core` (depends only on vanilla Minecraft via NeoForm, no loader) with `li.cil.oc2.platform`:
  `RegistryBridge` (`register`, `registerBlock/Item`, `blocks`, `addItemAlias`, `createRegistry`), `Platform` (ServiceLoader), `BlockHolder`/`ItemHolder`; implementation `NeoForgeRegistryBridge`. Migrated to the bridge:
  `Blocks`, `Items`, `BlockEntities`, `BlockCodecs`, `SoundEvents`, `RecipeSerializers`, `Entities`, `Containers`, `ItemGroup`, `DataComponents`, firmware, block-device-data, providers. Not migrated: `DeviceTypes`, `RegistryUtils` (both still on NeoForge `DeferredRegister`) and the client
  `Manuals`. The jar contains `core` classes; lint, tests and `gameTest` (15/15) are green. Deviation from the spec: sources were NOT moved into `core/`/`neoforge/` (the root project stays the NeoForge module): about 230 NeoForge files
  cannot live in `core` without NeoForge. `runData` fails on `Missing loottable 'oc2r:blocks/speaker'`, reproducible on `work` as well.
- Note: the original stage-1 statement (`core`/`neoforge` Gradle split without behavior changes + registries as a model subsystem) is superseded by the implemented variant above.
- [ ] **Stage 2**: a real `fabric` module + migration of capabilities/network/energy/client events by the stage-1 template.
- [ ] **Stage 3**: multiversion 1.21.1/26.1/26.2 through Stonecutter, for both loaders.
- [ ] **Stage 4**: CI/release matrix for 6 targets, CurseForge/Modrinth for each.
- [ ] `DeviceTypes` `DeferredRegister` is not bound to the mod bus, so the registration is effectively dead: investigate.
- [ ] `FirmwareRegistry.getKey` returns the registry name instead of the entry key: investigate and fix.

Toolchain: separate official plugins (`net.neoforged.moddev` for NeoForge, untouched; `fabric-loom` for Fabric), NOT Architectury Loom (rationale in the doc: risk of breaking the configured NeoForge stack for a benefit needed only at stage 2).

Tool `jmove` (`/storage/project/rust/jmove`) was checked for this case: not suitable for bulk moving `src/main/java` to `core/src/main/java` (it refuses to move a file outside the detected source root, and the package does not change here, so plain `git mv`).
It may be useful later for targeted package moves at stages 2+ (built from commit `0216fd6`, the working tree does not currently compile because of an uncommitted WIP fix-engine).

### 41.1 GameTest CI and CD

Modeled on fnuecke/oc2 (self-hosted runners shared). Upstream has a separate gradle subproject `gametest` with about 20 classes; task `gameTest` = `dependsOn(":neoforge:runGameTestServer")`; NeoForge writes JUnit XML
into `build/test-results/gameTest/*.xml`; CI runs `./gradlew gameTest` after `build`, uploads `gametest-results-${os}`, and `test-report.yml` (workflow_run) publishes the JUnit report through `dorny/test-reporter`.

- [x] Task `gameTest` in `build.gradle.kts`: `dependsOn("runGameTestServer")`.
- [x] `src/main/java/li/cil/oc2/gametest/` with 3 starter smoke tests: RegistrationTests (DeviceTypes), RecipeTests (everyModItemIsCraftable, everyRecipeCraftsInCraftingTable), DeviceBusTests (busTracksNeighborLifecycle,
  computerStartsWithoutBootError) + helpers TestSupport, ComputerFixture.
- [x] `.github/workflows/ci-work.yml`: step `./gradlew gameTest` after build, upload artifact `gametest-results`.
- [x] `.github/workflows/test-report.yml` (workflow_run from `ci`), publishing via `dorny/test-reporter@v3` (reporter `java-junit`).
- [ ] Gradually extend the suite for our modules (terminal, network connector, redstone interface) as work on [41.2](upstream.md#412-targeted-fixes-from-the-release) proceeds.

### 41.4 Dependency updates

Checked 2026-09-07.
- [x] ceres `0.0.6` -> `0.0.7` (2026-09-02): sanity check of array sizes on deserialization (> 64 MB rejected); applied in `gradle.properties`, `download-libs.sh`, `libs/`.
- [x] sedna `3.1.0` -> `4.0.1` (`ec49a41`, 2026-09-07): `gradle.properties`, `scripts/download-libs.sh`; `GlobalMemoryRangeAllocator` migrated to `DeviceBus` (`board.getDeviceBus().addDevice/removeDevice`); `compileJava` green.
  Confirmed 2026-09-13 (sedna 4.0.0 release notes): upstream added `Z80Board` next to `R5Board`, so sedna 4.0 supports both RISC-V and Z80 VMs. Adding Z80 as a selectable architecture in oc2r is a separate large feature, out of scope.

## 45. GameTest CI: a real baseline

Done, see PR `ci/gametest-parallel` (2026-09-16). GameTest had never actually run (neither locally nor in CI, a silent exit 0). Fixed: `@GameTestHolder`, own `empty.nbt`, `GameTestResultReporter` (fails on an empty/missing
report, closes vacuous-green), `ci-work.yml` split into `lint`/`test`/`gametest`.

- [ ] Remaining: 9 tests are marked `required = false` (reasons in git/PR history); remove the flag after fixing; run the vttest pages through GameTest layer 2 ([section 44.4](terminal.md#444-vttest-automation)) on top of the live infrastructure.
