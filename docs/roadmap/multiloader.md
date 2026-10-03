# Roadmap: multiloader, CI and dependency updates

Design: [docs/MULTILOADER.md](../MULTILOADER.md).

## 42. Multiloader (NeoForge and Fabric) and multiversion

Plan of 2026-09-15. Trigger: in 2026 Mojang moved to `YY.drop` versioning (26.1 "Tiny Takeover" in March, 26.2 "Chaos Cubed" in June; `1.21.11` is the last release of the old scheme). Both loaders already support 26.1/26.2.
Target matrix: {1.21.1, 26.1, 26.2} x {NeoForge, Fabric}: 6 targets. Coupling to NeoForge: 236 of about 1050 java files import `net.neoforged.*` directly (registries 40, network.handling 42, capabilities 30, energy 16,
FML/bus the rest), so a platform abstraction is required. After the Stage 2 network/energy (2026-09-28) and capabilities/common-event (2026-10-02) migrations: **171 files** (common 87, client 50, data 12, gametest 9, platform 9, api 4).

Stages:
- [x] **Stage 1: registries** (**merged** as PR #55, 2026-09-27; was `feat/multiloader-stage1`): module `core` (depends only on vanilla Minecraft via NeoForm, no loader) with `li.cil.oc2.platform`:
  `RegistryBridge` (`register`, `registerBlock/Item`, `blocks`, `addItemAlias`, `createRegistry`), `Platform` (ServiceLoader), `BlockHolder`/`ItemHolder`; implementation `NeoForgeRegistryBridge`. Migrated to the bridge:
  `Blocks`, `Items`, `BlockEntities`, `BlockCodecs`, `SoundEvents`, `RecipeSerializers`, `Entities`, `Containers`, `ItemGroup`, `DataComponents`, firmware, block-device-data, providers. Not migrated: `DeviceTypes`, `RegistryUtils` (both still on NeoForge `DeferredRegister`) and the client
  `Manuals`. The jar contains `core` classes; lint, tests and `gameTest` (15/15) are green. Deviation from the spec: sources were NOT moved into `core/`/`neoforge/` (the root project stays the NeoForge module): about 230 NeoForge files
  cannot live in `core` without NeoForge. `runData` fails on `Missing loottable 'oc2r:blocks/speaker'`, reproducible on `work` as well.
- Note: the original stage-1 statement (`core`/`neoforge` Gradle split without behavior changes + registries as a model subsystem) is superseded by the implemented variant above.
- [ ] **Stage 2**: a real `fabric` module + migration of capabilities/network/energy/client events by the stage-1 template.
  - [x] **network** (PR #56, 2026-09-28): `NetworkBridge`/`MessageRegistrar`/`MessageContext` in `core`, `Network.initialize(MessageRegistrar)` replaces direct payload registration, all messages and `MessageUtils` moved off `IPayloadContext`; NeoForge impls (`NeoForgeMessageRegistrar`, `NeoForgeNetworkBridge`) bound from `Main` via ServiceLoader + `RegisterPayloadHandlersEvent`.
  - [x] **energy** (PR #56, 2026-09-28): `EnergyStorage`/`AbstractEnergyStorage`/`EnergyBridge`/`EnergyCapabilityRegistrar` in `core`; all registration/query sites migrated (cables, charger, robots, monitors, projector, gateway, PCI cage, computer, creative energy, tooltips, gametest fixture). Fixed on the way: identity-stable capability wrapper (an unstable wrapper made the device bus re-add devices every scan — a rescan storm that hung `GameTestServer`; identity now comes from `equals`/`hashCode` on the wrapped storage, regression-tested, and the interim static wrapper cache it replaced was itself an unbounded leak) and non-thread-safe `HashSet`/`HashMap` in `DeviceTable`/`AbstractDeviceBusElement` (now `ConcurrentHashMap`-backed).
  - [x] **capabilities** (2026-10-02): `BlockCapability`/`EntityCapability`/`ItemCapability` keys, `CapabilityBridge` (lookups, `BlockCapabilityCache`, invalidation listeners, `invalidateBlock`), `CapabilityRegistrar`, `ItemHandler`/`FluidHandler`/`FluidStack`/`ItemHandlers` in `core`; `NeoForgeCapabilityBridge`/`NeoForgeCapabilityRegistrar`/`NeoForgeCapabilities` map the keys and value types onto NeoForge. All lookups and registrations of device bus element, device, network interface, redstone emitter, terminal user provider, robot, item handler, fluid handler and energy go through `Platform.capabilities()` / `CapabilityRegistrar`; `common/capabilities/Capabilities` holds loader-independent keys (standard ones keep the `neoforge:` ids). Wrappers compare equal by the wrapped object (and lookups unwrap our own wrappers), otherwise the device bus re-adds the device on every scan — the same rescan storm as in the energy migration. Known leftover: `RobotItem.registerCapabilities` (item energy) was never subscribed before this change and is still not wired in.
  - [x] **common game events** (2026-10-02): `Event`/`CommonEvents` in `core` (server about-to-start/started/stopping/stopped, server and level tick, level and chunk load/unload, add-reload-listener); `NeoForgeCommonEvents` fires them from the NeoForge game bus; common listeners (`ServerScheduler`, `ChunkUtils`, `BlobStorage`, `FileSystems`, `RPCItemStackTagFilters`, `Allocator`, `TunnelManager`, `NetworkTunnelDevice`, `InternetManagerImpl`, `RobotEventHandler`, `ForgeEventHandlers`) subscribe through `CommonEventListeners.register()`, called from `Main`. The 17 `RegisterCapabilitiesEvent` subscribers collapsed into `CapabilityProviders.registerAll(registrar)` fired by `NeoForgeCapabilitySetup`.
  - [ ] **DeferredRegister leftovers**: `RegistryUtils` and the client `Manuals` (`DeviceTypes` stays on DeferredRegister deliberately — see below).
  - [x] **environment** (2026-10-03): `PlatformEnvironment` (`isClient`, `isModLoaded`, `configDir`) in `core`, `NeoForgeEnvironment`/`FabricEnvironment`; replaced `FMLLoader.getDist()`, `ModList` and `FMLPaths` in common code, and the `initialize(IEventBus)` leftovers of the registry classes.
  - [x] **client registrations** (2026-10-03): `ClientRegistrar` (renderers, model layers, colour handlers, setup tasks) with `NeoForgeClientRegistrar`/`FabricClientRegistrar`; `ClientEvents` gained `CLIENT_TICK_START`, `RENDER_LEVEL` and `HIDE_HOTBAR`.
  - [x] **item handlers** (2026-10-03): `ItemStackHandler`, `CombinedItemHandler`, `ItemHandlerSlot` and `NbtSerializable` in `core` (same behaviour and NBT format as NeoForge's); robot, computer, disk drive, flash flasher and the menus use them, `NeoForgeItemHandlers` is gone.
  - [x] **menus** (2026-10-03): `MenuBridge` (`createMenuType`, `openMenu` with extra data; Fabric carries the data as a byte array through `ExtendedScreenHandlerType`), `ScreenFactory`, menu screens registered through `ClientRegistrar.registerScreen`; `Main.LoadedLibrary` moved to `NativeLoader`.
  - [ ] **common code on Fabric** (measured 2026-10-03; after the item handler, config and menu steps javac reports 210 error sites in the first pass, previously about 330 unresolved symbols. What remains is structural: NeoForge-patched overrides that do not exist in vanilla (`BlockEntity.onLoad`/`handleUpdateTag`/`onChunkUnloaded`, `Block.rotate`/`getCloneItemStack`/`BlockState.getSoundType` with level arguments, `Item.onItemUseFirst`), about 25 files where common code names `client` classes (`LEGACY_OFFENDERS`), `NeoForgeStreamCodecs`, `InterModComms`, `NewRegistryEvent`, `IClientItemExtensions`/`ModelData`, and the mixins in `common/mixin`, `ModConfigSpec` now resolves through Forge Config API Port and `Dist`/`OnlyIn` through the dist-marker jar; build with `-Pfabric.common=true`). First measurement by compiling all of `src/main/java` minus client/gametest/data/Main in the `fabric` module): about 760 unresolved symbols, in this order of weight: `ModConfigSpec` (config, 10 files, 186 errors; Fabric needs the NeoForge Config API Port or an own spec), `Dist`/`OnlyIn` (57 files; the annotation jar is enough to compile), `ItemStackHandler`/`IItemHandler`/`SlotItemHandler` (14 files, map onto the `ItemHandler` in core), NeoForge-patched vanilla methods (`ServerPlayer.openMenu` with extra data, `BlockState.getSoundType` with level arguments, `ItemStack.set`), `NeoForgeStreamCodecs` (6), `SubscribeEvent`/`EventBusSubscriber` (9), `InterModComms`/JEI/ProjectRed integrations, `NewRegistryEvent` and a few smaller hooks.
  - [ ] **`fabric/` module**: fabric-loom skeleton (official plugin, not Architectury — rationale in [MULTILOADER.md](../MULTILOADER.md)), entrypoint, Fabric implementations of the existing bridges (`RegistryBridge`, `NetworkBridge`/`MessageRegistrar`, `EnergyBridge`/`EnergyCapabilityRegistrar`).
- [ ] **Stage 3**: multiversion 1.21.1/26.1/26.2 through Stonecutter, for both loaders.
- [ ] **Stage 4**: CI/release matrix for 6 targets, CurseForge/Modrinth for each.
- [x] `DeviceTypes` `DeferredRegister` is not bound to the mod bus, so the registration is effectively dead: **fixed** in `fd166127` (2026-09-27) — `initialize(modBus)` called from `Main`; deliberately left on the raw `DeferredRegister` because `DeviceType.REGISTRY` is public API created eagerly by the interface field, so routing it through `createRegistry()` would make a second, disconnected registry.
- [x] `FirmwareRegistry.getKey` returns the registry name instead of the entry key: **fixed** in `fd166127` (2026-09-27) — it ignored its argument, so `FlashMemoryWithExternalDataItem.withFirmware()` persisted a key that `getFirmware()` later resolved to the registry-level location and silently got `null`; now delegates to `REGISTRY.getKey(firmware)`.

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
