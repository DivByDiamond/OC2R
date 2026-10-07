# Spike: the Fabric module against Minecraft 26.1

Feasibility spike of 2026-10-03 for stage 3 of [multiloader.md](multiloader.md) (multiversion 1.21.1 / 26.1 / 26.2, here: Fabric side, 26.1 only).
Nothing was merged; the build changes below were made in a scratch worktree and discarded. This document is the only artifact.

## Summary

- A Fabric build against 26.1 is **toolchain-feasible today**: JDK 25 can be downloaded in the sandbox, Loom, Loader, Fabric API, Forge Config API Port, Team Reborn Energy and Architectury all have 26.1 builds, the Gradle project configures and Loom sets up Minecraft 26.1.2.
- **Markdown Manual has no 26.x build at all** (newest: 1.21.11). The in-game manual is a hard blocker on Fabric 26.1 until upstream ships one, or we drop/replace the manual dependency.
- The source does not compile: **1216 javac errors in 223 of 1116 files** after two mechanical renames (see "Method"). It is not a 26.1 delta but a 1.21.1 -> 1.21.2 ... 1.21.11 -> 26.1 delta (about ten Minecraft versions of API drift in one step).
- The loader-independent server/common code is mostly mechanical (NBT `Optional` getters, `ValueInput`/`ValueOutput`, `InteractionResult`, `Identifier`). The client rendering code (about 40 percent of the errors, concentrated in the projector/terminal/model/shader code) is a rewrite against the new render-state/pipeline architecture and dominates the effort.
- Rough effort for one developer: server-side only (dedicated server + `runGameTest` green) 1.5-2 weeks; full client parity 5-8 weeks in total. The existing seams absorb the Fabric API renames well; they do not absorb the vanilla drift, which needs per-version source overlays (Stonecutter) or a thin vanilla-compat layer.

## 1. Toolchain

