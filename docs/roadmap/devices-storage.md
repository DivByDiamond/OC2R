# Roadmap: devices, storage, input, firmware images

Cards, disks, CPU tiers, keyboard/mouse, tablet, flash builder and OS image delivery. In-game checks for sections 22 and 24 are in [qa-checklist.md](qa-checklist.md).

## 1. C API for Redstone Interface (#89)

Problem: Lua on the VM is slow, has no `sleep()`, and is inconvenient for real-time controllers; C/Rust for RISC-V is wanted. State: C library `librpc` in `src/main/scripts/lib/rpc/`. All items done:

- [x] TCC in the buildroot image: already there. sedna-buildroot 0.0.64 (pinned in `gradle.properties`) contains `/usr/bin/tcc` (297 KB) + `/usr/lib/tcc/include/` in `rootfs.cramfs` (checked by jar contents); see `docs/BUILDROOT.md`.
- [x] Examples: `src/main/scripts/lib/rpc/redstone_blink.c`, `src/main/scripts/lib/rpc/note_block_player.c` (+ updated `Makefile`).
- [x] C++ RAII wrapper: `src/main/scripts/lib/rpc/rpc_raii.hpp` (`rpc::Bus`/`rpc::Device`, destructors close the bus) + demo `example_raii.cpp`; `rpc_device_invoke_*_raw` added to `rpc.c`/`rpc.h`.

## 3. TCC in the image

