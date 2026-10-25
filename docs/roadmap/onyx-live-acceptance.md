# Onyx live OC2R acceptance — 2026-10-25

Checked on actual Fabric Minecraft clients for both 26.1.2 and 26.2, using
OC2R `de288996`, the real `FLASH_MEMORY_ONYXOS` item and `OnyxOSFirmware`.
Each computer had 64 MiB RAM, a GPU, live monitor/projector and creative power,
with zero removable hard-drive items. The root disk was OC2R's private built-in
VirtIO block device. No host-generated pixels were injected into the displays.

## Results

| Case | 26.1.2 | 26.2 |
| --- | --- | --- |
| Firmware boot and root shell login | Passed | Passed |
| Native `fb-draw` RGB565 writes reaching the live monitor | Passed | Passed |
| RAM-only shell variable and private-root file after real world save/reopen | Passed | Passed |
| OnyxFS snapshot of the full system root | Reproduced `-12` / `NoMem` | Reproduced `-12` / `NoMem` |
| OnyxFS create/list/mutate/rollback on a minimal root | Passed | Passed |

The native display tool reported `320x200x16` for the monitor and `640x480x16`
for the projector. Device discovery order differs between versions, so the
fixture checked actual client monitor texture pixels rather than assuming a
fixed framebuffer index. Screenshots show the red framebuffer on the world
monitor. The resumed shell produced a RAM token and read its file from the
private root; this is separate from the guest filesystem snapshot syscall.

The minimal root ran a 3,145-byte root-ring probe as `/bin/init`. It recorded
`snapshot_create=1`, `snapshot_list=1`, `snapshot_rollback=0`, then verified the
original file bytes after mutation and emitted `OC2R_SNAPSHOT_QA_OK`. Thus the
OC2R non-removable disk path supports the operation. The normal system image
needs 965 data blocks plus 12 metadata blocks before boot/test writes.
The initial direct-pointer count of 197 omitted indirect file data; the old
OnyxFS archive format permits only 31 input blocks. Its `NoMem` failure is a filesystem snapshot
capacity limit, not evidence that OC2R's root disk is removable or unsavable.
The subsequent full-root fix and shutdown investigation are recorded in
[the runtime follow-up](onyx-runtime-followup.md).

## Exact source and artifacts

The current kernel was built with `smode` from
`/storage/project/systems/Onyx/OnyxKernel`, commit
`9bec3351b187fafd6f9628007d0be3ff5ef437ac`, including pre-existing uncommitted
TCP working-copy changes. They were preserved. This is a working-copy build,
not an assertion that the clean commit has identical bytes. ABI verification
covered all 100 syscall definitions against the local shell/compiler sources.
The matched packaged userland was cloned and extended only with the native
probe; runtime configuration overrides selected the candidate kernel/root.
The mod's tracked bundled resources were not replaced.

- Kernel SHA-256: `66148d93213b723010d102c13b82f6edb49d0c4a2d2fde75d0f1f466915e60d5`.
- Full probe root: `2d1dabe87b72630e9fe329f2b7af541acd3a5df9601d89368572d662e99fbfa7`.
- Minimal root: `d2a1e204214124b8205fe4bb18d4d31d5cd9384f3103213b6f6a64dff4a551d2`.

Local JSON results, source/artifact manifest, guest logs, screenshots and the
reproduction stand are retained under `build/reports/onyx-live-acceptance/`.
The isolated fixture uses a separate client run directory and restores the
normal generated test entrypoint metadata after each run. This acceptance does
not change the production dependency versions or establish proxy/contraption
integration support.