| Item | Finding |
|---|---|
| Java | **25** (Minecraft 26.1.2 `javaVersion` is `java-runtime-epsilon`, major 25; Loom 1.18.2 Gradle module metadata: `org.gradle.jvm.version` 25; fabric-loader 0.19.5 also publishes 25). The Gradle daemon itself must run on JDK 25 to apply Loom 1.18. |
| JDK availability | `/usr/lib/jvm` has only 21. `api.foojay.io` (200) and `api.adoptium.net` (200, redirect to GitHub release assets) are reachable; Temurin 25.0.4.1 (141 MB tarball) was downloaded and unpacked into the scratchpad, no hacks needed. The repo already has the foojay toolchain resolver in `settings.gradle.kts`, so Gradle can auto-provision JDK 25 for a `JavaLanguageVersion.of(25)` toolchain when it has network; I ran Gradle itself with `JAVA_HOME` pointing at the downloaded JDK. |
| Gradle | Loom 1.18.2 module metadata requires Gradle 9.7.0; the wrapper is 9.6.1, so it needs a bump (9.7.0 distribution is downloadable). The whole multi-project build (the NeoForge root project with ModDevGradle 2.0.144, `core`) configured fine on Gradle 9.7.0 + JDK 25. The NeoForge/`core` projects still target a Java 21 toolchain for 1.21.1; with a per-version toolchain this must stay version-specific. |
| Loom | 1.18.2 is the newest stable (1.18.0-alpha.* and 1.17.x also exist; 1.17.x is the line the repo comment calls out). **Loom no longer needs mappings for 26.1** (Mojang ships unobfuscated, named jars): the plugin id is different. `fabric-loom` (the current id) fails with `Failed to setup Minecraft, ... Configuration 'mappings' has no dependencies`; `net.fabricmc.fabric-loom` (no remapping; the maven repo also lists `fabric-loom-remap`, `fabric-loom-no-remap` modules) works with the `mappings(...)` line removed. In the no-remap plugin `modImplementation`/`modCompileOnly` are plain `implementation`/`compileOnly`, there is no remap step, and mod jars built for old versions cannot be consumed (Loom rejects them: `Expected official namespace for access widener entry, found: intermediary in mod: architectury`). |
| Access widener | Header must be `accessWidener v2 official` instead of `named` (Loom: `Expected official namespace for access widener entry, found: named`). All 7 entries of `oc2r.accesswidener` still resolve in 26.1 (`AbstractContainerScreen.leftPos/topPos/hoveredSlot`, `Screen.renderables`, `MenuScreens$ScreenConstructor`, `LevelResource.<init>`, `ServerCommonPacketListenerImpl.connection` checked with javap). Fabric's own libs moved to the "class tweaker" name (`forgeconfigapiport.classtweaker`); the `.accesswidener` file name was still accepted. The 1.21.1 and 26.1 builds therefore need different AW headers (per-version resource). |
| Fabric Loader | 0.19.5 (latest; 0.19.x needed for 26.x; Forge Config API Port 26.1.5 requires `>=0.19.0`). |
| Fabric API | `0.155.3+26.1.2` is the newest 26.1 line (versions `+26.1`, `+26.1.1`, `+26.1.2` exist from 0.140.0 up; 26.2 is `0.161.0+26.2`, 26.3/26.4 builds already exist). 1.21.1 uses `0.116.17+1.21.1`. |
| `fabric.mod.json` | Same format; `depends` as today. 26.1 mods use `"minecraft": "~26.1-"` (Forge Config API Port 26.1.5 ships exactly that; the trailing dash admits pre-releases), `"fabricloader": ">=0.19.0"`, `"fabric-api": ">=0.149.0"`, `"java": ">=25"`. Our current `"minecraft": "~1.21.1"` and `"java": ">=21"` must become per-version values (Stonecutter template of the file). |
| Mojang hosts | `piston-meta`, `launchermeta`, `libraries.minecraft.net`, `piston-data`, `resources.download.minecraft.net` and `maven.parchmentmc.org` answer from the sandbox, Loom downloaded 26.1.2 client/server jars. Parchment does not exist for the unobfuscated versions (names are Mojang's, parameter names are in the jar). |
| Dependency verification | `gradle/verification-metadata.xml` rejects the new Loom plugin POM; every new artifact (Loom 1.18.2, loader, API modules, energy 5.0.0, FCAP) needs verification entries (I ran with `--dependency-verification=off`). `gradle.lockfile` and the per-module lockfiles will change too. |
| Libraries | see section 4. |

### Resulting scratch configuration (not committed)

`gradle.properties`: `minecraft_version=26.1.2`, `fabric_loader_version=0.19.5`, `fabric_api_version=0.155.3+26.1.2`, `team_reborn_energy_version=5.0.0`, `forge_config_api_port_version=jUe0ucoE` (26.1.5).
`fabric/build.gradle.kts`: plugin `net.fabricmc.fabric-loom` 1.18.2, toolchain/`options.release` 25, `mappings(...)` line deleted, all `modImplementation` -> `implementation`, Markdown Manual and Architectury removed, `net.neoforged:mergetool:2.0.0:api` made non-transitive (its transitive `net.minecraftforge:srgutils:0.4.15` is not found by the stricter repository content filters under the new plugin). Patch kept outside the repo.

## 2. Compile experiment

### Method

`JAVA_HOME=<Temurin 25> ./gradlew :fabric:compileJava --dependency-verification=off` (Gradle prints at most 1000 errors, so the real counts come from running `javac -proc:none -Xmaxerrs 100000` directly on the source list and class path Gradle resolved). The first Gradle run stopped at the 1000-error cap. Two purely mechanical renames were then applied to the scratch tree to unmask the next layer: `ResourceLocation` -> `Identifier` (104 files, 193 symbol errors) and removal of the deleted `net.minecraft.MethodsReturnNonnullByDefault` annotation (114 files, mostly `package-info.java`). Afterwards: **1216 errors, 223 files of 1116** (the Fabric compile unit: `core/` + `src/main/java` minus excludes + `fabric/src/main`). javac attributes all classes, but a missing class hides errors on its members, so the real number is higher; treat 1216 as a lower bound of the shape, not the total.

By area (after the two renames):

| Area | Errors | Files |
|---|---|---|
| shared common (`src/main/java/.../common`, `api`) | 595 | 132 |
| shared client (`src/main/java/.../client`) | 444 | 67 |
| `fabric/` platform + client model classes | 133 | 13 |
| `fabric/` entrypoint, hooks, mixins | 32 | 9 |
| `core/` | 12 | 2 |

### Error groups, concrete examples, effort

1. **Pure renames (done in the experiment, about 1 hour with a script).** `ResourceLocation` -> `net.minecraft.resources.Identifier` (everywhere), `net.minecraft.MethodsReturnNonnullByDefault` removed (use `javax.annotation`/JSpecify or drop). These are the only groups that are version-trivial to script, and they are the same in Stonecutter via a `//? if` import alias or a mass rename.

2. **NBT / serialization (common, about 170 error sites in 104 files that touch `CompoundTag`; 3-5 days).**
   - `CompoundTag` getters return `Optional` since 1.21.5: 56 `contains(String,int)` mismatches (type constants gone: `TAG_ANY_NUMERIC`), Optional<CompoundTag>/Integer/Boolean/String/Long/byte[] results (about 110 sites), `getAllKeys()` -> `keySet()` (`FabricInteractionHooks.java:36`), `getUUID/hasUUID/putUUID` removed (53 sites), `NbtUtils.readBlockPos/writeBlockPos` changed (13).
   - Block entities and entities save through `ValueInput`/`ValueOutput`: `BlockEntity.loadAdditional/saveAdditional(CompoundTag, Provider)` -> `(ValueInput)` / `(ValueOutput)` (26 overrides cannot override; `Entity.saveWithoutId/load` in `EntityMixin.java:33-43`; `AbstractRobotEntity`). This is semantic, not a rename: every `loadAdditional`/`saveAdditional` (68 occurrences) and all helper classes in `common/serialization/nbt` and `common/util/nbt` (`NBTUtils`) need a port or a neutral wrapper. The NBT byte format of existing worlds must stay readable (`ItemStackHandler`/ceres blobs).

3. **Vanilla gameplay API drift (common, 3-4 days).** `ItemInteractionResult`/`InteractionResultHolder`/`sidedSuccess` removed in favour of `InteractionResult` (32 sites: `AbstractBlockDeviceItem`, `BusCableInteractionHandler`, ...), `Block.getCloneItemStack(LevelReader,...)` signature (`FabricInteractionHooks.java:67`, `MinecraftPickBlockMixin`), `ChunkPos` is now a record with private `x`/`z` and a different constructor (`ChunkUtils`: 22 errors), `Level.random`/`isClientSide` no longer accessible, `Tier`/`Tiers`/`TieredItem` -> `ToolMaterial` (wrench/tool items), `Util` moved (`Util.make`), `DyedItemColor` constructor, `appendHoverText` signature, `GameRules` access, `SHAPELESS_RECIPE`, `RecipeSerializer` registrations, `Item.getRecipeRemainder` (`FabricGameHooks.java:66`), `ResourceLocationException`, `Registry` internals (`FabricRegistryBridge.java:114`, `MappedRegistry.createSimple`; `:149`/`:171` `Optional<Reference>` from registry lookups).

4. **Fabric API changes (fabric glue, about 165 errors, 2-3 days).** Seams are intact; each class needs renames:

   | Old (1.21.1 build) | 26.1 | Where |
   |---|---|---|
   | `ExtendedScreenHandlerType`/`ExtendedScreenHandlerFactory` (`...screenhandler.v1`) | `net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<T, D>` (takes a `StreamCodec` for the extra data), `ExtendedMenuProvider` | `FabricMenuBridge.java:6,29,36` (the byte-array transport can become a real codec) |
   | `PayloadTypeRegistry.playC2S()/playS2C()` | `serverboundPlay()/clientboundPlay()` | `FabricMessageRegistrar.java:24-44` |
   | `ServerWorldEvents.LOAD/UNLOAD`, `ServerTickEvents.START_WORLD_TICK`, `ClientTickEvents.START_WORLD_TICK` | `ServerLevelEvents.LOAD/UNLOAD`, `START_LEVEL_TICK` / `END_LEVEL_TICK` | `FabricCommonEvents.java:38-42`, `FabricClientEvents.java:26`, `FabricBlockEntityHooks.java:39`, `FabricClientBlockEntityHooks.java:17` |
   | `WorldRenderEvents`/`WorldRenderContext` | `client.rendering.v1.level.LevelRenderEvents` (extraction/submit-phase events: `START_MAIN`, `AFTER_OPAQUE_TERRAIN`, `COLLECT_SUBMITS`, ...) | `FabricClientEvents.java:33-49` (feeds `ClientEvents.RENDER_LEVEL`, used by the projector) |
   | `ColorProviderRegistry`, `BlockColor`/`ItemColor` | `BlockColorRegistry` taking `BlockTintSource` lists (`ClientRegistrar` in `core` exposes `BlockColor`/`ItemColor`, the interface itself changes) | `FabricClientRegistrar.java:7-65`, `core/.../ClientRegistrar.java:5-6` |
   | `EntityModelLayerRegistry` | `ModelLayerRegistry` | `FabricClientRegistrar.java:52` |
   | `BlockRenderLayerMap` | gone; layer is a block-state-model/`ChunkSectionLayer` property (`ChunkSectionLayer`: SOLID, CUTOUT, TRANSLUCENT) | `FabricClientSetup.java:31` (bus cable cutout) |
   | `BuiltinItemRendererRegistry` | gone (items render through item models/special model renderers, there is no `BlockEntityWithoutLevelRenderer`) | `FabricClientSetup.java:40` (robot item) |
   | `CoreShaderRegistrationCallback` | gone (no `ShaderInstance`; shaders are `RenderPipeline`s) | `FabricClientSetup.java:33`, `ModShaders` |
   | `FabricItemGroup` | `creativetab.v1.FabricCreativeModeTab` / `CreativeModeTabEvents` | `FabricGameHooks.java:88` |
   | Forge Config API Port `fabric.api.neoforge.v4.NeoForgeConfigRegistry/NeoForgeModConfigEvents` | `fabric.api.v5.ConfigRegistry/ModConfigEvents` (`ModConfigSpec` still under `net.neoforged.neoforge.common`, shared config code needs no change) | `OC2RFabric.java:3-4,28-33` |
   | `PreparableReloadListener.reload`, `Identified` listener | signature changed (`FabricCommonEvents.java:58-73`) | |
   | Team Reborn Energy 4.1.0 / Transfer API / API Lookup | **no errors** in `FabricEnergy`, `FabricEnergyBridge`, `FabricCapability*` against energy 5.0.0 + transfer-api 8.0.6 (compile-level only; the energy 5.0.0 pom pulls `fabric-transfer-api-v1:7.0.0` and loader 0.18.4 that Gradle upgraded) | |

5. **Mixins (7 server, 9 client, 14 targets; about 1 week, only verifiable at runtime).** All 14 target classes and the 7 access-widener targets still exist in 26.1. Checked with javap against the 26.1.2 jar, the injection points:

   | Mixin | Target | 26.1 |
   |---|---|---|
   | `LevelChunkMixin` | `setBlockState`, `clearAllBlockEntities` | present, same shape |
   | `ServerChunkCacheMixin` | `save` | present |
   | `ServerPlayerGameModeMixin` | `useItemOn` + redirect of `ServerPlayer.isSecondaryUseActive()` | present (`useItemOn` returns `InteractionResult`); redirect target needs re-check |
   | `EntityMixin` | `saveWithoutId`, `load` | now `saveWithoutId(ValueOutput)` / `load(ValueInput)`, handler signatures must change |
   | `MinecraftPickBlockMixin` | `pickBlock` | **renamed `pickBlockOrEntity`**, the `Block.getCloneItemStack` redirect target changed |
   | `ClientPacketListenerMixin` | `handleBlockEntityData` | present |
   | `LevelChunkClientMixin` | `replaceWithPacketData` | present, 3rd parameter changed |
   | `FogRendererMixin` | `setupFog` | class moved to `renderer.fog.FogRenderer`, `setupFog` now returns `FogData` |
   | `GuiMixin` | `renderItemHotbar` | **now `extractItemHotbar(GuiGraphicsExtractor, DeltaTracker)`** |
   | `EntityRendererMixin` | `renderNameTag` | **gone** (`getNameTag(T)` remains; name tags are extracted into render state) |
   | `FrustumMixin` | `offsetToFullyIncludeCameraCube(int)` | present |
   | `LevelRendererMixin` | `renderLevel` (9 new parameters: allocator, `CameraRenderState`, `GpuBufferSlice`, `ChunkSectionsToRender`), `renderSnowAndRain`, `renderSky`, `shouldShowEntityOutlines`, `entityTarget/getItemEntityTarget/getWeatherTarget` | `renderLevel` reshaped, `renderSnowAndRain`/`renderSky` **gone**, the rest present; the projector depth pass hooks around `RenderSystem.depthMask`/`applyModelViewMatrix`/`FogRenderer`, none of which exist (11 errors at `LevelRendererMixin.java:51-157`) |
   | `MinecraftMixin` | `getMainRenderTarget`, `useShaderTransparency` | present (`useShaderTransparency` is static) |
   | `SoundEngineMixin` | `play` wrapping `SoundBufferLibrary.getStream` | `play` now returns `SoundEngine$PlayResult`; target `getStream` call must be re-verified |

   The mixin config uses `compatibilityLevel` `JAVA_21`; it should be `JAVA_25`. Mixins are validated only at runtime (client start-up), not by compilation.

6. **Client rendering (the bulk: 444 shared-client errors in 67 files + 133 in Fabric platform/model classes; 3-5 weeks).** Minecraft 26.1 has the new extract/submit render architecture and the Blaze3D `RenderPipeline`/`GpuDevice` abstraction. Concretely missing in our code:
   - `GuiGraphics` -> `GuiGraphicsExtractor` (70 errors; 21 files; screens, widgets, terminal overlay): `render` methods become `extractRenderState`-style.
   - `BakedModel`, `ItemOverrides`, `BakedQuad`, `BlockModelRotation`, `ItemTransforms` (moved to `resources.model.cuboid`), `BlockAndTintGetter` (moved), `Material` (moved to `resources.model.sprite`): the Fabric bus cable and monitor models (`FabricBusCableModel`, `FabricMonitorModel`, `FabricModels`: 89 errors) must be redone on `BlockStateModel` + the Fabric Renderer API v1 mesh/`QuadEmitter` (a different API from the 1.21.1 `FabricBakedModel`), plus `ModelLoadingPlugin` changes.
   - `RenderType`, `ShaderInstance`, `VertexBuffer`, `BufferUploader`, `GlStateManager`, `RenderSystem.depthMask/enableBlend/setShaderColor/applyModelViewMatrix`, `LightTexture`, `ItemRenderer`, `BlockEntityWithoutLevelRenderer`: `ModRenderType` (13), `ModShaders` (7), the `client/renderer/stage` projector pipeline (`DepthBufferStage`, `ColorCompositingStage`, `DepthOnlyRenderTarget`), `MonospaceFontRenderer`, terminal rendering `common/vm/terminal/render` (28) and the robot model (`RobotModel`: 10) all use immediate-mode style GL or `ShaderInstance`s. They have to be rewritten against `RenderPipeline`s, render states and `SubmitNodeCollector`. The projector's depth/colour compositing is the single riskiest piece (it hooks the world renderer's internals).
   - Block entity renderers (monitor, computer, projector): `BlockEntityRenderer` is now an extract-state/submit pair; `BlockEntityRendererProvider<T>` has 2 type arguments (`FabricClientRegistrar.java:39`).
   - `client/manual/**` (47 errors) and `MonospaceFontRenderer`: depend on Markdown Manual (blocker, section 4).
   - `NativeImage.setPixelRGBA` renamed, `ResourceLocationException`, creative-tab/tooltip hooks (`ClientCompat` is a Fabric-specific shim over vanilla fields and will need checking).

