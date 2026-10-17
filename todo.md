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

- [ ] Verify all "code ready" features in game (multi-monitor, GPU tiers, CPU tiers, codecs, disks, cables) (see docs/roadmap/qa-checklist.md)
- [x] Speaker audio on NeoForge 26.1.2 (2026-10-16): real guest RPC beep produced 880 Hz for 500 ms; fixed PCM playback from one packet to all 44 (0.51042 s captured versus 0.51084 s supplied, delivered over about 10 s by RPC). Preload, 100 ms read cap and manual underrun recovery are covered by regression tests. Full suites pass: 482 tests on 26.1.2 and 478 on 1.21.1. Evidence in [build/reports/render-qa](build/reports/render-qa); details in [multiloader rendering notes](docs/roadmap/multiloader.md#26x-rendering-notes-2026-10-14).
- [x] World shutdown persistence: prevent post-unload empty-chunk overwrite; restore 26.x scalar NBT strings and rebuild item devices after level assignment. Real save/reload preserves 12 block entities, eight computer components and guest root writes (2026-10-16).
- [ ] Verify warm CPU/RAM continuation and automatic framebuffer retention after world reload; the current live check boots to guest login with hardware and root writes preserved.
- [x] Speaker follow-up: cached PCM streams are dropped on world exit and when the sound engine stops ticking them (2026-10-17); in-game check of both lifecycle transitions still open (see docs/roadmap/multiloader.md)
- [~] Finish multiversion stage 3 (branch multiversion-stage3): 1.21.1 is complete on both loaders; finish 26.1.2/26.2 rendering and acceptance as listed in [multiloader.md](docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion), then proceed to stage 4 release matrix.
  - [x] Port in-world terminal row-mesh construction to 26.2 `BufferBuilder`/`ByteBufferBuilder`; 26.1.2 and 26.2 compile.
  - [x] Port 26.2 network cable rendering to the level submit pipeline; both 26.1.2 and 26.2 compile.
  - [x] Verify 26.2 network cables and terminal backgrounds/glyphs/cursor in game (2026-10-16). Fixed the terminal text pipeline's forward-depth comparison for 26.2 reversed-Z; the in-world screen now shows glyphs, colored text/background and the blinking cursor. Screenshot: `build/reports/render-qa/26.2-terminal-world-after-depth-fix.png`.
- [x] Sedna 5.2.0 + buildroot 0.2.10 + upstream §49 ports (branch feat/sedna-5.2; upstream master moved past the 5.1.0 recorded before — ported 5.2.0/0.3.0/0.2.10 instead; see docs/roadmap/upstream.md#49-fnuecke-oc2-since-f37735d-checked-2026-10-08)
- [ ] Remove the `required = false` flag from 9 GameTests after fixing them (see docs/roadmap/multiloader.md#45-gametest-ci-a-real-baseline)
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
- [ ] Projector follow-up: entity/particle/block-entity shadow depth on 26.1.2, port depth/compositing to 26.2. Real guest framebuffer and server energy drain/restoration verified on 26.1.2 (see docs/roadmap/multiloader.md)
- [x] 26.1.2 real display path: guest Linux drives monitor and projector together, monitor GUI shows the same framebuffer, and real generator removal/restoration disables/restores both images; authoritative monitor power and per-device frame delivery fixed (2026-10-16)
- [x] Monitor floating framebuffer: align display/status plane with cube model and separate support shape from selection/collision; side view and multiblock geometry regression verified (2026-10-16)
- [ ] Monitor/projector state duplicate sync and VS2 dimension filtering (see docs/roadmap/cables-bus-energy.md#29-world-sync-fixes)
- [ ] Robot: verify and fix thread race in RobotActionProcessor (needs manual check) (see docs/roadmap/robot.md#28-robot-fixes)
- [ ] Robot: collider chewing terrain, event handler leak, place() item consumption, export loses VM state (see docs/roadmap/robot.md#28-robot-fixes)
- [ ] Keyboard/mouse capture and link mechanics (see docs/roadmap/devices-storage.md#15-keyboard-mouse-and-input-rework)
- [ ] UART tablet wire minigame and mirror terminal (shares an item base with the tower tablet) (see docs/roadmap/devices-storage.md#16-uart-tablet-item)
- [ ] 3D floppy model for DiskDriveRenderer (see docs/roadmap/devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code)
- [~] Flash/OS image writer: local-file and bundled firmware GUI implemented; verify client flow, then consider URL manifests (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [ ] `flashMemorySize` config instead of hard-coded 12 MB (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [x] Bug on 26.1.2 client: the computer screen stayed completely black (found in the first in-game run, 2026-10-11); fixed 2026-10-14 in b7d2dcbc — the in-world terminal is now replayed through `submitCustomGeometry` (`ModRenderType.getTerminalText/getTerminalCursor`), verified in game: the PC screen shows terminal output. The monitor block renders its ported `submitCustomGeometry` path correctly too (it shows the "not enough energy" status); showing a picture on it just needs in-game energy + a mounted computer, not a render fix. The GUI terminal (computer/robot screen) is drawn through `GuiElementRenderState` (90bfeb45, compiles, still to be checked in game) (see docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion, 26.x rendering notes)
- [x] 26.1.2 GUI terminal: capture pose/scissor at extraction, separate solid backgrounds/text/cursor, and snapshot cursor geometry; computer and robot GUI visually checked on 2026-10-16 (see docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion, 26.x rendering notes)
- [x] Bug on 26.1.2 client: network cables are not visible, the connection sounds play but there is no cable model (found 2026-10-11); fixed 2026-10-14 in b7d2dcbc — vanilla sends block entity data on a block update only when the entity supplies a packet, and `NetworkConnectorBlockEntity` returned null, so live connections on an already-loaded chunk never reached the client; added `getUpdatePacket()` (as `BusCableBlockEntity` already had for facades); verified in game: cables draw between connectors. The separate "old items stopped working until replaced" note is not reproduced yet
- [ ] Wire in or delete the dead qodana task; CI upload reports `if: always()` (see docs/roadmap/quality.md#39-lint-stages)
- [ ] Lint stage B (failBuild, checkstyle severity=error, SpotBugs baseline) and stage C (Error Prone on) (see docs/roadmap/quality.md#39-lint-stages)
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
- [ ] Addon API registration without `DeferredRegister`, working on both loaders (see docs/roadmap/multiloader.md#58-addon-api-on-both-loaders)
- [ ] Icebox: OC2R outside Minecraft, standalone or browser runner (see docs/roadmap/icebox.md#57-oc2r-outside-minecraft)
- [ ] Icebox: resizable screen (#12), projector rendering ideas, buildroot image rebuild, block area enlarge (see docs/roadmap/icebox.md)

## Conventions

- One line per task; long context goes into the pages under `docs/roadmap/`.
- Status boxes: `[ ]` open, `[x]` done, `[~]` partly done.
- Section numbers are stable: code and commit messages reference them, never renumber (26a/26b replace the duplicated 26).
- Commit messages must not contain bullet lists of squashed commits.
