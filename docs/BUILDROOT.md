# Buildroot / Minux image

The guest OS image that every computer boots is produced by the
[`North-Western-Development/minux`](https://github.com/North-Western-Development/minux)
project — a fork of [Buildroot](https://buildroot.org/) for OC2. It builds a
64-bit RISC-V kernel + root filesystem and packages them into a
`sedna-buildroot-<version>.jar` that ships inside this mod (jarJar) as:

| File (in the jar, under `generated/`) | Purpose |
|----------------------------------------|---------|
| `fw_jump.bin`                          | OpenSBI firmware (`MinuxFirmware`/`OnyxOSFirmware`) |
| `Image`                                | RISC-V Linux kernel (`MinuxFirmware`) |
| `rootfs.cramfs`                        | root filesystem, always attached as the RFS VirtIO block device (`BuiltinDevices`) |
| `bootfs.squashfs`                      | boot filesystem, attached as the BFS VirtIO block device |

The root filesystem is attached to every VM, regardless of which firmware
(minux or onyxos) is flashed, so tools present in `rootfs.cramfs` are available
on every computer.

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

- `gradle.properties` → `sedna_buildroot_version=0.0.72-oc2r1`
- `build.gradle.kts` → `implementation("li.cil.sedna:sedna-buildroot:${sedna_buildroot_version}")`
- `scripts/download-libs.sh` → downloads the jar from the OC2R
  [bundled-deps release](https://github.com/TumRedSun/OC2R/releases/tag/bundled-deps)

## Custom image: 0.0.72-oc2r1

Upstream minux **0.0.71** removed 9p from the guest kernel
([minux#12](https://github.com/North-Western-Development/minux/issues/12)),
which broke the `/mnt/builtin` mount (OC2R issue #17). The maintainer reverted
this on master (`a49110a1`), but no fixed release exists yet, so we ship a
custom image:

- `rootfs.cramfs`, `bootfs.squashfs`, `fw_jump.bin` — verbatim from minux 0.0.71;
- `Image` — Linux **6.12.104** built from minux master with
  `CONFIG_9P_FS=y` / `CONFIG_NET_9P=y` / `CONFIG_NET_9P_VIRTIO=y`
  (verified in QEMU: `builtin` tag mounts to `/mnt/builtin`).

Switch back to upstream as soon as a minux release >= 0.0.72 is published:
revert `scripts/download-libs.sh` to the minux release URL and bump versions.

## Rebuilding the image

## Rebuilding the image

Building the image is **outside this repository**: it requires the full
buildroot cross-compilation toolchain (or the minux CI). The upstream build
(see the minux repo, `.github/workflows/publish.yml`):

```sh
git clone https://github.com/North-Western-Development/minux.git
cd minux
make            # builds fw_jump.bin, Image, rootfs.cramfs into output/images/
./minux-bootfs/build.sh
./gradlew -Psemver='<tag>' build   # packages everything into sedna-buildroot-<tag>.jar
```

Then bump `sedna_buildroot_version` in `gradle.properties`, re-run
`scripts/download-libs.sh --force`, and rebuild the mod. To change what goes
into the image (e.g. add/remove a buildroot package), edit the minux config
(`BR2_PACKAGE_*` entries) and rebuild upstream.