## 3. Seams: what absorbs what

| Seam | Absorbs | Does not absorb |
|---|---|---|
| `Platform`/`RegistryBridge`/`NetworkBridge`/`MessageRegistrar`/`EnergyBridge`/`CapabilityBridge`/`MenuBridge`/`CommonEvents`/`PlatformEnvironment` (`core` interfaces, Fabric impls in `fabric/.../platform`) | **Fabric API renames and restructurings**: all of table 4 is confined to `FabricMessageRegistrar`, `FabricMenuBridge`, `FabricCommonEvents`, `FabricClientEvents`, `FabricClientRegistrar`, `FabricClientSetup`, `FabricGameHooks`, `OC2RFabric`. No shared (`src/main/java`) class mentions a Fabric API. Energy/transfer/capabilities compile without change. | Vanilla types in the interfaces: `ClientRegistrar` exposes `BlockColor`/`ItemColor`/`BlockEntityRendererProvider<T>`, `RegistryBridge` exposes `Registry`/`ResourceKey`, `MenuBridge` exposes menu types. Those signatures change with the Minecraft version, so the `core` interfaces need per-version variants (or version-neutral functional parameters, e.g. a `TintFunction` of our own). |
| `GameHooks` (core), `FabricInteractionHooks`, `PlatformBlockEntity` per loader | The loader differences; the 26.1 `ValueInput`/`ValueOutput` change lands in `PlatformBlockEntity` (declares `loadAdditional/saveAdditional`-adjacent hooks) and in each block entity subclass. | Vanilla persistence API (section 2.2). Needs a version-neutral abstraction (`NbtReader`/`NbtWriter` or a `Persistable` adapter in `core`) used by all 68 sites, introduced on 1.21.1 first. |
| `ClientProxy`, `ClientCompat` (twice, same FQN), `ClientEvents` (`CLIENT_TICK_START`, `RENDER_LEVEL`, `HIDE_HOTBAR`) | Event delivery. | The payload of `RENDER_LEVEL`/`HIDE_HOTBAR`: they hand out `PoseStack`/`GuiGraphics`-era types; the 26.1 render-state/submit events carry different data. The projector and terminal overlay need a per-version client renderer. |
| `FabricModels`/`FabricBusCableModel`/`FabricMonitorModel` | - (loader specific by design) | Entire rewrite for 26.1 (section 2.6). The loader-neutral logic that was split out earlier (`BusCableModelUtils`, `MonitorQuads`) is the reusable part and should stay free of vanilla model types. |
| Mixins (`oc2r.fabric.mixins.json`, `oc2r.fabric.client.mixins.json`) | - | Per-version by nature (section 2.5). Targets: client mixin config entries must be selected per version (Stonecutter can swap the config file or the `client` list). |
| `fabric.mod.json`, `oc2r.accesswidener` | - | Per-version resources (versions, `java`, AW namespace `named` vs `official`). |

