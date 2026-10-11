# Buildroot / Linux image

The guest OS image that every computer boots is produced by
[`fnuecke/sedna-buildroot`](https://fnuecke.github.io/maven) (resolved through
that Maven repository into `libs/` by `scripts/download-libs.sh`). It builds a
64-bit RISC-V kernel + root filesystem and packages them into a
`sedna-buildroot-<version>.jar` that ships inside this mod (jarJar) as:

| File (in the jar, under `generated/`) | Purpose |
|----------------------------------------|---------|
| `firmware.bin`                         | minimal RISC-V boot ROM, stored into flash (`MinuxFirmware`) |
| `firmware.properties`                  | `regionSize` of the firmware (8192) |
| `rootfs.ext2`                          | root filesystem with kernel, attached as the RFS VirtIO block device (`BuiltinDevices`, `BuildrootBlockDeviceData`) |

Since 0.2.x the firmware no longer carries a kernel image: the boot ROM loads
the kernel from disk, so the flash only holds `firmware.bin` and the root
filesystem (with `/boot/Image`) must be reachable on a VirtIO block device.
The separate boot filesystem (`bootfs.squashfs` + BFS device) is gone with it.

The root filesystem is attached to every VM, regardless of which firmware
(minux or onyxos) is flashed, so tools present in `rootfs.ext2` are available
on every computer.

## 9p and /mnt/builtin

The 0.2.x kernel has 9p built in (`CONFIG_9P_FS=y`, verified via the `v9fs`
strings in `/boot/Image`), and `/etc/init.d/S90virtio9p` mounts every
virtio-9p tag at `/mnt/<tag>` — so the mod's `builtin` share (guest scripts,
`librpc`, docs) keeps working at `/mnt/builtin`, same as before.

## TCC (Tiny C Compiler)

Since **sedna-buildroot 0.0.64** the image ships TinyCC. The minux buildroot
config enables it with:

```
BR2_PACKAGE_TINYCC=y
```

Verification against the bundled `sedna-buildroot-0.0.64.jar`:

```
usr/bin/tcc                 # 297 KB static-ish executable
usr/lib/tcc/include/*.h     # stddef.h, stdarg.h, tcclib.h, ...
usr/lib/tcc/runmain.o
licenses/tinycc-*/COPYING
```

So inside the guest OS a C program can be compiled on the fly, e.g.:

```sh
tcc redstone_blink.c rpc.c -o redstone_blink   # librpc lives in /mnt/builtin/lib/rpc/
./redstone_blink up
```

`librpc` (see `docs/SRC_STRUCTURE.md` → `lib/rpc/`) talks to Minecraft-side
devices over `/dev/hvc0`; examples: `redstone_blink.c`, `note_block_player.c`,
`example_raii.cpp`.

### Pinning the version

- `gradle.properties` → `sedna_buildroot_version=0.2.10`
- `build.gradle.kts` → `implementation("li.cil.sedna:sedna-buildroot:${sedna_buildroot_version}")`
- `scripts/download-libs.sh` → downloads the jar from
  `https://fnuecke.github.io/maven` into `libs/`

## History: the custom 0.0.72-oc2r1 image (retired)

Upstream minux **0.0.71** removed 9p from the guest kernel
([minux#12](https://github.com/North-Western-Development/minux/issues/12)),
which broke the `/mnt/builtin` mount (OC2R issue #17). Until the 0.2.x switch
we shipped a custom image (minux 0.0.71 files + Linux 6.12.104 with
`CONFIG_9P_FS=y`) via the OC2R `bundled-deps` release. The 0.2.x line has 9p
built in again (see above), so the custom image is retired.

## Rebuilding the image

Building the image is **outside this repository**: it requires the full
buildroot cross-compilation toolchain (or the upstream CI). Then bump
`sedna_buildroot_version` in `gradle.properties`, re-run
`scripts/download-libs.sh --force`, and rebuild the mod.
