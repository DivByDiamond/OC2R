# Roadmap

The old monolithic `todo.md` was split into English pages by subsystem. **Section numbers are stable**: source code, tests and commit messages reference them (for example section 36, 39, 44.5, 47),
so every page keeps the original `## N. Title` headings and `### N.M` sub-headings. The original had two sections numbered 26; they are now **26a** (video codec) and **26b** (OnyxOS in OC2R).
Sections 4 and 5 never existed. Split sections (closed part in `done.md`, open part elsewhere) are marked with "and done.md".

## Pages

| Page | Contents |
|---|---|
| [terminal.md](terminal.md) | VT100/xterm terminal: rendering, diff sync, audits, line drawing, double-size, vttest automation |
| [video-gpu.md](video-gpu.md) | Multi-monitor, GPU items, video codec (RAW/H.264/DELTA), jcodec removal, video performance items |
| [network-inet.md](network-inet.md) | TCP/IP stack and internet card, VXLAN, network performance, security hardening, library notes |
| [network-overhaul.md](network-overhaul.md) | Wires, towers, tablet, Create Radars (section 48) with historical sections 20 and 43 |
| [cables-bus-energy.md](cables-bus-energy.md) | Bus cable, energy transfer, world sync fixes, cable in-game tests |
| [devices-storage.md](devices-storage.md) | Cards, disks, CPU tiers, keyboard/mouse, UART tablet, flash builder / OS loader, OnyxOS delivery |
| [robot.md](robot.md) | Robot fixes and robot audit rows |
| [multiloader.md](multiloader.md) | NeoForge + Fabric multiloader, GameTest CI, dependency updates |
| [quality.md](quality.md) | Lint stages, tests, audits, code documentation, NOPMD review |
| [upstream.md](upstream.md) | Sync with upstream fnuecke/oc2 v0.3.0 |
| [qa-checklist.md](qa-checklist.md) | All "code ready, verify in game" boxes in one place |
| [icebox.md](icebox.md) | Vague or blocked backlog |
| [done.md](done.md) | Closed sections and closed parts of open sections |

## Section map

