# Feasibility spike: NeoForge side on Minecraft 26.1 / 26.2

Spike of 2026-10-03 for roadmap [item 42, stages 3 and 4](multiloader.md) (multiversion 1.21.1 / 26.1 / 26.2 through Stonecutter). Scope: NeoForge only (the Fabric side is investigated separately). Nothing here was merged:
all experiments ran in a scratch copy of the worktree (`scratchpad/s`), only this document is committed.

## 1. Verdict

- The toolchain is obtainable and works in the sandbox: JDK 25 (Temurin 25.0.4.1, downloaded from the Adoptium GitHub release that foojay points to), ModDevGradle 2.0.148, NeoForge `26.1.2.114`, NeoForm `26.1.2-1`.
- `core` (our loader-independent module) compiles against 26.1.2 and 26.2 after 4 distinct fixes in 2 files (17 lines).
- The NeoForge module does not: **1548 unique compile errors in 253 of 1105 files** after the two cheap mechanical renames (`ResourceLocation` -> `Identifier`, drop `@MethodsReturnNonnullByDefault`). That number is a lower bound: javac stops
  reporting in files whose imports no longer resolve, and Mojang code that compiles but changed behaviour is invisible.
- The jump is not "26.1 vs 1.21.1": it covers 1.21.2 ... 1.21.11 and 26.1 in one go (item components, rendering pipeline rewrite, item models as data, `ValueInput`/`ValueOutput` serialization, new capability/transfer API in NeoForge,
  GameTest rework). The 26.1 -> 26.2 step is small by comparison (class diff 300 removed / 570 added of about 10.7k; `core` compiles unchanged).
- Estimate: roughly 35-50 person-days to a runnable NeoForge 26.1 build with feature parity, about 5 more per additional 26.x drop. Largest cost: client rendering (monitor, projector, bus cable model, robot, shaders) and gametests. Details in section 5.
- Blockers that need a decision, not work: the Markdown Manual (no 26.x build of either variant), ProjectRed / CodeChickenLib / CB Multipart (no 26.x artifact), and whether 1.21.1 stays supported in the same tree (section 4).

## 2. Toolchain (what was run)

### 2.1 Versions available

| Item | Finding |
|---|---|
| NeoForge on maven.neoforged.net | `26.1.0.x` ... `26.1.2.x` (146 builds; `26.1.2.114` newest non-beta, release builds carry no `-beta` suffix), `26.2.0.x` (89 builds, newest `26.2.0.88`), `26.3.0.x-beta` (45 builds). Latest 1.21.1 is `21.1.253` (we pin `21.1.248`). |
| NeoForm | `26.1.2-1`, `26.1.1-1`, `26.1-1`, `26.2-1`, `26.2-2`, plus snapshots. Required by `core` (`neoFormVersion`). |
| Java | `config.json` of `neoform-26.1.2-1` says `java_target: 25`, functions run on `java_version: 25`. The mod, `core` and every Gradle toolchain must be JDK 25 (we use 21). |
| ModDevGradle | `net.neoforged.moddev` 2.0.148 is the newest on plugins.gradle.org (we use 2.0.144). Works with Gradle 9.6.1 (our wrapper) running on JDK 25. |
| Mappings | Mojang ships unobfuscated jars since 26.1: the NeoForm pipeline has no mapping step any more (`downloadClient/Server -> preProcessJar -> decompile -> patch`). Names are Mojang names, but some classes were renamed in the 1.21.x -> 26.1 period (`ResourceLocation` -> `net.minecraft.resources.Identifier`, `GuiGraphics` -> `GuiGraphicsExtractor`, ...). |
| Parchment | Not published for 26.x: `maven.parchmentmc.org` has `parchment-1.21.11` as newest. `neoForge.parchment {}` has to go for 26.x (loss: parameter names/javadoc in the IDE only). |
| Stonecutter | `dev.kikugie.stonecutter` 0.9.8 (plugins.gradle.org and maven.kikugie.dev). |

