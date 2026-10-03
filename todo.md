# TODO

Index of open work. Details live in [docs/roadmap/](docs/roadmap/README.md) (English pages by subsystem; section numbers are stable and map to pages in the README there).

| Page | Topic |
|---|---|
| [terminal.md](docs/roadmap/terminal.md) | VT100 terminal, diff sync, double-size, vttest |
| [video-gpu.md](docs/roadmap/video-gpu.md) | Monitors, GPU, video codec, jcodec removal |
| [network-inet.md](docs/roadmap/network-inet.md) | Internet card, VXLAN, security hardening |
| [network-overhaul.md](docs/roadmap/network-overhaul.md) | Wires, towers, tablet, Create Radars |
| [cables-bus-energy.md](docs/roadmap/cables-bus-energy.md) | Bus cable, energy, world sync |
| [devices-storage.md](docs/roadmap/devices-storage.md) | Cards, disks, input, flash builder, OnyxOS |
| [robot.md](docs/roadmap/robot.md) | Robot fixes |
| [multiloader.md](docs/roadmap/multiloader.md) | NeoForge + Fabric, GameTest CI |
| [quality.md](docs/roadmap/quality.md) | Lint, tests, audits, docs |
| [upstream.md](docs/roadmap/upstream.md) | Upstream fnuecke/oc2 sync |
| [qa-checklist.md](docs/roadmap/qa-checklist.md) | In-game verification boxes |
| [icebox.md](docs/roadmap/icebox.md) | Vague or blocked backlog |
| [done.md](docs/roadmap/done.md) | Closed work |

## Open tasks

- [ ] Verify all "code ready" features in game (multi-monitor, GPU tiers, CPU tiers, codecs, disks, cables, speaker) (see docs/roadmap/qa-checklist.md)
- [ ] Merge multiloader stage 2 (branch feat/multiloader-stage2, Fabric module + loader-independent bridges); then multiversion and release matrix (stages 3 and 4, feasibility in docs/roadmap/multiversion-26-neoforge.md and multiversion-26-fabric.md) (see docs/roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion)
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
- [ ] Monitor/projector state duplicate sync and VS2 dimension filtering (see docs/roadmap/cables-bus-energy.md#29-world-sync-fixes)
- [ ] Robot: verify and fix thread race in RobotActionProcessor (needs manual check) (see docs/roadmap/robot.md#28-robot-fixes)
- [ ] Robot: collider chewing terrain, event handler leak, place() item consumption, export loses VM state (see docs/roadmap/robot.md#28-robot-fixes)
- [ ] Keyboard/mouse capture and link mechanics (see docs/roadmap/devices-storage.md#15-keyboard-mouse-and-input-rework)
- [ ] UART tablet wire minigame and mirror terminal (shares an item base with the tower tablet) (see docs/roadmap/devices-storage.md#16-uart-tablet-item)
- [ ] 3D floppy model for DiskDriveRenderer (see docs/roadmap/devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code)
- [ ] Flash/OS image writer: flasher GUI + OS Loader block with two sources (config file and URL manifest) (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [ ] `flashMemorySize` config instead of hard-coded 12 MB (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [ ] OnyxOS: verify sedna FDT, PLIC/CLINT, virtio numbering, RAM needs, layout minux (see docs/roadmap/devices-storage.md#26b-onyxos-in-oc2r-s-mode-boot-onyxfs-disk-network)
- [ ] OnyxOS repositories: release workflow, oc2r-firmware.json, README section (see docs/roadmap/devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image)
- [ ] Upstream #294 NeoForge startup crash, #230 projector blank display, #288 sound attenuation (needs repro) (see docs/roadmap/upstream.md#413-not-applicable-or-already-closed)
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
- [ ] Icebox: resizable screen (#12), projector rendering ideas, buildroot image rebuild, block area enlarge (see docs/roadmap/icebox.md)

## Conventions

- One line per task; long context goes into the pages under `docs/roadmap/`.
- Status boxes: `[ ]` open, `[x]` done, `[~]` partly done.
- Section numbers are stable: code and commit messages reference them, never renumber (26a/26b replace the duplicated 26).
- Commit messages must not contain bullet lists of squashed commits.