## 4. Library availability for Fabric 26.1

| Library | 1.21.1 (current) | 26.1 | Verdict |
|---|---|---|---|
| Fabric Loader | 0.16.14 | 0.19.5 | available |
| Fabric API | 0.116.17+1.21.1 | 0.155.3+26.1.2 (also `+26.1`, `+26.1.1`) | available |
| Forge Config API Port | Modrinth `N5qzq0XV` | `jUe0ucoE` = 26.1.5 (`mc26.1.x`), includes night-config 3.8.3 inside (our separate night-config `include`s become redundant); API package `fabric.api.v5` | available, one rename |
| Team Reborn Energy | 4.1.0 (4.2.0 needs 1.21.5+) | 5.0.0 on maven.fabricmc.net (depends on transfer-api 7.0.0) | available (compile verified, runtime not) |
| Architectury API | `Pzc2FP5K` 13.0.11 | `TZJ7aDWD` = 20.0.4+fabric (also 20.0.2) for 26.1/26.1.1/26.1.2 | available, only needed for Markdown Manual |
| **Markdown Manual** | 1.2.6 (Loom 1.14 limit), 1.2.7/1.2.8 | **none**: newest Fabric/NeoForge builds are `MC1.21.11-*-1.2.7`; Modrinth lists game versions up to 1.21.11 | **blocker** for the manual on 26.1 (see recommendations) |
| ceres / sedna / sedna-buildroot | in `libs/` | plain Java libraries, no Minecraft dependency | unaffected (built for Java 21 bytecode, loadable on 25) |