### 2.2 `neoFormVersion` for `core`

Still the right mechanism. `./gradlew :core:compileJava` with `neoform_version=26.1.2-1` resolved, decompiled and recompiled Minecraft (no loader) without any change to `core/build.gradle.kts` except the toolchain (`JavaLanguageVersion.of(25)`).
The same with `26.2-2` also compiled. The NeoForm version string is `<mc>-<n>` and is independent of `neo_version` as before (`26.1.2-1` vs `26.1.2.114`).

### 2.3 JDK availability in the sandbox

`ls /usr/lib/jvm`: only JDK 21 (`java-21-openjdk-amd64`). Gradle toolchain resolution via foojay (`api.foojay.io`) works: it returned the Temurin 25 package; the archive is served from the Adoptium GitHub release (`github.com/adoptium/temurin25-binaries`), 141 MB, extracted
and used as `JAVA_HOME` (`openjdk 25.0.4.1 LTS`). `api.adoptium.net` answers too. So no blocker. (`settings.gradle.kts` already applies the foojay resolver 0.9.0, so on a normal machine a `languageVersion = 25` toolchain is auto-provisioned.)

### 2.4 Build-configuration changes that were needed (scratch only)

Applied to a copy; none of it committed:

1. `gradle.properties`: `minecraft_version=26.1.2`, `neo_version=26.1.2.114`, `neoform_version=26.1.2-1`.
2. `build.gradle.kts`, `core/build.gradle.kts`: moddev `2.0.148`, `JavaLanguageVersion.of(25)`.
3. Remove the `parchment { ... }` block.
4. **Remove `add("additionalRuntimeClasspath", ...)`** (3 lines for ceres/sedna/sedna-buildroot): fails with "there is no additional classpath anymore for Minecraft 26.1.2. Add the dependency to a standard configuration such as implementation or runtimeOnly". Our `implementation` already has them.
5. `gradle/verification-metadata.xml` and `gradle.lockfile` have to be regenerated (the new plugin POM failed dependency verification; I ran with `--dependency-verification=off` and deleted the lockfiles). Expect a one-time `--write-verification-metadata sha256` + `--write-locks` per target.
6. `settings.gradle.kts`: `include("fabric")` removed (Loom 1.14.10 fails with `Failed to find official mojang mappings for 26.1.2`, covered by the Fabric spike).
7. Dependencies: JEI `29.43.0.106` (artifact names `jei-26.1.2-common-api`, `jei-26.1.2-neoforge-api`; resolves from maven.blamejared.com), ProjectRed/CCL/CBM/Architectury/Markdown Manual removed (section 3).
8. Not tried: Error Prone 2.50 / SpotBugs / PMD / Checkstyle on JDK 25 (`compileJava` ran with Error Prone applied and did not complain, the other analysers were not run).

Note: Maven Central answered 429 once during resolution of JEI's POM (Gradle asks Central before blamejared); a retry succeeded.

## 3. Library availability for 26.1

