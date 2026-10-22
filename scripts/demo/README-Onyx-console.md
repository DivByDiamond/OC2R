# OnyxOS console fixes

`scripts/demo/onyx-console.patch` applies to OnyxKernel revision
`dc3aa4b` (the revision after the previously bundled `1f39a22`).
The owning source repository is `/storage/project/systems/Onyx/OnyxKernel` on
this development machine. The patch is GPL-3.0-or-later, matching OnyxKernel.
It is kept here so that rebuilding a binary does not lose these fixes.

The stock kernel mirrored stdout to an 80×24 UART terminal while reporting the
primary framebuffer's larger cell grid to applications. At 1920×1080, `otop`
placed its footer on row 66, outside the PC GUI. The patch uses a common grid of
at most 80×24 on Sedna, bounded by the physical screen on smaller displays.
Other platforms retain their full framebuffer console grid. Native `/dev/fbN`
geometry, mmap, and direct application drawing keep the full device resolution.

The patch also fixes ANSI `EL 1` and `EL 2` line erasure, moves routine file-open
and ELF-segment traces to debug level, and publishes only the console viewport
instead of copying an entire high-resolution framebuffer on every console
refresh. Full-frame presentation remains available for native framebuffer use.

## Rebuild boundary

The command below rebuilds the original console-only patch, not the complete
current networking/RPC or console-scale/login candidate. To reproduce the full
candidate, follow [README-Onyx-build.md](README-Onyx-build.md), which includes
all five kernel-checkout patches, the login rebuild, compiler/libc and HTTP
patches, and a separate root-image staging directory. The later console
presentation/login behavior is described in
[README-Onyx-console-scale.md](README-Onyx-console-scale.md).

## Console-only rebuild

Use Rust 1.97 with the `riscv64gc-unknown-none-elf` target and
`riscv64-elf-objcopy`. Work in a disposable clone, preserving the owning checkout:

```sh
git clone --no-hardlinks /storage/project/systems/Onyx/OnyxKernel /tmp/onyx-console-build
cd /tmp/onyx-console-build
git checkout dc3aa4b
git apply --check /path/to/oc2r/scripts/demo/onyx-console.patch
git apply /path/to/oc2r/scripts/demo/onyx-console.patch
# This produces the console-only checkpoint; use the full instructions above for the demo.
cargo test -p onyx_kernel -- --test-threads=1
cargo kbuild --features smode
riscv64-elf-objcopy -O binary \
  target/riscv64gc-unknown-none-elf/release/onyx-kernel \
  /tmp/onyx-kernel.bin
sha256sum /tmp/onyx-kernel.bin
```

After the actual guest smoke test passes, install that binary as
`src/main/resources/onyxos/onyx-kernel.bin`. These changes do not require changing
the OnyxFS base image. Any network/demo tools packaged into that image have their
own build instructions. Do not replace a running world's base filesystem image
without preserving compatibility with its saved overlays.
