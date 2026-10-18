# Native Onyx device RPC

The demo image includes `oc2ctl` and its reusable C library
`scripts/demo/guest/oc2rpc.{h,c}`. These run inside Onyx on RV64 and talk to the
real OC2R device bus through raw `/dev/hvc0`. This channel is independent of
Ethernet and the UART terminal. No host command executes robot actions.

The Onyx kernel changes are retained in `scripts/demo/onyx-rpc.patch`, based on
OnyxKernel `dc3aa4bdfaa8d86d1b241d0056248685b1feac48`. Apply alongside the console
and networking patches when rebuilding the combined demo kernel.

The driver negotiates modern VirtIO VERSION_1, deliberately leaves multiport
disabled, and selects four queue entries, matching Sedna's console maximum.
It retains every byte of a used receive buffer across partial reads, and keeps
transmit buffers alive until the device completes their descriptors. Writes
are partial/nonblocking; the userspace library retries EAGAIN. There are no
per-byte page allocations or unbounded kernel transmit waits. The driver also
handles the legacy MMIO ring layout.

## Guest commands

Run as the default root user. Only one process may own the RPC bus at a time.
Use `run /bin/oc2ctl ...` if the shell does not resolve its name directly.

```text
run /bin/oc2ctl list
run /bin/oc2ctl methods robot
run /bin/oc2ctl robot-status
run /bin/oc2ctl robot-demo
run /bin/oc2ctl inspect
run /bin/oc2ctl scanner
run /bin/oc2ctl inventory
run /bin/oc2ctl inventory-demo
run /bin/oc2ctl sound-demo
run /bin/oc2ctl call robot getEnergyStored
```

`robot-demo` moves forward/back, then turns right/left. It checks the specific
queued action ID until SUCCESS; accepting a queue request is not reported as
completed movement. Keep the front cell clear. The robot must have power.
`inspect` and `scanner` require a scanner module. `inventory-demo` requires the
inventory operations module, an empty currently selected inventory slot, and a chest containing
items directly above the robot. It takes one item and returns it; it stops on
failure. `sound-demo` requires a sound card and requests an 880 Hz, 300 ms tone.

`call TYPE METHOD JSON_ARRAY` exposes the remaining device methods without
hardcoding every peripheral. `methods TYPE` prints the actual signatures.
For multiple devices of one type, use `TYPE:UUID` with a UUID from `list`.
Account for shell quoting when passing JSON strings; the typed demo commands
avoid that problem.

The library supports list, typed discovery, method enumeration, generic
invocation and robot action completion. It skips unsolicited event messages
while waiting for a response. It does not expose an event subscription API.
Limits are explicit: 8191-byte request JSON, 32767-byte response JSON, 32 levels
of JSON nesting, 5-second RPC deadline, and a 15-second action completion
window. Oversize/malformed responses and remote errors are failures, not false
success or silently converted zeros. A timed-out or abandoned request leaves its outcome unknown: the device may
have executed it even though the reply was not received. The protocol has no
request IDs, and closing/reopening `/dev/hvc0` does not clear delayed replies.
Reset/reboot the VM bus before retrying a mutating call; reopening the file
descriptor alone cannot resynchronize the protocol.

## Compiler and libc repairs

Two independent upstream issues were reproduced in actual Onyx execution:

- `materialize(VAL_LVAL)` emitted raw ADDI for structure member offsets. A
  field at offset 32768 resolved to offset zero. The isolated repair uses the
  existing `rv_addi_imm` expansion. Patch:
  `scripts/demo/onyxcc-large-member.patch`.
- The first `puts`/`fputs`/`fwrite` could run before `streams_init`, leaving
  stdout's buffer capacity zero. `fwrite` then looped forever with zero
  progress. Adding diagnostic printf hid the failure because printf initialized
  streams. The repair initializes streams in fwrite, fread and fgetc, covering
  the corresponding puts/getchar entry paths. Patch:
  `scripts/demo/onyx-libc-stdio-init.patch`.

These patches target OnyxCompiler `f9f78d88d6ecd16aee1ef4ab00cee291265d9428`.
Use an isolated checkout; do not modify the owning working tree implicitly.

```sh
git -C /path/to/isolated/OnyxCompiler apply /path/to/oc2r/scripts/demo/onyxcc-large-member.patch
git -C /path/to/isolated/OnyxCompiler apply /path/to/oc2r/scripts/demo/onyx-libc-stdio-init.patch
make -C /path/to/isolated/OnyxCompiler onyxcc
/path/to/isolated/OnyxCompiler/onyxcc --ring1 \
  -I scripts/demo/guest -o /tmp/oc2ctl.onx \
  scripts/demo/guest/oc2ctl.c scripts/demo/guest/oc2rpc.c
```

OnyxCC automatically links the libc next to its executable. Rebuild other C
applications and tools against that corrected libc as well. The C bus structure
also places the large response array last so small fields stay below the
compiler's old immediate-offset boundary. This layout does not replace the
compiler repair for other programs. This patch verifies that particular member
address bug; it is not a general audit of the compiler.

## Verification and its boundaries

```sh
python3 scripts/demo/tests/test_rpc.py
```

The native host test compiles the same library with warnings treated as errors
and exercises a raw PTY: fragmented three-byte input, a device list over 4 KiB,
JSON strings containing braces/escaped quotes, unsolicited events, void/null
results, remote errors, rejected unknown primitives, invalid number/escape grammar, trailing parameters,
malformed replies, depth limit and response size limit. A separate CLI fixture
verifies that an occupied selected inventory slot prevents all transfers. Expected: `RPC_NATIVE_FRAMING_PARSER_OK`, seven requests, exit zero.

`scripts/demo/tests/RpcTransportProbe.java` boots the real Onyx kernel, root
image, shell and native `oc2ctl` on Sedna. Its synthetic host endpoint advertises
80 devices, producing a response larger than a 4096-byte DMA buffer, then
handles discovery and invocation. The test requires Sedna, Ceres, their runtime
libraries and Gson on the Java classpath:

```sh
javac -cp "$SEDNA_CLASSPATH" -d /tmp/oc2r-rpc-smoke scripts/demo/tests/RpcTransportProbe.java
java -cp "$SEDNA_CLASSPATH:/tmp/oc2r-rpc-smoke" RpcTransportProbe \
  src/main/resources/onyxos/fw_jump.bin /tmp/onyx-kernel.bin /tmp/onyx-demo.img 64
```

Expected: `SEDNA_RPC_TRANSPORT_PASS requests=3`. This proves actual guest byte
transport and parsing. It does not prove movement of a Minecraft robot; that
requires the separate in-game arena run against real devices.

Minimal native regression sources are retained in
`scripts/demo/tests/onyxcc_large_member.c` and
`scripts/demo/tests/onyx_stdio_first_puts.c`. On the reviewed checkpoints, the
old compiler returned offset zero and exited 1; the patched compiler returned
32768 and exited 0. The old first-puts program stalled without producing its
marker; the corrected libc printed `ONYX_STDIO_FIRST_PUTS_OK` and exited 0.