| Library | Used for | 26.1 / 26.2 status |
|---|---|---|
| ceres 0.0.7, sedna 4.0.1, sedna-buildroot | pure Java, no Minecraft dependency | resolved from `libs/` and compiled against 26.1.2; Java 21 bytecode runs on 25. Runtime on 26.1 not tested. |
| JEI | `compileOnly` API, `runtimeOnly` | available: `29.43.0.106` for 26.1/26.1.1/26.1.2 on NeoForge (Modrinth + blamejared); 26.2 is the `30.x` line (`30.39.0.232`). JEI plugin classes were part of the error set (API unchanged by me). |
| Architectury API | dependency of the Markdown Manual | `20.0.4+neoforge` (and `20.0.2`) for 26.1-26.1.2 on Modrinth; a 26.2 listing exists. Only needed because of the manual. |
| Markdown Manual (`markdownmanual`) | in-game manual (`client/manual/**`, `ManualItem`) | newest builds `MC1.21.11-neoforge-1.2.7` / `-fabric-1.2.7`; its Modrinth page lists game versions up to `1.21.11`. **No 26.x build.** The "Renewed" fork we pin on NeoForge 1.21.1 (`maven.modrinth:13P81Hg3:1.3.1`) is 1.21.1-only. `li.cil.manual.api` is unresolved in 6 places. |
| CodeChickenLib / CB Multipart | runtime-only, ProjectRed integration | newest on maven.covers1624.net: `1.21.11-4.7.0.533` / `1.21.11-3.6.0.169`. No 26.x. |
| ProjectRed | `compileOnly` API (bundled cable integration, `Integrations.registerModIntegration`) | no 26.x on Modrinth search; the covers1624 maven did not list a 26.x version. Treat as absent. |
| Forge Config API Port | Fabric only | not relevant here. |
| night-config | NeoForge ships it (`core 3.8.3`, `toml 3.8.3` in the NeoForge 26.1.2 POM) | fine. |
| oc2r native networking | JNI/native jars via `jarJar` | independent of Minecraft. Not run. |

Consequences: the manual and the ProjectRed integration have to be feature-gated for 26.x (compile them out through Stonecutter, or ship without) until the upstream libraries move. Dropping the manual dependency also removes Architectury.

## 4. Compile experiment

### 4.1 What was run

1. `./gradlew :core:compileJava` on 26.1.2 with only the build-config changes of 2.4: 36 errors in 8 files (all in `core/.../platform`).
2. After `ResourceLocation` -> `Identifier` (95 files, 313 occurrences, one regex) the same task: 11 errors in 2 files. After minimal stubs (below) `BUILD SUCCESSFUL`; the same with NeoForm `26.2-2`.
3. `./gradlew compileJava -q` (root, NeoForge module) with the NeoForge 26.1.2.114 patched Minecraft decompiled and recompiled (6882 NeoForge/Minecraft source files recompiled in 48 s, 92 classpath items): first run 1001 errors (javac `-Xmaxerrs 1000` cap in our build);
   after removing `@MethodsReturnNonnullByDefault` (net.minecraft annotation, 114 `package-info`/class files, removed in 26.1; replace with `javax.annotation`/`org.jspecify` or drop) and lifting the cap to 20000: **1588 errors, 1548 unique**, in 253 of the 1105 main files (`common` 642, `client` 589, `gametest` 138, `data` 123, `platform` 55, `api` 1).
   Raw logs and the categoriser are in the session scratchpad, not in the repository.

### 4.2 Core (the stage-1/2 seams)

| Cause | File | Fix |
|---|---|---|
| `BlockEntityRendererProvider<T>` now has a second type parameter (render state) | `core/.../platform/ClientRegistrar.java:34` | `<T, ?>` (used above: `BlockEntityRendererProvider<T, ?>`); real fix needs the render state type in the bridge |
| `BlockColor` / `ItemColor` removed: tinting is `BlockTintSource` / `ItemTintSource`, item tints are data-driven (`assets/<ns>/items/*.json`) | `ClientRegistrar.java:5,6,44,47` | stubbed to `Object`; needs a redesign of `registerBlockColor/ItemColor` |
| `ItemStack.save(provider, tag)` / `ItemStack.parse(provider, tag)` removed (codec-based) | `core/.../platform/ItemStackHandler.java:142,159` | `ItemStack.CODEC` with `provider.createSerializationContext(NbtOps.INSTANCE)` |
| `CompoundTag` getters return `Optional` (`getIntOr`, `getListOrEmpty`, `getCompoundOrEmpty`), `contains(String, int)` gone | `ItemStackHandler.java:153-157` | switched to the `...Or` variants |

### 4.3 Errors grouped by cause (NeoForge module, unique errors)

Counts are first-layer errors; numbers per symbol are occurrences of the error, files in parentheses where useful.

