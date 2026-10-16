# Bundled OnyxOS images

Images in `src/main/resources/onyxos/` were rebuilt on 2026-10-16 from
`/storage/project/systems/Onyx/` using `OnyxOS/scripts/build-oc2r.sh`.

The build includes OpenSBI jump firmware, the S-mode OnyxKernel, rebuilt
init/login tools and shell, the RGB565 framebuffer diagnostic, and a raw
16 MiB OnyxFS root image. The kernel is loaded at `0x80200000`.

Verified fixes include:

- The reproduced Minecraft crash after loading `init` was a failed VirtIO
  keyboard negotiation: the driver left a nonzero device base with a null
  used-ring pointer, then polled address `NULL + 2`. Input now negotiates
  `VIRTIO_F_VERSION_1`, publishes readiness only after successful setup, and
  resets/frees partial queues on failure. Evdev letters/digits and polling
  during blocking console reads are also corrected.

- FDT traversal no longer rediscovers VirtIO devices or initializes a queue
  twice. Framebuffer discovery handles `/soc` and legacy `/chosen` nodes,
  deduplicates MMIO addresses, and exposes `/dev/fb0`, `/dev/fb1`, etc.
- Framebuffer ioctls, read/write and mmap operate on the selected device.
  VFS dispatches devfs writes, and the diagnostic uses RGB565 for 16-bit screens.
- Packed framebuffer MMIO bases need not be page aligned. mmap preserves the
  physical page offset and reserves the full rounded extent; the diagnostic
  unmaps that aligned extent. First, middle and last pixels on all three
  framebuffers were checked, including after a world reload, while keyboard
  registers remained intact.
- Shell redirection binds standard streams explicitly; file writes and
  in-process commands such as `pwd > file` keep and restore their descriptors.
- VirtIO block capacity is measured in 512-byte sectors. The old calculation
  incorrectly treated `size_max` as a multiplier and could advertise space
  beyond the backing disk. The 16 MiB image now grows to 4096 OnyxFS blocks.

Standalone Sedna 5.2.0 verification at 64 MiB RAM covers init, root login,
shell, redirection, create/read/copy/rename/delete, a saved-image reboot, and
three independently written RGB565 framebuffers. The same guest image is used
for the Minecraft integration checks. 166 kernel tests, FDT regressions and the
existing OnyxApps checks were also run; detailed logs are in
`build/reports/onyx-qa/`.

Log in as `root` with an empty password. Four 16 MiB memory modules provide
the tested 64 MiB configuration. This is a tested configuration, not a measured
minimum for every application.

## Firmware-only boot and root storage

The OnyxOS flash selects a private writable OnyxFS root disk provided by the
computer itself, using the bundled 16 MiB image. A separate OnyxOS hard-drive item is optional. The packaged
image is shared read-only; each computer saves its modified pages in its VM
snapshot. Minux and Onyx keep separate root overlays when firmware is changed.
Existing hard-drive items retain their contents and remain attached as extra
disks. An existing Onyx hard drive is not copied into the automatic root disk.

The same `config/oc2r/onyxfs.img` override supplies the immutable base for new
Onyx roots. Keep that base compatible with already saved overlays; changing
its filesystem layout underneath existing writes is not an image migration.

## Importing an OS inside Minecraft

Insert a flash chip or writable hard drive into the flash writer and use it
with an empty hand. Its screen offers bundled Minux/Onyx firmware and a local
file chooser. Shift-use ejects the item. A running guest must release the
writer before it can be programmed from this screen.

Custom flash images must contain flat RISC-V machine code for the normal RAM
entry address, `0x80000000`. For the OpenSBI jump layout, place OpenSBI at file
offset zero and the S-mode kernel at offset `0x200000`. Convert ELF executables
to a suitable flat boot image before importing. Import a raw filesystem/disk
`.img` into a hard-drive item if that OS needs its own disk. Selecting bundled
Onyx firmware supplies its root image automatically.

Local imports are bounded by the inserted item's capacity and a 64 MiB image
limit. The server receives ordered chunks and checks the final SHA-256 before
replacing the item's data. Interrupted or rejected uploads retain the original
item. Files are stored as world-local blobs rather than image bytes in item NBT;
no datapack or server-side path is required.


## Multiple screens

A connected projector and each monitor appear as separate native framebuffer
nodes. Their numbering follows device discovery; inspect `/dev` and each
framebuffer's ioctl metadata rather than assigning a physical role by index.
The guest format is `r5g6b5`, with a two-byte pixel and the exported row stride.

The bundled diagnostic can address a single device and choose a color:

```text
run /bin/fb-draw /dev/fb0 red
run /bin/fb-draw /dev/fb1 green
run /bin/fb-draw /dev/fb2 blue
```

Omit the color for a gradient. The console uses its primary framebuffer;
applications can write or map the other devices independently. The basic
framebuffer path does not require the RPC library.


## Input

The computer terminal uses UART input. Native VirtIO keyboard input currently
uses one primary keyboard; additional initialization returns `Busy` without
resetting the working queue. The standalone smoke connects two real Sedna
keyboards and verifies a character from the selected keyboard reaches a guest
command. Multiple native keyboard aggregation remains separate work; all
connected framebuffer outputs can be addressed independently.

## Verified source revisions

- OnyxKernel: `1f39a22309d5f38a87f314b2a9a8aa898c440b4f`.
- OnyxShell: `636fef68547f3431fc9ed8eb3ddcd06900567e0a`.
- OnyxOS: `888f69a5d7dccfdad382bc93641e8ea03ae6a8de`.
- Kernel SHA-256: `55eac6a5ddf55cf667a4f313be9708f061ff108ddcc728729671c1efdd5f8a74`.
- Root image SHA-256: `bcbc1f870be9b14967eec802e6021c1c6e934ac1748d893430c7d62261fab31b`.

Real Minecraft 26.1.2 checks cover flash-only Onyx boot, login and file operations,
three independent outputs, monitor GUI, separate Minux/Onyx root writes through
firmware changes and world reload, and imported media surviving reload in the
player inventory. Warm CPU/RAM continuation and automatic framebuffer retention
across reload remain separate checks; these checks log in and redraw after boot.
