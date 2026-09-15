# Cable System Rework — Design (Core)

Design doc for replacing OC2R's two independent, ad-hoc wiring mechanisms
with one unified cable system. Written 2026-09-15, not yet implemented —
this is the plan, not the build. Sub-projects after this one (wrench
tool/UX, cross-mod tool-tag compatibility, migrating existing player
builds) get their own specs when work starts on them.

## Why

Today there are **two unrelated block types** doing "wire two things
together", and their bugs are actually the same bug wearing two faces:

- **`BusCableBlockEntity`/`DeviceBusElement`** (device bus: CPU↔memory↔
  disk↔monitor↔keyboard) — full graph BFS re-scan (`BusElementManager.
  collectBusElements`) on *every* topology change, hard-capped at 128
  elements (`TOO_COMPLEX`), and a `MULTIPLE_CONTROLLERS` state that
  freezes the whole bus for 5s if two computers land on one segment.
- **`NetworkConnectorBlockEntity`** (internet mesh / energy) — a
  completely separate point-to-point model: max 2 connections per
  connector, 16-block max distance, raycast-obstruction check, manual
  "owned cable" bookkeeping for who respawns the item on disconnect.

Reported symptoms — cables appearing out of nowhere, energy silently
stopping over distance, lag on runs with turns, breaking/replacing a
cable leaving stale connections, energy and device-bus traffic
conflicting on the same run — all trace back to the same root cause:
**cached neighbor/network state that isn't strictly derived from one
source of truth, and gets re-derived inconsistently between client and
server or after a chunk reload.** Two more targeted rescans and one more
special-case flag do not fix that; removing the mutable cached state
does.

## Scope of this document

One unified cable block replacing both `BusCable` and `NetworkConnector`.
Carries device-bus traffic, energy, and internet-mesh traffic over the
same physical run — the player places one kind of wire, not two.

Out of scope for this doc (own future specs):
- The wrench/tool UX for per-face disconnect (needs this core to exist
  first).
- Migrating existing player worlds off `BusCable`/`NetworkConnector`.
- Any device-sharing feature beyond the ownership model below (e.g. an
  explicitly "shareable" monitor).

## Connection model

**Auto-connect by adjacency.** Two cable blocks that are face-adjacent
are connected — no player action. A cable adjacent to a device (computer,
monitor, energy-consuming block, etc.) auto-connects if that device's
face offers a compatible capability, same as the current per-face
NeoForge capability lookup already used by `AbstractBlockDeviceBusElement
.getNeighbors()`.

**Per-face override, in the data model from day one.** Each cable BE
stores a per-face connection state — `AUTO` (default) / `FORCED_ON` /
`FORCED_OFF` — even before the wrench tool that lets a player set it
exists. Baking this into the save format now avoids a data migration
later when the wrench sub-project ships.

**`NetworkConnectorBlockEntity` is deliberately removed, not folded in.**
Its whole reason to exist — a through-air link up to 16 blocks with an
obstruction check, no physical cable required — has no equivalent in an
adjacency-based model *by definition*. This is an intentional feature
loss, written down here so it doesn't surface later as "wait, where did
long-distance connectors go?" A pair of wireless endpoint blocks
replicating that use case is a plausible future sub-project, not part of
this core rework.

**Hub and switch blocks are removed too.** Internet-mesh topology becomes
simply "which cable network is this computer's card connected to" — the
whole connected network already behaves like one big switch, so
`NetworkHubBlockEntity`/`NetworkSwitchBlockEntity`'s job is subsumed by
the cable network itself.

## Topology: no persistent network objects

**The most consequential decision in this design, so stated plainly:**
there is no network object saved to NBT, no network ID, no merge/split
bookkeeping surviving a save. This directly targets the "phantom
cable"/"broke on reload" bug class — the entire point-of-failure that
persistent, incrementally-merged network state represents (an old,
well-documented pain point in this genre of mod — Mekanism/AE2 grid code
has years of exactly this class of desync bug) is designed out by not
having that state exist at all.

Instead:
- Each cable BE holds a **cache reference** to "the network this cable
  currently belongs to" — purely in memory, never serialized.
- Any topology-relevant event invalidates that cache: block placed,
  block broken/removed, `onLoad`/chunk load-unload, a wrench toggling a
  per-face override.
- On the next access after invalidation, a **fresh BFS runs from the
  requesting cable**, walking currently-loaded neighbors, and the result
  (the resolved network view) is written back into every visited BE's
  cache.
- After a server restart, or a chunk reload, the network is silently
  rebuilt lazily on first access — there is nothing to "recover" because
  nothing persisted that could go stale.

**Performance framing**: this is not a regression from removing the 128
cap's motivation. The old system's cost was never the BFS itself — it
was re-running that BFS (and re-registering every capability listener)
from `serverTick()` on a schedule, whether or not anything changed.
Removing all topology work from `tick()` (see below) means a full BFS
becomes an O(N) cost paid **once per player action** (place, break,
wrench), not a recurring per-tick cost — a full rebuild of a large
network is milliseconds, and it simply doesn't happen unless something
actually changed.