## 5. Recommended Stonecutter split on Fabric

1. **Do not try to make one source tree compile for 1.21.1 and 26.1 with `//? if` everywhere.** The drift is ten Minecraft versions wide; the error list above is about 1200 sites. Split along seams instead:
   - Keep `core/` + the shared common code on a **version-neutral API** where cheap (NBT access, `Identifier` alias, interaction results) and use Stonecutter comments only for these small, mechanical differences (`ResourceLocation`/`Identifier`, `getUUID`, `Optional` NBT getters can be hidden behind a few `core` helpers that Stonecutter swaps).
   - **Per-version source overlays** (`versions/1.21.1/`, `versions/26.1/` or Stonecutter's swap blocks for whole files) for: client rendering (`client/renderer/**`, `client/gui/**` render methods, `common/vm/terminal/render`), the Fabric model classes, the mixin classes and configs, `FabricClientSetup`/`FabricClientRegistrar`/`FabricClientEvents`/`FabricMenuBridge`, `fabric.mod.json`, `oc2r.accesswidener`.
   - Loom plugin id, mappings block, `modImplementation` vs `implementation`, Java toolchain and `options.release` become per-version build logic in `fabric/build.gradle.kts` (Stonecutter constants), the Gradle JVM bump to 25 is global.
2. **26.1 and 26.2 should share one overlay** (same unobfuscated, same render architecture; Fabric API `+26.2` is the next line of the same API), so the matrix is effectively two code lines per loader: 1.21.1 and 26.x.
3. The 1.21.1 line can stay exactly as it is (no change to the current green build). The risk is the shared-code changes made for 26.1 regressing 1.21.1; the 46 Fabric game tests and NeoForge game tests are the safety net, and every neutralising refactor should land and be proven on 1.21.1 first.

## 6. Recommended order of work

1. Toolchain scaffold (1-2 days): Gradle 9.7.0 wrapper, `libs`/verification metadata/lockfile updates, Stonecutter project for the `fabric` module with `26.1` as second target (Java 25 toolchain, `net.fabricmc.fabric-loom`, `implementation` instead of `modImplementation`, AW header, `fabric.mod.json` template, mixin `compatibilityLevel`). CI image/runner needs JDK 25 and Gradle 9.7 in addition to 21.
2. Neutralise common code on 1.21.1 first (3-5 days): `Identifier` alias, NBT helper layer, `ValueInput`/`ValueOutput` adapter in `PlatformBlockEntity`, interaction-result helpers, `ChunkPos` access, `ToolMaterial`. Verify 1.21.1 stays green (lint, tests, NeoForge and Fabric game tests).
3. Port the shared common code and the Fabric bridge glue to 26.1 until `./gradlew :fabric:compileJava` is green with the client code excluded (like the current `-Pfabric.common=false` split, plus a `fabric.client=false` switch) (about 1 week). Then the Fabric server side runs: `:fabric:runGameTest` (46 tests) on 26.1 is the first real milestone (about 1.5-2 weeks total from the start).
4. Mixins: server mixins first (`LevelChunk`, `ServerChunkCache`, `ServerPlayerGameMode`, `Entity`), then client, one at a time with a start-up smoke test (about 1 week).
5. Client in this order, each as its own PR: registrations/screens/GUI (`GuiGraphicsExtractor`) -> models (bus cable, monitor on `BlockStateModel` + Renderer API mesh) -> block entity renderers and robot -> terminal rendering/fonts -> projector depth pass and shaders (`RenderPipeline`s). 3-5 weeks, the last two items are the risk. `runClient` under xvfb with the harness used for the 1.21.1 verification applies.
6. Manual: decide before step 5 (see below).
7. Release matrix (stage 4) for the Fabric 26.x artifact.

## 7. Blockers and risks

- **Markdown Manual has no 26.x release.** Options: wait for upstream; fork/port the library (it is a rendering library, so it faces the same GuiGraphics/render rewrite as our client); ship Fabric 26.1 without the manual (`ManualItem` stand-in, as before the manual was wired) and add it later; or replace it with an in-house book. This must be decided by the maintainer; I did not investigate the library's licence or source.
- **Projector depth pass and shaders** hook renderer internals (`Frustum`, `LevelRenderer.renderLevel`, `RenderSystem` state, `ShaderInstance`) that no longer exist; there may be no direct equivalent in the pipeline architecture, so the feature may need a redesign, not a port.
- **NBT/world format**: the migrations touch every `loadAdditional`/`saveAdditional`. The 1.21.1 saves must still load (or a 1.21.1 -> 26.1 world upgrade through DataFixers must be tested), the Fabric 26.1 build starts fresh worlds only unless this is verified.
- **Java 25 everywhere**: the whole Gradle build must run on JDK 25 (Loom 1.18 constraint); the CI runners and contributors need it; the 1.21.1 targets keep compiling at release 21 through toolchains.
- Counts here are compile-level and a lower bound: no mixin was applied, no client was started, and the 26.1 runtime behaviour of Fabric API pieces (menu extra data, networking, energy) is unverified.
- The NeoForge side of 26.1 (and `core`'s NeoForm 1.21.1 dependency, which cannot compile 26.1 types) is being investigated separately; `core` is compiled as plain sources by the Fabric module, so it did not block this spike.

## 8. Effort estimate (one developer, calendar time)

| Piece | Estimate |
|---|---|
| Toolchain + Stonecutter scaffold | 1-2 days |
| Neutralising/porting common code (NBT, interaction, ChunkPos, tools, registry) | 1-1.5 weeks |
| Fabric glue (menu, networking, events, config, creative tab, capabilities, registry bridge) | 2-3 days |
| Mixins (server + client) incl. runtime checks | about 1 week |
| Client: GUI/screens, models, block entity/robot renderers | 2-3 weeks |
| Client: terminal renderer, projector, shaders | 1-2 weeks, high uncertainty |
| Manual (depends on decision) | 0 (drop) to 1-2 weeks (port) |
| **Total** | **server-side green: 1.5-2 weeks; full client parity: 5-8 weeks** |
