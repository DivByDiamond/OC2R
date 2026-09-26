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

- [ ] **Connection to the computer/monitor**: Shift+RMB on the keyboard/mouse block (the block highlights), then RMB on a monitor or computer: "linked successfully". The link holds until the monitor or keyboard/mouse is broken.
- [ ] **Control capture**: RMB on the keyboard/mouse block captures control (cursor capture).
- [ ] **Input logic**: once control is captured, the player aims (raycast) at the wanted monitor and input focus moves to it. All key presses and mouse clicks are forwarded straight to the computer (mechanic as in Tom's Peripherals).
- [ ] **ESC** is the only key the keyboard/mouse does not capture: it leaves input mode.

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

- [ ] Block (like `FlashMemoryFlasher` / `DiskDrive`): GUI with slots "image source" (file in `config/oc2r/` or a path in the world folder) and "flash/disk".
- [ ] Input UI: text field for the path to `.bin`/`.img` (for example `config/oc2r/onyx-kernel.bin`, `config/oc2r/onyxfs.img`) + a "Write" button.
- [ ] Output: `flash_memory_onyxos` (flash with firmware=onyxos but the kernel from the given file) or `hard_drive_onyxos` (disk with rootfs from the file).
- [ ] The "external images" mechanism already exists: `OnyxOSFirmware`/`OnyxOSBlockDeviceData` read `config/oc2r/*` with a jar fallback (commit `0b90b3b`). The block must use the same source but with a chosen file.
- [ ] Alternative/extension (the URL source): download over the network (from `inet/`) into `config/oc2r/` and flash.
- [ ] Build + in-game check: flash from a file, insert into a computer, OnyxOS boots with a custom kernel/rootfs.
