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
- [ ] NeoForge startup crash (#294): needs repro.
- [ ] Projector blank display (#230): needs repro.
- [ ] Sound attenuation (#288): needs repro. Note: the "speaker is silent" check of [section 21](qa-checklist.md#from-section-21) may be the same problem as #288.

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