- [x] Update minux to include TCC: done upstream, the minux (buildroot) config contains `BR2_PACKAGE_TINYCC=y`, release 0.0.64 includes tcc; the version is pinned in `gradle.properties`/`download-libs.sh`.
- Building a new buildroot image needs a toolchain/Docker for minux outside this repo (instructions: `docs/BUILDROOT.md`): moved to [icebox.md](icebox.md#3-buildroot-image-rebuild).

## 15. Keyboard, mouse and input rework

- [x] **Combined keyboard/mouse block and linking**: keyboard and mouse are one block. Shift+RMB selects and highlights it, then RMB on a computer, monitor or projector establishes the link: "linked successfully". Persist the link until either endpoint is broken.
- [x] **Control capture**: RMB on the keyboard/mouse block captures control (cursor capture).
- [x] **Input logic**: once control is captured, the player aims (raycast) at the wanted monitor and input focus moves to it. All key presses and mouse clicks are forwarded straight to the computer (mechanic as in Tom's Peripherals).
- [x] **Release capture**: Right Ctrl by default, remappable in Minecraft's Controls settings. Reserve the configured release key for leaving input mode; Escape and mouse buttons remain available to the guest. Release held guest keys/buttons when leaving capture or changing focus.

Accepted on 2026-10-28 across NeoForge and Fabric 1.21.1, 26.1.2 and 26.2.
Native Onyx input, font and completed-frame evidence is recorded in
[onyx-input-display-acceptance.md](onyx-input-display-acceptance.md).

## 16. UART tablet (Item)

Part of the tablet item family with two modes sharing one item base: this UART mode and the tower/radio mode of [section 48](network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars).

- [ ] **RMB with the tablet on a computer block**: opens the components GUI, but in the top-right corner there is a connection minigame: 3 pins (RX, TX, GND) and 3 pull-out wires. The user must connect the wires to the pins (like the wires task in Among Us). Once connected correctly, the GUI closes and the link is established.
- [ ] **RMB with the tablet in the air**: opens the tablet terminal GUI that mirrors what the connected computer's UART is currently outputting.

## 22. Disks: cleanup, tiers, 3D models, dead code

- [x] Orphaned blobs cleanup: `BlobStorage.ACTIVE_HANDLES` (registered on `validateHandle`), `cleanupOrphaned()` on `ServerStartedEvent` via `ServerScheduler` with a 5 s delay.
- [x] `HardDriveWithExternalDataItem` was not dead code (checked 2026-09-15): registered as `HARD_DRIVE_ONYXOS` (`Items.java:97`), provider `hard_drive_custom` in `ProviderRegistry`, recipe (`StorageRecipes.java:178`), model, color, creative tab entry.
- [x] HDD sizes per tier: `diskSizeTier1/2/3/4` (8/16/32/128 MB) in `VMSpec`/`Config` replace `diskSizeFactor`; new HDD tiers 8/16/32/128 MB (were 2/4/8/16).
- [x] Flash memory tiers: `flashMemorySizeTier1/2/3` (4/8/16 MB) in config; new items `flash_memory_small`/`flash_memory_medium`, existing `flash_memory` = 16 MB.
- [ ] **3D floppy model**: the disk drive slot draws a 2D icon (`FIXED` display context). Add a proper 3D floppy model for `DiskDriveRenderer`.
- [ ] Build + in-game check (hot-swap, data persistence, cleanup): see [qa-checklist.md](qa-checklist.md#from-section-22).

## 24. CPU: frequency config, new tiers, governors

Done: new CPU tiers (50/100/200/400/1000 MHz), config `cpuFrequencyTier1/2/3/4` in `GameplaySpec`, `Config.vmTimeQuotaMs` instead of hard-coded 25 ms, `cycleLimit` cap (at most 2 ticks ahead), updated crafts.

- [ ] Build + in-game check (different CPUs, energy/performance balance): see [qa-checklist.md](qa-checklist.md#from-section-24).
- [ ] CPU governors (named in the section title, no separate item existed): decide whether to implement.

## 25. Flash Builder: GUI, repository manifest, custom image

Verdict: option A (whole image) + a GitHub allowlist + `flashMemorySize = 15 MB` in config.

**Combined item with [section 30](#30-os-loader-block-flash-writer-block)**: one "flash/OS image writer" feature with two sources: (1) a local config-file image (`config/oc2r/*.bin|*.img`, section 30) and (2) a URL/repository manifest download (this section).
Both write a flash/HDD item with the chosen OS. Details of each source are kept in its own section.

Problem: writing custom firmware currently needs the guest OS and `flash.sh` (no GUI, no URL loading). 12 MB is hard-coded in 5 places.

### Mod (oc2r)

- [ ] **`Config.flashMemorySize = 15`** (MB) in `StorageSpec`/`Config`, read everywhere instead of hard-coded 12 MB: `Items.java` (flash_memory), `ByteBufferFlashStorageDevice` (`claimMemory`/`allocate`), `FlashMemoryFlasherDevice`, `MinuxFirmware`, `src/main/scripts/bin/flash.sh`.
- [ ] **Flasher GUI** (`flash_memory_flasher`): container + screen (currently no GUI at all, only physical insert/eject). Field "repository URL" + a "Write" button.
- [ ] **`FirmwareManifest`** (manifest parser): reads `oc2r-firmware.json` -> `{name, version, layout, image}`.
- [ ] **`FirmwareDownloader`**: downloads the manifest and the `.img` through `java.net.http.HttpClient` (async, on a worker pool), **allowlist**: only `github.com`, `raw.githubusercontent.com`, `objects.githubusercontent.com` (host list in config).
- [ ] **Flash image assembly**: `layout` = `minux`: OpenSBI (`fw_jump.bin` from the jar) at offset 0 + the image at offset 2 MB, zero tail up to `flashMemorySize`; `layout` = `raw`: image as is. Written into a blob (`BlobStorage`), handle -> item NBT.
- [ ] **Error handling**: network unavailable / invalid manifest / size > flashMemorySize -> message in the GUI, the flash item is not damaged.
- [ ] **Example `oc2r-firmware.json`** in `docs/` as a community template.
- [ ] Build + in-game check (flasher GUI, writing an OnyxOS image, booting a computer from it).

### OnyxOS repositories (example for people)

- [ ] `.github/workflows/release.yml`: on tag push build boot (riscv-gcc) + kernel (cargo) + shell; glue `onyx-flash.img` (OpenSBI -> 0, kernel -> 2 MB, zeros up to 15 MB); attach to the GitHub Release.
- [ ] `oc2r-firmware.json` at the repo root:
```json
{
  "name": "OnyxOS",
  "version": "0.3.0",
  "layout": "minux",
  "image": "https://github.com/loki5512344/OnyxOS/releases/latest/download/onyx-flash.img"
}
```
- [ ] README section "How to make your own firmware" (manifest + workflow as an example).

## 26b. OnyxOS in OC2R: S-mode boot, OnyxFS disk, network

2026-10-16 update: the Onyx firmware now selects an automatic per-computer
writable root image, so an HDD item is optional. Rebuilt Onyx boots at 64 MiB
in Sedna and exposes independent `/dev/fbN` devices. FDT traversal, framebuffer
devfs writes, standard-stream redirection and VirtIO disk capacity were fixed.
Current build, usage and verification details are in [ONYXOS.md](../ONYXOS.md).
The notes below retain the earlier integration history.

(Original number 26; the first section 26 is now [26a](video-gpu.md#26a-video-codec-switchable-raw-and-h264).)

Goal: run OnyxKernel (github.com/loki5512344/OnyxKernel) inside an OC2R VM as an alternative to Minux. Status 2026-09-15: all three former blockers are closed and OnyxOS really boots in OC2R.
This section is kept as diagnosis history (the diagnosis was right, everything is solved since).

Verified facts: `linker.ld` `KERNEL_BASE = 0x80200000` matches the kernel load address in `MinuxFirmware` (`startAddress + 0x200000`), so `layout: minux` fits as is. The M-mode `boot.S` blocker is solved by
`boot_smode.rs` (a separate S-mode entry point that parks secondary harts, zeroes BSS and jumps into `kmain`). The rootfs is delivered as OnyxFS (`src/main/resources/onyxos/onyxfs.img`) through
`HARD_DRIVE_ONYXOS`/`OnyxOSBlockDeviceData` (see [section 22](#22-disks-cleanup-tiers-3d-models-dead-code)). The hard-coded network `[10,0,2,15]` is solved: `srv/main/mod.rs` tries DHCP first, skips it
explicitly when there is no virtio-net device (comment "OC2R/sedna has none"), and falls back to static `10.0.2.15/255.255.255.0`, which matches the point-to-point model of the OC2R network card (see section 27).

Closed OnyxKernel items (2026-08-23): login incorrect for root fixed in `OnyxKernel/init/src/login/mod.rs` (the OC2R terminal sends Enter as `'\r'`, TerminalInput.java:15; both CR and LF are stripped now; root is seeded with an EMPTY
password in `seed.rs` and a bare Enter is accepted; rebuild the image with `cargo ibuild` + OnyxOS `scripts/mk-onyxfs-disk.sh`, re-flash a fresh image on existing disks); `boot_smode.rs` (`--features smode`);
network hard-code removed; monitor output: `libfdt/fdt/framebuffer.rs` finds `/chosen/simple-framebuffer` (compatible/reg/width/height/stride, MMIO below 0x80000000), `fb::init_device()` takes MMIO geometry,
`put_pixel` handles r5g6b5 (16bpp LE), `draw`/`writer`/devfs use dynamic sizes, `srv/main/display.rs` tries the FDT fb first with a RAM fallback for QEMU; `onyx-kernel.bin` rebuilt and replaced in the mod resources.
Mod side: the OnyxFS disk is delivered as a virtual HDD (`HARD_DRIVE_ONYXOS`/`OnyxOSBlockDeviceData`, override via `config/oc2r/onyxfs.img`); a computer with the OnyxOS disk boots to `login:` with OnyxKernel (checked in game).

Open:
- [ ] Check: UART NS16550A (compatible with sedna), virtio_net, virtio-blk, libfdt: that the sedna FDT is parsed by `early_init`.
- [ ] `layout: minux` in `FirmwareManifest`/`FirmwareDownloader` ([section 25](#25-flash-builder-gui-repository-manifest-custom-image)) already places the kernel at 0x80200000: verify with a real Onyx image.
- [ ] Memory: how much RAM OnyxOS needs (256 MB in QEMU) vs OC2R `maxAllocatedMemory` (512 MB default): does it fit.
- [ ] sedna interrupt controller: OnyxKernel uses PLIC + CLINT; do they exist in sedna and how are they passed in the FDT.
- [ ] VirtIO numbering `/dev/vda|vdb|vdc` (sedna: vda=bootfs, vdb=rootfs, vdc=first HDD): where the OnyxFS disk lands, any conflict.
- [ ] GPU/framebuffer: OnyxKernel draws PSF fonts into the framebuffer; check the `r5g6b5` format now that GPU tiers (section 23) exist.

## 30. OS Loader block (Flash Writer block)

A separate block into which flash memory is placed and a path/URL to a firmware image is given; the output is an item (flash/HDD with the flashed OS), with no need to rebuild the mod or touch datapacks/the server.
Part of the combined item described in [section 25](#25-flash-builder-gui-repository-manifest-custom-image) (two sources: config file and URL).

Why: OnyxOS images are currently baked into the jar (or read from `config/oc2r/`). The player should be able to "flash their own OS" in game: insert a flash/disk, point to an image source, get an item.
No mod rebuild for a new OS, no server permissions (the player works with items in their own world).

- [x] Reuse the flash writer block: normal empty-hand use opens its GUI; shift-use ejects the medium.
- [x] Select a local flat RISC-V `.bin` or raw disk `.img` inside Minecraft, without a datapack or server file path.
- [x] Write a blank flash chip or writable HDD via bounded 16 KiB chunks, acknowledgments and final SHA-256 verification; keep the original on failure.
- [x] Select bundled Minux/Onyx firmware directly. Onyx provides its private persistent root disk automatically.
- [x] Persist image bytes as world-local blobs with NBT references; exclude simultaneous guest mounting and item extraction while writing.
- [x] Server GameTest covers authorization, transfer, lease, NBT restoration and reopened blob contents (26.1.2).
- [x] Real Minecraft GUI/file chooser, raw Minux flash boot, built-in Onyx boot, capacity errors, cancelled upload retaining the previous image, and written media surviving reload in the player inventory.
- [ ] URL/manifest sources remain a separate extension; local-file import does not require them.

## 50. Driver layer: every device reachable without Lua

Today there are two ways for a guest program to talk to a device: the RPC layer (JSON over a VirtIO serial port, used by Lua, MicroPython and `librpc` in C) and the `VMDevice` layer (MMIO regions, interrupts, device tree nodes, used by disks, keyboard, monitor, NIC). Most game-specific devices exist only in the RPC form: redstone interface, inventory/fluid/energy access, robot and its modules, sound card, scanner. A program with a kernel driver, or a custom OS such as OnyxOS, cannot use them without speaking the RPC protocol.

Goal: a new device is described once and offered in two faces, so a user chooses the way to program it.

How it should work:
- **RPC face** (as now): method list, JSON, no driver. Best for scripts, quick experiments and teaching.
- **Native face**: the same operations as a small register/queue interface exposed as a `VMDevice` with a device tree node and an interrupt. The guest reaches it through a driver. Three tiers of driver, cheapest first:
  1. Reuse a standard VirtIO class where one fits (block, net, input, console, 9p) so the stock Linux driver and OnyxKernel's VirtIO code work unchanged.
  2. A userspace driver library (`mmap` of the device region through `/dev/mem` or UIO, plus an interrupt via `/dev/uioN`) in C, with Rust bindings. No kernel rebuild needed, works on any Linux image.
  3. A real kernel driver (in-tree for the bundled Linux image, in OnyxKernel for OnyxOS) exposing a normal interface (`/dev/...`, sysfs, input events, ALSA-style nodes).
- One register specification per device in `docs/DEVICES.md` (offsets, semantics, interrupts, versioning) is the contract that all drivers follow. Both faces share one Java implementation underneath, so behaviour (cooldowns, energy cost, permissions) cannot diverge.

Pros: real OS-level programming (drivers, daemons, filesystems for game devices); other operating systems can be written against a documented spec; Lua stays as the easy path; the same game rules apply on both paths.

Tasks:
- [ ] Write the register spec format and put the existing native devices (disk drive, keyboard, monitor, NIC) into `docs/DEVICES.md` as the reference.
- [ ] Check which VirtIO classes the bundled Linux image and OnyxKernel already enable (sedna-buildroot config) and which of them sedna provides.
- [ ] Pick the pilot device with the simplest semantics (redstone interface) and build both faces: a `VMDevice` plus a userspace C library plus a `/dev` driver for the bundled Linux and for OnyxOS.
- [ ] A shared `DeviceBackend` abstraction in the Java code so the RPC adapter and the MMIO adapter call the same logic.
- [ ] Roll out to the other devices by value: inventory/fluid/energy access, sound card, robot, scanner, then every device added in sections 51 to 55 ships with both faces from day one.
- [ ] Decide how a missing guest driver is reported (device present in the device tree but unclaimed) and document it.

## 52. Pocket computer

A handheld computer item: the player carries a full VM with a small screen and a keyboard, opened in a GUI from the item.

How it should work: an item with a computer inventory (CPU, memory, flash, one or two card slots), a terminal screen opened by right click, a power state that persists in the item data and runs only while the owner holds it (or in a background mode with a cost). It uses the same VM, terminal and device code as the block computer, with a device bus that exposes only the item's own components plus the wireless card from [section 51](network-overhaul.md#51-wireless-networking-and-positioning).
It is the same item base as the tablet in [section 16](#16-uart-tablet-item) and [section 48](network-overhaul.md#48-network-overhaul-wires-towers-tablet-create-radars): the pocket computer is the general mode, the UART and tower modes are special apps of it.

Pros: programmable tools in the field (GPS client, remote control of robots and doors, a terminal for any computer in range); a natural user of the wireless network; low extra code because the VM, GUI and persistence already exist.

Tasks:
- [ ] Item, data components for state and inventory, power/energy model (battery in the item).
- [ ] Reuse `ComputerTerminalManager` and the VM lifecycle for a non-block owner; define what happens on drop, death and dimension change.
- [ ] GUI screen with a compact terminal and a slot panel.
- [ ] Persistence on both loaders and the multiversion nodes.
- [ ] Recipes, manual pages and a game test (boot, save, reload).

## 54. More peripherals

Each device here follows [section 50](#50-driver-layer-every-device-reachable-without-lua) (RPC face and native face).

- [ ] **Printer and paper**: a block that takes ink and paper and prints pages from a program. Pages are items with text and a title that can be bound into a book and read in game. Pro: physical output, documentation and in-world signs without screens.
- [ ] **Media player (record drive)**: a drive that plays music discs and custom audio from a program; audio is streamed as compressed PCM through the existing sound card path. Pro: music, announcements and alarms that share one audio pipeline with the sound card.
- [ ] **Redstone relay**: a separate block on the bus with its own six redstone sides, so one computer controls many places through a cable instead of needing an interface card at each location; analog and bundled output. Pro: cheap automation of large builds.
- [ ] **Command computer**: an operator-only block that can run server commands from a program; only creative-mode operators can place it, gated by a permission and a config flag (off by default on servers). Pro: map makers and admins script worlds.
- [ ] **Lectern/pedestal link** and other display peripherals as follow-ups once the printer exists.

## 55. Uniform block access (inventory, fluid, energy)

Goal: any block that exposes an item, fluid or energy capability becomes a device on the bus without a per-mod adapter, with predictable method names so scripts for one storage block work for all of them.

How it should work: one generic provider builds a device from the capability (as `ItemHandlerBlockDeviceProvider`, `FluidHandlerBlockDeviceProvider` and `EnergyStorageBlockDeviceProvider` already do) and offers a consistent set: list/size/get item detail, push and pull items between two bus inventories, tanks and fluid transfer, stored energy and capacity. Names and result tables stay the same across blocks and are documented once.

Tasks:
- [ ] Audit the three providers and list which operations are missing against the set above.
- [ ] Add push/pull between two devices on the same bus (transfer without moving through the computer).
- [ ] Standardise names and result shapes, add documented examples for Lua, Python and C.
- [ ] Expose the same operations on the native face ([section 50](#50-driver-layer-every-device-reachable-without-lua)).
- [ ] Tests with vanilla chests, furnaces and a mod storage block.

## 56. Software shipped with the image

Goal: a new player should do something useful in the first five minutes without knowing Linux.

How it should work: the bundled Linux image and OnyxOS ship a small curated set of programs and a help system, mounted read-only from the mod (the `/mnt/builtin` share already exists):
- `help` with offline pages for every device (generated from the same documentation as the in-game manual and from `@Callback` descriptions), and `lsdev` improved to show which face (RPC or native) each device has.
- Everyday tools: a friendly editor and file manager, `monitor` (mirror a terminal to a monitor block), `chat` and `gps` clients for [section 51](network-overhaul.md#51-wireless-networking-and-positioning), `wget` with an allow-list for the internet card, a package fetcher that installs from the URL manifest of [section 25](#25-flash-builder-gui-repository-manifest-custom-image).
- Robot programs: dig a tunnel or quarry, go to coordinates, refuel, build a line or a wall ([section 53](robot.md#53-robot-as-a-full-worker)).
- A "first run" prompt that prints the three commands to try and where the manual is.

Pros: lower entry barrier; the examples double as tested documentation; every program is also a reference for people writing their own.

Tasks:
- [ ] Choose the set, write the programs in Lua and a few in C (so both routes are shown), add them to `src/main/scripts`.
- [ ] Generate `help` pages from the existing manual and device docs in the build.
- [ ] Add a motd and first-run hint to the image.
- [ ] Game test that boots the image and runs each program once.
