# Roadmap: upstream fnuecke/oc2

## 41. Upstream oc2 v0.3.0: comparison and porting

The upstream maintainer returned to the original mod (release v0.3.0, 1.21.1/0.3.0). Our fork is a full refactor, independent of their tree (not a git fork, own git history). Some of their terminal fixes overlap the already closed
[section 36](done.md#36-closed-items) (M1-M5): considered closed, do not port.

Sub-sections in other pages: [41.1 GameTest CI and CD](multiloader.md#411-gametest-ci-and-cd) and [41.4 dependency updates](multiloader.md#414-dependency-updates); robot row of 41.2 in [robot.md](robot.md#412-robot-rows).

### 41.2 Targeted fixes from the release

Fixed items are in [done.md](done.md#412-fixed-items) (Redstone side mixed-up #164 `ec49a41`, network connector on fences #225). Rows that were "already handled" or plain feature requests are closed as won't-do or done:

- [x] Robot `detect(side)` API (issue #108): closed as won't-do (feature request; see [robot.md](robot.md#28-robot-fixes)).
- [x] JEI computer recipe (issue #270): already handled, `ExtraGuiAreasJEIPlugin` deliberately removes the preconfigured computer from JEI; normal computer recipes are visible. Closed.
- [x] Keyboard as terminal user (issue #186): already handled, `TerminalUserProvider` is implemented in `ComputerTerminalManager` and `Robot`; `TerminalKeyboardHandler` is GUI-level. Closed.

### 41.3 Not applicable or already closed

- Terminal fixes of v0.3.0 are covered by section 36 (M1-M5, commit f6f9d09); ours are more detailed (charset G0/G1, SGR recovery, dirty-mask overflow, ST-string ESC-abort parity).
- Linux fdisk/swap (#47/#268), console keymaps (#147): guest Linux image, not mod Java code.
- Block device data unification (#127, BREAKING): datapack image format (`data/oc2/block_devices/{hdd,floppy,flash}/`), needs a separate compatibility decision.

## 42. North-Western-Development/oc2r: what changed since 2025-09 (checked 2026-10-03)

Local clones for reference live in `ref/` (git-ignored via `.git/info/exclude`): `ref/oc2r` (full history), `ref/OpenComputers` and `ref/OpenComputers-CE` (shallow). Compared the `1.21.1` and `1.20.1` branches (last real activity: merged PRs #113, #116/#117, #123, #138, #143; since August 2026 only a `gradle.properties` bump).

Everything user-visible from that period is **already in our tree**, so there is nothing left to port:

- [x] Android native library and the JVM ICMP fallback (`34f7af7e`, `32bd195a`): `NativeLoader` has the `ANDROID` platform and the probe/fallback.
- [x] Projector / simple-framebuffer fix (`43923e0f`, `106c48aa`, issue #25): `#address-cells`/`#size-cells` on `/chosen`, no `no-map`.
- [x] Unicode sanitisation in the font renderer (`f694982b`), deferred font atlas upload (`62618c0e`), async executor reopen for single player (`daf67b59`), `ClassCastException` guard in `AbstractBlockEntityDeviceProvider` (`2cb3c81d`).
- [x] Restricted container data component, free flash in the computer/robot recipes, old device tags on upgrade, firmware key name (#112, #113, #116).
- [x] `OC2R_BUS_PATH` for the Lua bus (#138), `flash.sh` rewrite and the larger `fw_jump.bin` (#123, #143; byte-identical to ours), `tcpdump` removed.
- Not applicable: the `ColorData[]` NBT serializer (`89c8ec71`) fixes a crash in upstream's terminal; ours keeps the colour tables `transient`. `Invalidatable` removal (`4a49e898`) is a refactor of upstream's own bus code that our fork already replaced.

Open upstream branches worth a look only if the internet card is touched again: `InternetCardChanges-Test` (2025-07, "add cache").

## 49. fnuecke/oc2 since f37735d (checked 2026-10-08, ported 2026-10-11)

`ref/oc2` pulled from `f37735d` to `480af73` (17 commits, 2026-10-02 to 2026-10-07). Upstream master has since moved to Sedna 5.2.0 / sedna-cpm 0.3.0 / sedna-buildroot 0.2.10 (2026-10-09); that is what was ported, not the 5.1.0 recorded here before.

- [x] **Sedna 5.2.0** (`sedna_version=4.0.1` → `5.2.0`, `sedna_buildroot_version=0.0.72-oc2r1` → `0.2.10`, branch `feat/sedna-5.2`). Verified against the 4.0.1/5.2.0 API diff: `MemoryMap.setDirty` removal, `Buildroot` API swap (firmware boot ROM + single rootfs.ext2, bootfs gone), board syscon. sedna-cpm (now 0.3.0) is still not a dependency — nothing here uses it (no Z80/CP-M).
- [x] **System controller adoption**: the board syscon is published into the guest device tree (`SystemControllerProvider`), so capable guests can reboot/power off through it; a guest-initiated reboot cycles the VM instead of wedging it in STOPPED. Host-driven `requestPowerOff`/`requestReset` stay unused on purpose — no shipped guest enables them yet and the syscon interrupt is not routed.
- [x] Issue #315 (`984b831`): item devices are no longer created on the client (`isClientSide` on the item-stack handlers); `HardDriveDeviceWithInitialData` carries the `BlockDeviceData` and resolves the image on the worker thread; `BlockDeviceData.getCapacity()` added for the side-safe energy math.
- [x] Capability caching (`67e1e00`, `630a0ff`): **reviewed, not ported**. There is no `Invalidatable` here and the `CapabilityBridge`/`BlockCapabilityCache` already re-resolve per call; providers return `Optional<Device>`. Same bug class cannot occur; per-call delegating wrappers remain optional hardening.
- [x] Crafting module (`b1366f9`), RPC-only (no MLAPI layer here): `craft()` over slots 0-8 with output from the selected slot, recipe, docs, no dedicated game test (recipe tests cover it).
- [x] Scanner module (`802b8be`), RPC-only: `inspect`/`scan` (7x7x7 hardness cube, 5s cooldown) / `canSeeSky`/`getScanLayer`, recipe, docs, `ScannerModuleTests`.
- [x] Device naming rework (`a1527f1`, `8cf6ee3`): **skipped on purpose**. The bug it fixes (HLAPI vs MLAPI naming parity, #316) cannot occur here — there is no MLAPI/IO layer, no `SYSTEM` index-0 contract, no per-device id codecs. The synthetic display-name devices already cover the alias-like behavior.
- [x] Robot basic localization (`3acd14a`), RPC-only: `GlobalPos` origin (first tick + dimension change, persisted), `getFacing`/`getPosition`/`calibratePosition` plus guest library wrappers, docs, `RobotTests.robotLocalizationTracksMovement`.
- [x] Less jittery robot movement (`aa8c8b4`): `lerpTo` snap thresholds with `isComplete`, carried-entity lift/drag in `moveTowards`, `canCollideWith` via the vehicle rule with an ignore list, `ServerGamePacketListenerImplMixin` (NeoForge + Fabric) so standing on a robot counts as grounded, `RobotCarryTests`. Renderer float→double does not apply (our animation state has no absolute-time parameter). 26.x notes: `lerpTo` does not exist there (override is 1.21-only), `Boat.canVehicleCollide` is inlined per version, client `shouldMoveEntity` falls back to the server rule.
- [x] Tank module texture tweak (`22a16d0`): nothing to port (no tank module here; take the PNG if the module is ever ported).
- [x] Broken-link check (`083846d`): nothing to port (no in-game manual renderer here).
- [x] Tests upstream changed along the way (`d153daa`, `cc16a81`, `ee02e3e`): reviewed. `cc16a81`/`ee02e3e` are mass cleanups of upstream's own test tree (81 and 61 files in a layout we do not have): nothing to port. `d153daa` adds `VMRunner.setLockstep(true)` for tests (VM runs inside the tick, so tick timeouts mean something; boot timeout 900k to 50k ticks). Not ported: it is a test-only behavior change in `VMRunner`; take it only if game tests start timing out nondeterministically.

### Follow-up review — 2026-10-23

The reference checkout was updated from `276b061` to `6e94a86` (17 commits).
See [the detailed review](upstream-2026-10-23.md) for movement/destruction,
cable lifecycle, block initialization, rendering and Velocity config-sync
candidates. These are investigation items, not reproduced OC2R defects or
completed ports. Sedna 5.3.0 is kept separate from the Fabric 26.x migration.