**A. NBT / serialization API (about 330 errors, mostly mechanical, wide).**
- `CompoundTag.getX()` now returns `Optional<X>`: "incompatible types: Optional<...> cannot be converted to ..." (CompoundTag 37, int 21, boolean 13, String 12, byte[] 8, long 8, Tag 7), `contains(String,int)` 37, `getList(...)` 9, `hasUUID`/`getUUID`/`putUUID` 18+17+16, `writeBlockPos`/`readBlockPos` 7+6. Example: `common/blockentity/computer/bus/ComputerBusElement.java:93` (`hasUUID`), `common/blockentity/network/connector/NetworkConnectorConnectionStore.java:28,59`.
- Block entity/entity persistence moved to `ValueOutput`/`ValueInput` (`net.minecraft.world.level.storage`): `saveAdditional` 13, `loadAdditional` 13 (does not override / cannot be applied), `handleUpdateTag` 9, `onDataPacket` 1, entity `addAdditionalSaveData`/`readAdditionalSaveData` (`common/entity/robot/AbstractRobotEntity.java:70,73`). Example: `ComputerBlockEntity.java:137,143`.
- Effort: 3-5 days. The seam `PlatformBlockEntity` (per loader, same FQN) declares `onLoad`/`handleUpdateTag`/`onDataPacket` but not save/load, so it does not absorb this yet; adding a loader-neutral `save(ValueOutput)`/`load(ValueInput)` pair there (and the same for 1.21.1 delegating to `CompoundTag`) is the Stonecutter-friendly way.

**B. Interaction results and item/block hooks (about 90 errors).**
- `ItemInteractionResult` removed (18 + 14), `InteractionResultHolder` removed (9), `InteractionResult.sidedSuccess` gone (12): `common/block/cable/BusCableBlock.java:119,127`, `common/item/network/NetworkInterfaceCardItem.java:103,112`, `common/block/computer/interaction/ComputerBlockInteraction.java:70`, `common/block/disk/DiskDriveBlock.java:98`.
- Other signature changes: `Item.appendHoverText` 5, `getOrCreateDescriptionId` 5 (`common/item/MemoryItem.java:16`), `neighborChanged` 3 (`VxlanBlock.java:42`), `spawnAtLocation(ItemStack)` 6 (`InventoryOperationsHelper.java:82,110`), `Tier` removed 16 (`BlockOperationsModuleDevice.java:27,170`), `Entity.interact` etc.
- Effort: 2-3 days.

**C. Recipes, data components, tags (about 60 errors).**
- `RecipeSerializer` is no longer an interface (`common/item/crafting/WrenchRecipe.java:47,50`), `RecipeBuilder.shaped(RecipeCategory, Item)` 34 (`data/recipe/CardRecipes.java:11,22`), `ItemTagsProvider.tag(TagKey)` 12 (`data/tag/ModItemTagsProvider.java:110,118`), `ChunkPos` is a record with private fields (`x`/`z` 21, constructor 14: `client/audio/LoopingBlockEntitySound.java:41`, `client/renderer/cable/NetworkCableRenderer.java:51`), `Util` moved (16).
- Effort: 2-3 days, plus data pack JSON migration (recipes in `data/` 142 json files, `pack_format` 34 -> 26.x format, lang/blockstate/model JSON re-validation, see E).

**D. Client GUI (about 130 errors, 21 files).**
- `GuiGraphics` is `GuiGraphicsExtractor` with a different (extraction-style) API: 68 errors in 21 files (`client/ClientCompat.java:9,45`, container screens, `AbstractModContainerScreen.render/renderBg/renderLabels/renderTooltip` 12+4+2+1 "does not override", `ImageButton.onPress/renderWidget`, `mouseClicked/keyPressed/keyReleased` signatures no longer match (probably event-object parameters, not verified) (4+4+2 in `AbstractMachineTerminalScreen`, `AbstractMonitorDisplayScreen.java:62`)).
- Effort: 3-5 days (terminal, keyboard and monitor screens are input-heavy).

