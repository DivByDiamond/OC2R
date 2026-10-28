# TODO

Index of open work. Details live in [docs/roadmap/](docs/roadmap/README.md) (English pages by subsystem; section numbers are stable and map to pages in the README there).

| Page | Topic |
|---|---|
| [terminal.md](docs/roadmap/terminal.md) | VT100 terminal, diff sync, double-size, vttest |
| [video-gpu.md](docs/roadmap/video-gpu.md) | Monitors, GPU, video codec, jcodec removal |
| [network-inet.md](docs/roadmap/network-inet.md) | Internet card, VXLAN, security hardening |
| [network-overhaul.md](docs/roadmap/network-overhaul.md) | Wires, towers, tablet, Create Radars |
| [cables-bus-energy.md](docs/roadmap/cables-bus-energy.md) | Bus cable, energy, world sync |
| [devices-storage.md](docs/roadmap/devices-storage.md) | Cards, disks, input, flash builder |
| [robot.md](docs/roadmap/robot.md) | Robot fixes |
| [multiloader.md](docs/roadmap/multiloader.md) | NeoForge + Fabric, GameTest CI |
| [quality.md](docs/roadmap/quality.md) | Lint, tests, audits, docs |
| [upstream.md](docs/roadmap/upstream.md) | Upstream fnuecke/oc2 sync |
| [qa-checklist.md](docs/roadmap/qa-checklist.md) | In-game verification boxes |
| [icebox.md](docs/roadmap/icebox.md) | Vague or blocked backlog |
| [done.md](docs/roadmap/done.md) | Closed work |


## Open tasks