**The 128-element hard cap is replaced by a generous, configurable
default (start at ~1024)**, and exceeding it no longer freezes the whole
network. The player-facing effect changes from "the entire bus goes
`TOO_COMPLEX` and stops" to "the single device that pushed the network
over the limit is flagged unavailable, everything else keeps working" —
consistent with the UX principle that one bad connection must not take
down a working network.

**Zero topology work happens in `tick()`.** The only thing that ticks is
energy distribution across an already-resolved network view (see below).
Topology is entirely event-driven.

## Device ownership

Multiple computers can legally share one physical network segment now
(no more `MULTIPLE_CONTROLLERS` error state) — the wiring merges without
friction. But device *ownership* stays exclusive: a hard drive mounted
by two independent guest kernels with no coordination is filesystem
corruption, not a shared resource, so "just let both computers use it"
is not on the table for storage/CPU/memory-class devices.

**Ownership is computed, never stored.** On each network rebuild, every
device's owning controller is derived deterministically from the
currently-*loaded* set of controllers on that network — e.g. the
controller at the lowest `BlockPos`. No ownership record is saved
anywhere, so there is nothing to desync. A practical side effect: if the
owning computer's chunk unloads, ownership deterministically re-derives
to the next controller on the next rebuild — failover happens for free,
with no special-cased "handle owner disappearing" logic.

**Ownership is visible, never silent.** A controller that loses the race
for a device does not just fail to see it — it sees the device listed
as **occupied, naming the owning computer**, both through the API (so
guest-side tooling can report it) and via an in-world tooltip on the
device block. A player who builds a second computer onto an existing
segment and finds their disk missing gets an explicit reason, not silent
unavailability they have to debug by breaking cables at random.

## Energy

**No network-level energy buffer.** Each cable BE holds its own small
energy buffer, saved with that block entity like any other stateful
block — never centralized in the (unsaved, lazily-rebuilt) network
object, which would have nowhere durable to put the energy when a chunk
unloads mid-transfer. The network is purely a *view* that aggregates and
routes between per-cable buffers; it owns no state of its own.

**External faces expose a receive-only `IEnergyStorage`** (`canExtract()`
returns `false`). This is what makes "accept energy from other mods'
cables" (Mekanism Universal Cable, Pipez Energy Pipe, anything using the
standard capability) close to free — no custom bridge, just implementing
the capability every energy-producing mod already knows how to push
into. Explicitly one-directional: OC2R's network draws power from
adjacent foreign networks, it does not export into them.

## Traffic separation

One cable block, three logically separate "lanes" riding the same
resolved network view — invisible to the player, no default-on
configuration surface:

1. **Device-bus** — RPC/VM-device traffic (CPU, memory, disk, monitor,
   keyboard), subject to the ownership model above.
2. **Energy** — per-cable buffers, receive-only foreign-capability faces,
   ticked distribution.
3. **Internet-mesh** — replaces hub/switch; membership in the network is
   membership in the mesh.

Splitting these was the direct fix for the "energy transfer blocks data
transfer on the same run" symptom: today they collide because the mental
model of "one wire" doesn't match the physical model of "two different
block types, only one of which even carries device-bus traffic." Making
one block type carry all three lanes, cleanly separated internally,
closes that gap.

## UX principles (requirements, not suggestions)

1. **Nothing happens silently.** A connection forming/breaking is
   visible (cable model updates per-face); a wrench interaction gives
   immediate feedback (particles/sound) for exactly the face it changed.
2. **Errors are local, not global.** One bad connection or an
   over-limit device gets flagged on that device/face; it never takes
   the rest of a working network down.
3. **The energy/data split is invisible by default.** The player places
   one kind of wire and thinks of it as one thing; any future
   fine-grained control is an opt-in addition on top of the core, not
   part of it.
4. **Tool compatibility is tag-based** (a `c:wrenches`-style item tag),
   not a specific OC2R item — any mod's wrench works, addressed in the
   wrench-tool sub-project but the tag convention is decided here so
   later work doesn't have to guess.
5. **One source of truth for topology, updated strictly by event.** No
   state that can independently drift between client/server or across a
   save/load cycle — this is the single change responsible for closing
   out the "phantom cable" bug class.

## What's deliberately not decided here

- Whether any device type ever becomes explicitly shareable (e.g. a
  monitor visible to two computers) — plausible future work, not part
  of ownership's default exclusive model.
- The wrench item/recipe/tool-tag registration details — belongs to the
  wrench sub-project.
- Save-format migration for existing `BusCable`/`NetworkConnector`
  builds — its own sub-project; this doc only fixes the data model going
  forward (per-face override lives in the new block from the start).