**E. Client rendering (the dominant cost, about 350 errors, 40+ files).**
- Models: `BakedModel` (18, 6 files), `ModelData` / `ModelProperty` (17 + 6, `client/hooks/BusCableModelHooks.java:27,34`), `IDynamicBakedModel` 5, `BlockModelRotation`, `ItemOverrides` 8, `BakedQuad`, `Material`
  (`client/model/BusCableModel.java:29`, `client/model/monitor/MonitorModel.java:10`), `BlockAndTintGetter` 12, `RenderType` 18 (it moved to `...renderer.rendertype`), `BlockEntityWithoutLevelRenderer` removed (robot / computer item renderers: `client/gui/screen/computer/ComputerBlockItemRenderer.java:17,33,81,90`;
  items render through `SpecialModelRenderer` + item model JSON now), `ItemProperties` (`client/item/CustomItemModelProperties.java:13,17`), packages `net.neoforged.neoforge.client.model.data` (13), `...model.geometry` (6, `RegisterGeometryLoaders`), `...model.generators` (5, datagen).
- Shaders / GL state: `ShaderInstance` removed (11, `client/renderer/MonitorGUIRenderer.java:17,123`, `platform/NeoForgeShaderEvents.java`), `RegisterShadersEvent` removed, `RenderSystem.depthMask/enableBlend/disableBlend/defaultBlendFunc/applyModelViewMatrix` gone (10+7+5+9+12), `LightTexture` 8
  (`client/renderer/BusInterfaceNameRenderer.java:14,96`), `VertexBuffer` 10. The pipeline is `RenderPipeline`-based now: the projector depth pass (`FrustumMixin`, `LevelRendererMixin`, `MinecraftMixin`, `DepthBufferStage`, `ColorCompositingStage`, core shaders in `assets/oc2r/shaders`) and the monitor/GPU renderers have to be rewritten, not patched.
- Block entity renderers: `BlockEntityRenderer<T>`/`Provider<T>` now take a render state type (9 + `ProjectorRenderer.java:18`, `InternetGateWayRenderer.java:16`, `platform/NeoForgeClientRegistrar.java:55`); the three renderers (projector, gateway, network cable) and the robot renderer move to the extract-render-state pattern.
- NeoForge client events: `RenderLevelStageEvent.Stage` / `getStage()` / `getProjectionMatrix()` (`platform/NeoForgeClientEvents.java:24-45`), `RenderFog`/`FogRenderer`, `TriState` (`platform/NeoForgeProjectorEvents.java:5,11,24,31`).
- Mixins: 4 in total (`FrustumMixin`, `LevelRendererMixin`, `MinecraftMixin`, `ServerChunkCacheMixin`). Their targets (`Frustum`, `LevelRenderer`, `Minecraft`) were reworked; they cannot be validated until the code compiles and a client starts (no display in the sandbox besides xvfb).
- Effort: 10-15 days (bus cable model with facades, monitor model/quads and GUI renderer, projector depth pass, robot model/renderer, item models and tints).