| Section | Page and anchor | Note |
|---|---|---|
| 0 | [done.md](done.md#0-refactoring-structure-solid-kiss-dry) | done |
| 1 | [devices-storage.md](devices-storage.md#1-c-api-for-redstone-interface-89) | C API for Redstone Interface |
| 2 | [icebox.md](icebox.md#2-resizable-screen-12) | deferred |
| 3 | [devices-storage.md](devices-storage.md#3-tcc-in-the-image) | TCC; image rebuild in icebox |
| 6 | [terminal.md](terminal.md#6-terminal-dynamictexture-rendering) | last item in icebox |
| 7 | [icebox.md](icebox.md#7-projector-rendering-improvements) | |
| 8 | [done.md](done.md#8-screen-and-container-auto-registration) | done |
| 9 | [quality.md](quality.md#9-lint-and-static-analysis) | |
| 10 | [quality.md](quality.md#10-tests) | |
| 11 | [done.md](done.md#11-jcodec-to-maven-dependency-cancelled) | cancelled |
| 12 | [video-gpu.md](video-gpu.md#12-multi-monitor-fragment-model) | in-game check in qa-checklist |
| 13 | [done.md](done.md#13-wire-rewrite-fe-and-eu-energy-connection-fix) | done |
| 14 | [done.md](done.md#14-computer-case-rewrite-block) | done |
| 15 | [devices-storage.md](devices-storage.md#15-keyboard-mouse-and-input-rework) | |
| 16 | [devices-storage.md](devices-storage.md#16-uart-tablet-item) | tablet family, see 48 |
| 17 | [done.md](done.md#17-automatic-vm-restart-when-the-device-set-changes) | done |
| 18 | [done.md](done.md#18-remove-jcodec-raw-rgb-buffer-obsolete) | obsolete |
| 19 | [terminal.md](terminal.md#19-terminal-diff-instead-of-raw-uart-on-the-client) | checks in qa-checklist |
| 20 | [network-overhaul.md](network-overhaul.md#20-push-based-bus-instead-of-polling-bfs-scans-closed) | closed, superseded by 43 |
| 21 | [done.md](done.md#21-sound-tone-generator-pcm-streaming-speaker-block) | open checks in qa-checklist |
| 22 | [devices-storage.md](devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code) | |
| 23 | [video-gpu.md](video-gpu.md#23-gpu-as-an-item-tiers-1-to-4) | |
| 24 | [devices-storage.md](devices-storage.md#24-cpu-frequency-config-new-tiers-governors) | |
| 25 | [devices-storage.md](devices-storage.md#25-flash-builder-gui-repository-manifest-custom-image) | merged with 30 |
| 26a | [video-gpu.md](video-gpu.md#26a-video-codec-switchable-raw-and-h264) | video codec (was the first 26) |
| 26b | [devices-storage.md](devices-storage.md#26b-onyxos-in-oc2r-s-mode-boot-onyxfs-disk-network) | OnyxOS (was the second 26) |
| 27 | [network-inet.md](network-inet.md#27-mod-subsystem-audit) | robot rows in [robot.md](robot.md#27-audit-rows-robot) |
| 28 | [robot.md](robot.md#28-robot-fixes) | |
| 29 | [cables-bus-energy.md](cables-bus-energy.md#29-world-sync-fixes) | and [done.md](done.md#29-closed-items) |
| 30 | [devices-storage.md](devices-storage.md#30-os-loader-block-flash-writer-block) | merged with 25 |
| 31 | [done.md](done.md#31-vt100-terminal-audit-no-1) | done |
| 32 | [quality.md](quality.md#32-block-logic-audit-and-pmd) | and [done.md](done.md#32-closed-items) |
| 33 | [quality.md](quality.md#33-code-documentation-and-nopmd-review) | |
| 34 | [cables-bus-energy.md](cables-bus-energy.md#34-manual-in-game-testing-wires-and-screens) | boxes in qa-checklist |
| 35 | [terminal.md](terminal.md#35-terminal-follow-up-prs) | |
| 36 | [terminal.md](terminal.md#36-vt100-terminal-audit-no-2) | and [done.md](done.md#36-closed-items) |
| 37 | [quality.md](quality.md#37-comprehensive-audit) | and [done.md](done.md#37-closed-items) |
| 38 | [network-inet.md](network-inet.md#38-performance-audit-network-monitors-bus) | video items in [video-gpu.md](video-gpu.md#38-video-items); closed in [done.md](done.md#38-closed-items) |
| 39 | [network-inet.md](network-inet.md#39-security-hardening-of-inet-linter-configs-libraries) | lint stages in [quality.md](quality.md#39-lint-stages); closed in [done.md](done.md#39-closed-items) |
| 40 | [video-gpu.md](video-gpu.md#40-removing-jcodec-full-usage-list) | |
| 41 | [upstream.md](upstream.md#41-upstream-oc2-v030-comparison-and-porting) | |
| 41.1 | [multiloader.md](multiloader.md#411-gametest-ci-and-cd) | GameTest CI/CD |
| 41.2 | [upstream.md](upstream.md#412-targeted-fixes-from-the-release) | fixed items in [done.md](done.md#412-fixed-items); robot row in [robot.md](robot.md#412-robot-rows) |
| 41.3 | [upstream.md](upstream.md#413-not-applicable-or-already-closed) | |
| 41.4 | [multiloader.md](multiloader.md#414-dependency-updates) | |
| 42 | [multiloader.md](multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion) | stage 1 done on branch, pending merge |
| 43 | [network-overhaul.md](network-overhaul.md#43-cable-system-rebuild-from-scratch) | implemented part in [done.md](done.md#43-implemented-device-bus-part) |
| 44 | [terminal.md](terminal.md#44-terminal-line-drawing-strikethrough-double-size) | |
| 44.1 | [terminal.md](terminal.md#441-line-drawing-characters-dec-special-graphics) | done |
| 44.2 | [terminal.md](terminal.md#442-strikethrough-sgr-929) | done |
| 44.3 | [terminal.md](terminal.md#443-double-sized-characters-esc-3-4-5-6) | PR-B open |
| 44.4 | [terminal.md](terminal.md#444-vttest-automation) | |
| 44.5 | [terminal.md](terminal.md#445-remaining-tofu-glyphs) | keep procedural quads until a font with glyphs lands |
| 45 | [multiloader.md](multiloader.md#45-gametest-ci-a-real-baseline) | done, one residual |
| 46 | [done.md](done.md#46-terminal-sweep-follow-ups) | done |
| 47 | [quality.md](quality.md#47-audit-2026-09-17) | terminal items in [terminal.md](terminal.md#47-terminal-items), network items in [network-inet.md](network-inet.md#47-network-items), bus items in [cables-bus-energy.md](cables-bus-energy.md#47-bus-items); closed in [done.md](done.md#47-closed-items) |
| 48 | [network-overhaul.md](network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars) | |
