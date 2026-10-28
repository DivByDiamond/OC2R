# Optional atomic framebuffer publication v1

Monitors and projectors continue to expose the unchanged RGB565 simple-framebuffer
pixel range (`stride = width * 2`). An independent 32-byte, four-byte aligned MMIO
companion advertises `compatible = "oc2r,frame-present-v1"`. Its `reg` uses the
board's two address cells and two size cells. `framebuffer-base` is exactly two
32-bit cells (one unsigned 64-bit value) identifying the associated framebuffer's
base. Never infer association from adjacency or append commands to pixel memory:
other framebuffer/device ranges may immediately follow pixels.

All defined accesses are aligned little-endian 32-bit loads/stores. Other sizes,
misaligned accesses, reserved offsets and unknown commands have no effect/read
zero. Pixel memory retains its existing supported widths.

| Offset | Register | Meaning |
| --- | --- | --- |
| 0x00 | MAGIC, read only | `0x3150464f` (`OFP1` bytes) |
| 0x04 | VERSION, read only | `1` |
| 0x08 | COMMAND, write only | BEGIN=1, COMMIT=2, ABORT=3, DISABLE=4 |
| 0x0c | STATUS, read only | bit 0 active transaction; bit 1 negotiated mode |
| 0x10 | CAPS, read only | bit 0 supports atomic completed-frame publication |
| 0x14–0x1f | reserved | zero |

Legacy guests stay in immediate mode until BEGIN. The first BEGIN snapshots the
current pixels as a complete baseline, including when nobody has copied an initial
frame yet. BEGIN sets STATUS=3; stores update draw pixels while host readers,
including forced late-watcher baselines, see the previous completed snapshot.
COMMIT while active atomically replaces the completed snapshot and marks it dirty,
then sets STATUS=2. Writes stay hidden in negotiated mode until a later COMMIT.
Repeated BEGIN while active and COMMIT while inactive do nothing.

ABORT discards partial draw pixels by restoring the last completed image and sets
STATUS=2. DISABLE also restores that image, then sets STATUS=0 and resumes legacy
immediate publication. Guests sharing the framebuffer with applications that write
pixels directly should BEGIN/draw/COMMIT/DISABLE for each presentation. Serialize
commands and pixel stores across guest writers; the protocol has no writer identity
or ownership enforcement. Check STATUS before beginning if cooperating writers may
be active. No Java monitor/lock survives a guest MMIO call.

VM reset performs DISABLE. Unmount/close preserves the draw pixel blob, including
partial writes from an active transaction, so resumed guest CPU stores can finish
that same frame. Optional device NBT preserves the negotiated status
and an independent completed image; old saves without that state resume legacy
mode. Restore only accepts an exactly sized image. This preserves a coherent visible
completed frame, but does **not** promise a point-in-time snapshot across CPU/RAM,
MMIO writes and the independently persisted pixel blob. When CPU progress and the
draw blob are saved together while paused, an active transaction resumes with its
partial draw intact and publishes only when that CPU reaches COMMIT. Otherwise
ABORT/DISABLE before starting a fresh full repaint.

Each pixel range is capped at 32 MiB before mapping. The companion reserves a
matching bounded host snapshot plus one page in the VM memory allocator; it never
extends the pixel range. Snapshot storage is reused between presentations, including
DISABLE/BEGIN cycles, and released on close. NBT serialization temporarily copies
at most one additional 32 MiB image. Encoders consume the common `copyFrame` path,
so monitor and projector clients require no protocol changes.

Compatibility during migration: automatic companion allocation respects saved MMIO
reservations, including devices that have not remounted yet; it cannot consume a
future saved framebuffer/keyboard range. A new, never exposed companion may be
omitted if its snapshot budget or MMIO range is unavailable, keeping the existing
legacy framebuffer mounted. Once an address or completed state has been saved,
mount must fail rather than silently remove a companion that resumed guest code or
its saved FDT may still reference. The actual computer/robot external-device base
provider starts at `0x30000000`; the no-provider allocator path also honors saved
reservations and preserves Sedna's distinct physical-RAM/MMIO address classes.