**F. NeoForge API changes outside rendering (about 40 errors, concentrated in `platform/`, which is the point of the seams).**
- Capabilities: `Capabilities.ItemHandler/FluidHandler/EnergyStorage` are now `Capabilities.Item/Fluid/Energy`, with the new transfer API: `EnergyHandler`, `ResourceHandler<ItemResource>`, `ResourceHandler<FluidResource>` (`net.neoforged.neoforge.transfer.*`); the old `IItemHandler`/`IFluidHandler`/`IEnergyStorage` are gone. Hit: `platform/NeoForgeCapabilities.java:110-146` (9) and the wrappers/views next to it. `BlockCapability.createSided/createVoid` themselves still exist. Absorbed by the capability bridge: only that one class family has to be rewritten; common code uses our `ItemHandler`/`FluidHandler`/`EnergyStorage` from `core`. Effort 3-4 days (insert/extract semantics changed to transaction + resource amounts, so the wrapper logic is not a rename).
- Network: `PacketDistributor.sendToServer` is gone from the client path (`platform/NeoForgeNetworkBridge.java:14`; client sends go through `ClientPacketDistributor`), `DirectionalPayloadHandler` (`platform/NeoForgeMessageRegistrar.java:9,63`), `IPayloadContext.getServer()` missing in `common/network/util/MessageUtils.java:54,72,90`. Effort 1 day.
- Game events: `AddReloadListenerEvent` (`platform/NeoForgeCommonEvents.java:7,70`; the replacement event was not looked up in this spike), `FMLLoader.getDist()` is no longer static (`platform/NeoForgeEnvironment.java:13`). Effort 0.5 day.
- Game hooks: `Item.hasCraftingRemainingItem/getCraftingRemainingItem` moved to `ItemStack.getCraftingRemainder` (`platform/NeoForgeGameHooks.java:60`), `IBlockStateExtension.onDestroyedByPlayer` signature (`:75`). 0.5 day.
- Datagen (`ExistingFileHelper` 10, `data/DataGenerators.java:18,26`, `ModBlockStateProvider.java:138,139` `itemModels()`): the block/item model provider rewrite is part of E; 3-4 days. (`runData` was already broken on 1.21.1 for `blocks/speaker`.)
- Registries, `DeferredRegister` leftovers, config (`ModConfigSpec`), `FMLJavaModLoadingContext`/mod bus: no errors reported in the first layer; `NeoForgeRegistryBridge` is not in the error list. Treat as unverified until the module links.
- `net.neoforged.api.distmarker` is used in 122 files and produced no errors.

**G. GameTest (138 errors, 22 test classes).**
- `@GameTest`, `@GameTestHolder`, `@PrefixGameTestTemplate` (31 + 16 + 16, e.g. `gametest/ComputerFixture.java:104`, `DeviceBusTests.java:16,21`) are gone; NeoForge 26.1 has `RegisterGameTestsEvent` (class present in the 26.1.2.114 universal jar) and vanilla tests are registry/data driven. `GameTestAssertException` constructor changed (25), `GameTestHelper.getBlockEntity` (13), checked exceptions (23, "unreported exception"). Our 22 tests, `GameTestResultReporter` and the empty template `empty.nbt` have to be re-plumbed; the test bodies mostly survive.
- Effort: 3-4 days, plus whatever the tests find at runtime. This is the only automated safety net the project has for a runtime port, so it should come early, not late.

**H. Libraries (about 15 errors).** `li.cil.manual.api` unresolved (6), JEI and ProjectRed plugin classes (depends on the dependency decisions in section 3).

### 4.4 Not run

`runClient`, `runServer`, `runGameTestServer`, `runData`, `build` (jarJar, native libs), `lint`. All of those depend on a compiling module. Known runtime-level risks: mixin targets, `neoforge.mods.toml` (`minecraft` `versionRange = "1.21.1"`, `neoforge [21.1,)`, `pack_format 34`), core shader JSON format, item model definitions (`assets/oc2r/items/*.json` do not exist, 62 item model JSON files under `assets/oc2r/models/item`), blockstate/model JSON (106 asset json), sound JSON.

## 5. Effort estimate

Person-days for one engineer who knows the code base; all numbers are estimates from the error groups above, not measurements.