- [ ] Verify all "code ready" features in game (multi-monitor, GPU tiers, CPU tiers, codecs, disks, cables) (see docs/roadmap/qa-checklist.md); part of the 26.x acceptance is already closed in [stage3-acceptance.md](docs/roadmap/stage3-acceptance.md)
- [x] Speaker audio on NeoForge 26.1.2 (2026-10-16): real guest RPC beep produced 880 Hz for 500 ms; fixed PCM playback from one packet to all 44 (0.51042 s captured versus 0.51084 s supplied, delivered over about 10 s by RPC). Preload, 100 ms read cap and manual underrun recovery are covered by regression tests. Full suites pass: 482 tests on 26.1.2 and 478 on 1.21.1. Evidence in [build/reports/render-qa](build/reports/render-qa); details in [multiloader rendering notes](docs/roadmap/multiloader.md#26x-rendering-notes-2026-10-14).
- [x] World shutdown persistence: prevent post-unload empty-chunk overwrite; restore 26.x scalar NBT strings and rebuild item devices after level assignment. Real save/reload preserves 12 block entities, eight computer components and guest root writes (2026-10-16).
- [x] Verify warm CPU/RAM continuation and automatic framebuffer retention after world reload (2026-10-20): all 11 framebuffer addresses and CRCs compared before any guest input, both computer CPU counters continued, fresh RAM-only shell nonces verified in the computer and in the robot; passed on 26.1.2 and 26.2 in a new JVM, including a 26.1.2-to-26.2 world copy. Legacy grouped monitor/keyboard state that was already overwritten still needs one cold computer stop/start (see [stage3-acceptance.md](docs/roadmap/stage3-acceptance.md)).
- [x] Speaker follow-up: cached PCM streams are dropped on world exit and when the sound engine stops ticking them (2026-10-17); in-game check of both lifecycle transitions still open (see docs/roadmap/multiloader.md)
- [x] Finish multiversion stage 3 (branch multiversion-stage3): closed 2026-10-20 for the four supported targets (NeoForge 1.21.1, 26.1.2, 26.2 and Fabric 1.21.1). The final verification passed all four assemblies, 1,680 unit tests (543 main + 17 core on each of the three NeoForge nodes) and 174 server GameTests (38 NeoForge 1.21.1, 40 NeoForge 26.1.2, 40 NeoForge 26.2, 56 Fabric 1.21.1) (see [stage3-acceptance.md](docs/roadmap/stage3-acceptance.md)). The remaining tail of the stage — CI confirmation of the branch and documentation sync — moved to Stage 4 Wave A (see [multiloader.md](docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion)); the repository-wide `checkstyleMain` part of that tail is closed since 2026-10-21 (see [quality.md](docs/roadmap/quality.md#39-lint-stages)).
  - [x] Embed Markdown Manual in shared sources and verify it in game on all four targets (2026-10-18); remove the external Markdown Manual/Architectury dependencies. Complete the Russian page set (43 pages), update English/Russian content against code and the actual Linux image, and verify Russian rendering/navigation. See [manual content audit](docs/MANUAL_CONTENT_AUDIT.md).
  - [x] Run the real GameTest matrix on all four targets (snapshot of 2026-10-18: 35 NeoForge 1.21.1, 37 NeoForge 26.1.2, 37 NeoForge 26.2 and 53 Fabric 1.21.1 tests, all required and passing; the final set of 2026-10-20 is 38/40/40/56 = 174, see the parent item); configure NeoForge mock-player channels before mod payload broadcasts.
  - [x] Port in-world terminal row-mesh construction to 26.2 `BufferBuilder`/`ByteBufferBuilder`; 26.1.2 and 26.2 compile.
  - [x] Port 26.2 network cable rendering to the level submit pipeline; both 26.1.2 and 26.2 compile.
  - [x] Verify 26.2 network cables and terminal backgrounds/glyphs/cursor in game (2026-10-16). Fixed the terminal text pipeline's forward-depth comparison for 26.2 reversed-Z; the in-world screen now shows glyphs, colored text/background and the blinking cursor. Screenshot: `build/reports/render-qa/26.2-terminal-world-after-depth-fix.png`.
- [~] Stage 4: six-target implementation, acceptance, CI and merge completed in PR #59 (`88376085`). Release publication is deliberately deferred at the owner's request; development continues without a new beta.
  - [x] Fabric 26.1.2/26.2 server and client ports, six-target CI/release wiring, independently reviewed and merged. Accepted PR CI: 1,682 unit tests, 330 OC2R server GameTests, modern client acceptance and static analysis.
  - [ ] Publish a future release only when requested; the presence of six-target release workflows is not a release request.
- [x] Implement four-target publishing (2026-10-22): `publish.yml` now stages and uploads NeoForge 1.21.1, NeoForge 26.1.2, NeoForge 26.2 and Fabric 1.21.1 to the GitHub release, CurseForge and Modrinth (one Modrinth version per Minecraft version, the mod version from the tag; `fabric.mod.json` now carries the real version instead of `unspecified`). Targets without a configured token or CurseForge project are skipped with a `::notice::` instead of failing the release. Owner-side setup still required: the `MODRINTH_TOKEN` secret and the `CURSEFORGE_PROJECT_ID_26_1_2` / `_26_2` / `_FABRIC` repository variables (see [multiloader.md](docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion), section 42, Stage 4 Wave A).
- [x] Document the two `ChunkMapMixin` implementations (2026-10-22): both files now carry a Javadoc block that names the counterpart, explains why the class cannot be shared (the Fabric module compiles the shared tree from a separate source set, and the two mixin configs never co-exist) and why the `remap` attribute differs (Loom remaps the target to the intermediary name, NeoForge has no SRG name for the private target).
- [x] Give the client mixin config its own refmap (2026-10-22): `mixins.oc2r.client.json` now points at `mixins.oc2r.client.refmap.json`, so a refmap emitted by an annotation processor can no longer be overwritten by the second config.
- [x] Sedna 5.2.0 + buildroot 0.2.10 + upstream §49 ports (branch feat/sedna-5.2; upstream master moved past the 5.1.0 recorded before — ported 5.2.0/0.3.0/0.2.10 instead; see docs/roadmap/upstream.md#49-fnuecke-oc2-since-f37735d-checked-2026-10-08)
- [x] Remove the `required = false` flag from 9 GameTests after fixing them: already done in `db7abe14` (2026-09-16); verified 2026-10-17 (snapshot: `:1.21.1:gameTest` ran 35 tests, all required, 0 failures; the suite grew to 38 tests on 1.21.1 in the 2026-10-20 matrix) (see docs/roadmap/multiloader.md#45-gametest-ci-a-real-baseline)
- [ ] Terminal double-size PR-B: double width and render of `lineAttrs` (see docs/roadmap/terminal.md#443-double-sized-characters-esc-3-4-5-6)
- [ ] vttest golden-grid tests (layer 1) plus the 6 UTF-8/resize regressions (see docs/roadmap/terminal.md#444-vttest-automation)
- [ ] Line-drawing glyphs in the font, then drop the procedural quads (see docs/roadmap/terminal.md#445-remaining-tofu-glyphs)
- [ ] Terminal: pending-wrap flag (CPR column, m2) and mode 1048 save/restore (m5) (see docs/roadmap/terminal.md#minor-open)
- [ ] Terminal: Utf8Decoder, CUU/CUD/CUF/CUB, DSR/DA reply and tab tests (see docs/roadmap/terminal.md#tests-gaps)
- [ ] Remove vendored jcodec after the DELTA soak (K4 to K6) (see docs/roadmap/video-gpu.md#40-removing-jcodec-full-usage-list)
- [ ] Video low priority: dirty-region encode (V6), client decode off render thread (V8), pooling (V9), sender dedup (V10) (see docs/roadmap/video-gpu.md#38-video-items)
- [ ] VXLAN security: authenticate incoming UDP (C1) and per-block VNI (C2) (see docs/roadmap/network-inet.md#39-security-hardening-of-inet-linter-configs-libraries)
- [ ] Confirm C3 (source IP spoofing) is closed by the ARP filter; add 169.254.0.0/16 and other ranges to deniedHosts (C4) (see docs/roadmap/network-inet.md#39-security-hardening-of-inet-linter-configs-libraries)
- [ ] Limit bandwidth and number of internet cards per player (see docs/roadmap/network-inet.md#39-security-hardening-of-inet-linter-configs-libraries)
- [ ] Fix IcmpHandler.reject source address 0.0.0.0 (see docs/roadmap/network-inet.md#27-mod-subsystem-audit)
- [ ] Write user docs for the internet card (no DHCP, point-to-point) and answer issue #13 (see docs/roadmap/network-inet.md#27-mod-subsystem-audit)
- [ ] Network overhaul: wire node model, multi-port connectors, wire spool, wire types, sagging renderer (see docs/roadmap/network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars)
- [ ] Towers, signal model, tablet item family (UART and tower modes), Create Radars compat (see docs/roadmap/network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars)
- [ ] Receive-only face energy, "occupied by Computer #N" tooltip and API, chunk-load event instead of INCOMPLETE retry, world migration (see docs/roadmap/network-overhaul.md#43-cable-system-rebuild-from-scratch)
- [ ] Wrench UX and hub/switch re-evaluation (see docs/roadmap/network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars)
- [x] Buildroot 0.2.10 writable rootfs: private COW overlay and VM snapshot persistence implemented; four rootfs regressions pass, with full suites at 478 tests on NeoForge 1.21.1 and 482 on 26.1.2. Real cold boot confirms writable `/` and mounted `/mnt/builtin`; root-file writes surviving save/reload verified in the real guest (see docs/roadmap/multiloader.md, 26.x rendering notes)
- [ ] Projector follow-up: block-entity shadow depth on 26.1.2 (entity/particle occluders exist but are not verified in game); the 26.2 depth/compositing port compiles (2026-10-17) but is not yet verified in game. Real guest framebuffer and server energy drain/restoration verified on 26.1.2 (see docs/roadmap/multiloader.md)
- [x] 26.1.2 real display path: guest Linux drives monitor and projector together, monitor GUI shows the same framebuffer, and real generator removal/restoration disables/restores both images; authoritative monitor power and per-device frame delivery fixed (2026-10-16)
- [x] Monitor floating framebuffer: align display/status plane with cube model and separate support shape from selection/collision; side view and multiblock geometry regression verified (2026-10-16)
- [ ] Monitor/projector state duplicate sync and VS2 dimension filtering (see docs/roadmap/cables-bus-energy.md#29-world-sync-fixes)
- [ ] Robot: verify and fix thread race in RobotActionProcessor (needs manual check) (see docs/roadmap/robot.md#28-robot-fixes)
- [ ] Robot: collider chewing terrain, event handler leak, place() item consumption, export loses VM state (see docs/roadmap/robot.md#28-robot-fixes)
- [x] Keyboard/mouse capture and links to PC, monitor and projector; remappable Right Ctrl release. All six client/server targets verified, including native Onyx input and completed display frames (2026-10-28; see docs/roadmap/onyx-input-display-acceptance.md)
- [ ] UART tablet wire minigame and mirror terminal (shares an item base with the tower tablet) (see docs/roadmap/devices-storage.md#16-uart-tablet-item)
- [ ] 3D floppy model for DiskDriveRenderer (see docs/roadmap/devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code)
- [~] Flash/OS image writer: local-file and bundled firmware GUI implemented; verify client flow, then consider URL manifests (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [ ] `flashMemorySize` config instead of hard-coded 12 MB (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [x] Bug on 26.1.2 client: the computer screen stayed completely black (found in the first in-game run, 2026-10-11); fixed 2026-10-14 in b7d2dcbc — the in-world terminal is now replayed through `submitCustomGeometry` (`ModRenderType.getTerminalText/getTerminalCursor`), verified in game: the PC screen shows terminal output. The monitor block renders its ported `submitCustomGeometry` path correctly too (it shows the "not enough energy" status); showing a picture on it just needs in-game energy + a mounted computer, not a render fix. The GUI terminal (computer/robot screen) is drawn through `GuiElementRenderState` (90bfeb45, compiles, still to be checked in game) (see docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion, 26.x rendering notes)
- [x] 26.1.2 GUI terminal: capture pose/scissor at extraction, separate solid backgrounds/text/cursor, and snapshot cursor geometry; computer and robot GUI visually checked on 2026-10-16 (see docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion, 26.x rendering notes)
- [x] Bug on 26.1.2 client: network cables are not visible, the connection sounds play but there is no cable model (found 2026-10-11); fixed 2026-10-14 in b7d2dcbc — vanilla sends block entity data on a block update only when the entity supplies a packet, and `NetworkConnectorBlockEntity` returned null, so live connections on an already-loaded chunk never reached the client; added `getUpdatePacket()` (as `BusCableBlockEntity` already had for facades); verified in game: cables draw between connectors. The separate "old items stopped working until replaced" note is not reproduced yet
- [ ] Wire in or delete the dead qodana task (the CI lint job already uploads its reports with `if: always()`) (see docs/roadmap/quality.md#39-lint-stages)
- [ ] Lint stage B/C tail (see docs/roadmap/quality.md#39-lint-stages): the failBuild gates and `severity=error` are in place since `b3695d99` (2026-08-27), the repository-wide `checkstyleMain` is green on 1.21.1, 26.1.2, 26.2, `core` and `fabric`, and SpotBugs now really runs on every node (2026-10-22: the 26.x gate had been vacuous because the exclude filter matched "generated" against the compilation path of every class of those nodes; that rule is gone and a deliberate probe defect is caught on 26.1.2 again). Open: the per-node baselines `baseline-26.1.2.xml` / `baseline-26.2.xml` hold 370/371 pre-existing findings of the 26.x nodes (67/65 of them were not covered by the shared baseline) and need a burn-down; Error Prone is not default-on for the whole matrix (the `core` and `fabric` modules do not apply the plugin).
- [x] 26.x unit and game tests in CI (2026-10-22): the `test-26x` job runs `:26.1.2:test :26.2:test :core:26.1.2:test :core:26.2:test` and both 26.x GameTest suites on a Java 25 runner and uploads the JUnit XML of every node; the demo scripts got their own `demo-scripts` job (shell syntax, C syntax check, the RPC test, the Java harnesses and a gate against hardcoded public endpoints). Measured locally: 543 + 543 unit tests (main nodes) and 17 + 17 (core nodes).
- [x] `LineLength` is enabled (2026-10-22): the global mute is gone, the check follows the `.editorconfig` limit of 120 for every `*.java`, and all violations are fixed (the worst files were `ProjectorPipelineRenderer.java`, `ManualScreen.java`, `ModernModelDefinitions.java`, `ProjectorBlockEntityDepth.java` and the demo QA harness). The imported MarkdownManual package is excluded with a documented file scope, except for the shims this project added to it. The check also runs on the generated 26.x trees, which forced the block constructors to be split: the 26.x replacement of `Properties.of()` is 16 columns longer than the 1.21.1 call (see [quality.md](docs/roadmap/quality.md#39-lint-stages)).
- [ ] Replace the PMD file scope with point suppressions: `config/pmd/ruleset.xml` enables `AvoidSynchronizedStatement`, `AvoidFieldNameMatchingMethodName` and `ExhaustiveSwitchHasDefault` again, but excludes them for a hard-coded list of about 26 files through `ends-with(pmd:fileName(), …)`, so the rules stay blind for those files. The ruleset comment states that the point suppressions belong into the files themselves; PMD 7 cannot exclude a non-XPath rule per file, so the listed files need an explicit `@SuppressWarnings("PMD.<rule>")` with a justification (see docs/roadmap/quality.md#39-lint-stages).
- [x] PMD and SpotBugs for the `fabric` module (2026-10-22): PMD runs on the module's own sources only (`fabric/src/main/java`), with the deliberate `oc2r$` mixin naming accepted by the ruleset and the 14 real findings fixed in code (diamond operator, varargs, method references, `ReentrantLock` instead of `synchronized`, `equals` on registry keys, simplified returns); SpotBugs analyses the module's own class output only (the shared and core classes stay on the auxiliary classpath as types, never as analysis input), because a baseline entry is matched by instance hash and the same source compiled against different mappings never matches. The module reports zero findings; the structural mixin patterns (mixin casts, shared mutable state, an unread `@Shadow` field) are suppressed per class in `config/spotbugs/exclude-filter.xml` with a justification, and two real defects were fixed on the way (`requestModelDataUpdate` re-read the mutable `level` field, `copyPersistentData` dereferenced a possibly absent tag). Both tasks are part of `lintRatchet`.
- [ ] Remove the 25 legacy client imports from common (`LEGACY_OFFENDERS`) (see docs/roadmap/quality.md#9-lint-and-static-analysis)
- [ ] Javadoc for undocumented classes and review of about 81 NOPMD markers (see docs/roadmap/quality.md#33-code-documentation-and-nopmd-review)
- [ ] Threading fixes: GlobalInterruptController atomic mask (L6), ServerScheduler listeners (P2), SocketManager (P3), RPC adapter TOCTOU (see docs/roadmap/quality.md#47-audit-2026-09-17)
- [ ] NBTDeserializerImpl must throw on unexpected Tag (L10); rename shadowed TunnelManager (A3) (see docs/roadmap/quality.md#47-audit-2026-09-17)
- [ ] Invert 6 common-to-client imports (L13) and break package cycles (A2) (see docs/roadmap/quality.md#47-audit-2026-09-17)
- [ ] Lint config style items: global checkstyle suppressions, dynamic dependency versions, gametest println (see docs/roadmap/quality.md#47-audit-2026-09-17)
- [ ] Tests for VM core, bus, inet (UDP/DHCP/DNS/TCP states) and concurrency (T1 to T4) (see docs/roadmap/quality.md#47-audit-2026-09-17)
- [ ] InternetConnectionImpl.saveAdapterState `.get()` on the server thread; ContainedDeps and sedna-buildroot version drift (see docs/roadmap/quality.md#37-comprehensive-audit)
- [ ] Guest CPU architectures in OC2R: keep RISC-V as default; add selectable Z80 via Sedna `Z80Board` and CP/M firmware, persisting architecture on the CPU item and through VM save/load. Lua is a software runtime, not a CPU architecture. After the Z80 vertical slice, evaluate adding x86 guest emulation (not an x86 host/build target) based on an emulator backend, board/peripheral compatibility and VM snapshot support. (clarified 2026-10-16)
  - [x] Z80 core spike (2026-10-16): host test proves Sedna `Z80Board` executes guest code from mapped 64 KiB RAM, writes the expected byte and halts.
  - [ ] Z80 vertical slice: board-neutral VM lifecycle and state, architecture selection, CP/M boot/terminal and save/load coverage; then assess x86 emulator backend options.
- [ ] Driver layer: every new device in two faces, RPC (Lua/Python/C) and native (MMIO/VirtIO with a guest driver for Linux); pilot with the redstone interface (see docs/roadmap/devices-storage.md#50-driver-layer-every-device-reachable-without-lua)
- [ ] Wireless card, wireless block and a positioning service (see docs/roadmap/network-overhaul.md#51-wireless-networking-and-positioning)
- [ ] Pocket computer, shared item base with the tablet (see docs/roadmap/devices-storage.md#52-pocket-computer)
- [ ] Robot as a full worker: fuel, tool slots, upgrades, more actions, after the section 28 fixes (see docs/roadmap/robot.md#53-robot-as-a-full-worker)
- [ ] Printer and paper, media player, redstone relay, command computer (see docs/roadmap/devices-storage.md#54-more-peripherals)
- [ ] Uniform inventory/fluid/energy devices with push/pull and stable method names (see docs/roadmap/devices-storage.md#55-uniform-block-access-inventory-fluid-energy)
- [ ] Software shipped with the image: help, everyday tools, robot programs, first-run hint (see docs/roadmap/devices-storage.md#56-software-shipped-with-the-image)
- [x] Fix `:26.1.2:runGameTestServer` crashing in the first computer test with `Payload oc2r:computer_bus_state_message may not be sent to the client!` (the mock-player channels are now configured before the mod payload broadcasts; the 26.x suites pass 40/40 on 26.1.2 and 40/40 on 26.2 — see [stage3-acceptance.md](docs/roadmap/stage3-acceptance.md))
- [ ] Addon API registration without `DeferredRegister`, working on both loaders (see docs/roadmap/multiloader.md#58-addon-api-on-both-loaders)
- [ ] Icebox: OC2R outside Minecraft, standalone or browser runner (see docs/roadmap/icebox.md#57-oc2r-outside-minecraft)
- [ ] Icebox: resizable screen (#12), projector rendering ideas, buildroot image rebuild, block area enlarge (see docs/roadmap/icebox.md)

## Conventions

## Demo request — 2026-10-18

- [ ] Complete the 26.1.2 OnyxOS demo arena, guest Internet/LAN/display/robot checks, terminal investigation, server audit, fenced manual code and CHANGELOG update; detailed checklist: [demo plan](docs/roadmap/demo-arena-26.1.2.md).
- [ ] The demo code is not covered by CI: `scripts/demo/` (about 5300 lines of C, shell and Java plus the Onyx kernel patches) is built and checked by hand only — no Gradle task and no workflow calls it. The demo addresses are hardcoded in the demo sources and docs (the public demo server and its ports in `scripts/demo/README-Onyx-network.md`, the QEMU/LAN addresses in `scripts/demo/qa/NetworkPair.java`; `netcfg` itself takes the addresses as arguments) — move them into a local config file or environment variables, and add at least a build/smoke step to CI.

## Conventions (continued)

- One line per task; long context goes into the pages under `docs/roadmap/`.
- Status boxes: `[ ]` open, `[x]` done, `[~]` partly done.
- Section numbers are stable: code and commit messages reference them, never renumber (26a/26b replace the duplicated 26).
- Commit messages must not contain bullet lists of squashed commits.

## Follow-up after Fabric 26.x acceptance

- [ ] Complete moving-construction and Velocity backend-switch integration acceptance; see [upstream follow-up](docs/roadmap/upstream-followup-2026-10-25.md). Existing Config API Port sync is present; reproduce before adding another channel.
- [x] Assess Sedna 5.3.0 separately: board snapshot migration, reset regressions and bundled Linux cold boot checked; see [assessment](docs/roadmap/sedna-5.3-assessment.md).
- [x] Upgrade development to Sedna 5.3.0 separately: six builds, 1697 unit tests, 372 OC2R GameTests and six live 26.x computer/robot migration profiles pass; retain the known invalid original 26.1 checkpoint result separately. See [upgrade acceptance](docs/roadmap/sedna-5.3-upgrade.md). No release publication.
- [ ] Before a future release, run dedicated legacy 1.21.1 live-client migration; automated 1.21.1 build/unit/server coverage already passes.

- [x] Run current-source Onyx through real OC2R firmware, live monitor and private root on Fabric 26.1.2/26.2; full-root snapshot/create/list/mutate/rollback and rollback after world reload pass with OnyxKernel `54c4f34` on both versions; see [runtime follow-up](docs/roadmap/onyx-runtime-followup.md).

## Active development order — release deferred

1. Completed live acceptance: the OC2R shutdown/final-save fix passes six RGB/world-reload cycles with exact CPU/RAM comparison. All six server regression suites pass (354 OC2R GameTests); see [runtime follow-up](docs/roadmap/onyx-runtime-followup.md).
2. Completed: OnyxKernel `54c4f34` removes the format limit; full-root and persisted archive checks pass on the actual OC2R private disk. Legacy archives remain readable; old-writer downgrades are unsupported.
3. Completed local acceptance: Sedna 5.3.0 with Buildroot 0.2.10 retained; old 5.2 world/robot RAM, private disks, framebuffers, repeated reboot and saved power-off state checked. Pre-existing stopped-disk data loss fixed separately; see [results and scope](docs/roadmap/sedna-5.3-upgrade.md).
4. Terminal double-size rendering and the scoped vttest regressions; then DELTA acceptance and jcodec/H.264 removal; then VXLAN security, robot fixes and network work from their subsystem plans.

Each fix has separate verification. No release tag or platform publication is part of this development sequence.