| Work | Days |
|---|---|
| Build: JDK 25 toolchain, ModDevGradle bump, verification metadata and lockfiles, drop `additionalRuntimeClasspath`/parchment, CI JDK | 1-2 |
| `core` and platform seams (ClientRegistrar tint/render-state redesign, ItemStackHandler, network, events, game hooks) | 3-4 |
| Capabilities and transfer API (`NeoForgeCapabilities` + wrappers) | 3-4 |
| NBT/`ValueInput`/`ValueOutput` across block entities, entities, items (group A) | 3-5 |
| Interaction results, items, recipes, tags, misc vanilla signatures (B, C) | 4-6 |
| GUI (`GuiGraphicsExtractor`, input events) | 3-5 |
| Rendering: models, item rendering, shaders, projector, monitor, robot (E) | 10-15 |
| Datagen and resource/data pack migration | 3-5 |
| GameTests (G) | 3-4 |
| Mixin and runtime debugging (first start, server, client, game tests green) | 5-10 |
| Manual and ProjectRed: gate out; replace manual later | 1-2 (gating), manual replacement open-ended |
| **Total to a green NeoForge 26.1** | **about 35-60, mid-estimate 45** |
| Each further drop (26.2): `core` compiles unchanged, expect rendering/API deltas | 3-6 |

## 6. Stonecutter layout

Stonecutter 0.9.8. Two compatibility families are visible: `1.21.1` (current code) versus `26.x` (26.1 and 26.2, differing by about 3% of Mojang's classes). Three Stonecutter versions with a two-way source split is the realistic shape; a three-way comment-preprocessor tree is not needed for 26.1 vs 26.2 until a concrete difference shows up.

Recommended layout:

```
settings.gradle.kts            stonecutter { create(rootProject) { versions("1.21.1", "26.1.2", "26.2"); vcsVersion = "26.1.2" } }
stonecutter.gradle.kts
versions/<v>/gradle.properties minecraft_version, neo_version, neoform_version, java (21 / 25 / 25), jei, no parchment on 26.x
core/src/main/java             loader- and version-independent (compiles on all three after the 17-line fix of 4.2 and the Identifier swap)
src/main/java                  shared NeoForge sources (the root module)
src/mc1211/java                version-specific classes with the same FQN, one copy per family (as already done per loader for PlatformBlockEntity and ClientCompat)
src/mc26/java
```

How to keep the sources in one tree:
1. **Token swaps** (Stonecutter `swaps`/`replacements`) for pure renames: `ResourceLocation` <-> `Identifier`, `GuiGraphics` <-> `GuiGraphicsExtractor` type names, `MethodsReturnNonnullByDefault`. These cover a few hundred of the 1548 errors (the `ResourceLocation` rename alone touched 95 files / 313 uses).
2. **Version-specific source sets with the same FQN** for anything that is structural: block entity save/load (`PlatformBlockEntity` extension), item/block tints and renderers, models, shaders, projector pipeline, gametest registration, `NeoForgeCapabilities` value mapping. This is the pattern the Fabric work already uses and the stage 1-2 seams were designed for.
3. **Comment preprocessor (`//? if >=26.1 {`)** only for one-line API differences inside otherwise shared methods (`getIntOr` vs `getInt`, `InteractionResult` factory methods). Avoid it in code the rendering rewrite touches: the resulting conditional density would make the files unreadable.
4. Resources: per-version resource roots (`versions/26.x/resources`) for `pack.mcmeta`, `neoforge.mods.toml` (the `minecraft` and `neoforge` `versionRange`), and the item model definitions that only 26.x needs; shared textures/lang/sounds stay common. Shaders and core-shader JSON are different per family.

Which existing seams already absorb differences:

| Seam | Absorbs | Does not yet |
|---|---|---|
| `RegistryBridge`, `BlockHolder`/`ItemHolder` | registration (no errors in this layer) | `ItemHolder.getId` uses `Identifier` (swap) |
| `NetworkBridge`/`MessageRegistrar`/`MessageContext` | payload plumbing is isolated to 3 files; `MessageContext` hides `IPayloadContext` | `MessageUtils` still calls a removed server accessor |
| Capability bridge (`CapabilityBridge`, `ItemHandler`/`FluidHandler`/`EnergyStorage`, `NeoForgeCapabilities`) | the entire new transfer API lands in one class family; common code is untouched | wrapper logic must be re-implemented against `ResourceHandler`/`EnergyHandler` transactions |
| `CommonEvents`/`ClientEvents` | most event churn (reload listeners, ticks) is in `NeoForgeCommonEvents`/`NeoForgeClientEvents` | render-level stages and fog events have no neutral equivalent |
| `GameHooks` | crafting remainder, break, sound type hooks: 2 errors on 26.1 | |
| `PlatformBlockEntity` | `onLoad`, update tag, data packet | save/load (`ValueOutput`/`ValueInput`) is called directly from 13+ block entities and is the largest non-rendering group |
| `ClientRegistrar` | renderer / colour / screen registration | signature of renderer provider (render state) and tint types are part of the interface and have to change |
| `ClientCompat`, `ClientProxy` | client calls that NeoForge patched into vanilla | `GuiGraphics` is in the signature of 21 files |
| `PlatformEnvironment` | `FMLLoader`/`FMLPaths`/`ModList` | `FMLLoader.getDist()` static -> instance change is one line in the NeoForge impl |
| Models split (`BusCableModelUtils`, `MonitorQuads`, `MonitorModelTypes`) | loader-neutral geometry logic | the Mojang model API itself changed, so even the "neutral" logic needs a 26.x variant |

## 7. Recommended order of work

0. **Decide the scope.** Is 1.21.1 maintained in the same tree (Stonecutter) or does 26.x become a successor line? If the latter, a branch plus the Identifier/Value-IO migration is cheaper. If Stonecutter, do step 1 on `work` first, in the 1.21.1 build.
1. Build plumbing only: Stonecutter skeleton with `versions/{1.21.1,26.1.2,26.2}`, JDK 25 toolchain per version, ModDevGradle 2.0.148, regenerate verification metadata and lockfiles, version-specific `gradle.properties`. Acceptance: 1.21.1 unchanged and green, `:core:compileJava` green on all three.
2. Mechanical swaps that are also valid on 1.21.1 where possible: `@MethodsReturnNonnullByDefault` replacement, `Identifier` alias via swap. Remove `additionalRuntimeClasspath` for 26.x.
3. Seams: loader-neutral `save/load` on `PlatformBlockEntity`; redesign `ClientRegistrar` colour and renderer-provider parts; the NBT helpers in core (`ItemStackHandler`).
4. Server-side game logic first (groups A, B, C, F-network/events/hooks, capabilities/transfer): everything outside `client/` and `data/` and `gametest/`. Goal: dedicated server boots with the mod.
5. GameTests (G) before any rendering work; they validate the server logic from step 4 and give the regression net.
6. Data pack/resources and datagen (E-datagen, resource migration) so recipes, loot tables, tags and models load.
7. Client: GUI (D), then models and item rendering (bus cable, monitor, robot), then block entity renderers, then the projector/monitor shader pipeline last (highest risk, the mixins live here).
8. Gate or replace the Markdown Manual and ProjectRed integration for 26.x; watch upstream.
9. 26.2 as a second Stonecutter version: bump `neoform_version=26.2-2`, `neo_version=26.2.0.x`, JEI `30.x`, fix the delta.
10. Stage 4 (CI matrix) for the 26.x NeoForge target afterwards: JDK 25 job, per-version artifacts.

## 8. Open questions and risks

- Rendering is the dominant risk and cannot be estimated better without starting it; a client smoke test needs xvfb (it worked for 1.21.1, see roadmap item 42).
- Behavioural changes that compile but break (item interaction order, `use` results, block entity sync, capability invalidation timing, transaction semantics of the transfer API). The identity-stable capability wrapper requirement documented in the roadmap (otherwise the device bus rescans) must be preserved in the new wrappers.
- Static analysis (Error Prone, SpotBugs, PMD, Checkstyle) on JDK 25 was not exercised.
- Mod loader metadata, jarJar of ceres/sedna and the native networking libs on 26.1 were not run.
- The Fabric side shares the Minecraft API changes (groups A to E), so the version-specific source sets should be shared between the two loaders wherever they only depend on Minecraft, not on the loader.
