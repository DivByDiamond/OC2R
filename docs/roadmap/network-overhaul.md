# Roadmap: network overhaul (wires, towers, tablet, Create Radars)

Sections 20 and 43 are historical inputs to section 48. The design documents are [docs/CABLE-SYSTEM.md](../CABLE-SYSTEM.md) and [docs/NETWORKING.md](../NETWORKING.md).
The implemented device-bus part of section 43 is recorded in [done.md](done.md#43-implemented-device-bus-part).

## 20. Push-based bus instead of polling BFS scans (closed)

Closed as superseded by [section 43](#43-cable-system-rebuild-from-scratch) (and then 48). The problem statement stays valid: the bus topology was rebuilt by a full BFS from the root on
every scan (`ComputerBlockEntity.serverTick()` -> `virtualMachine.tick()` -> `busController.scan()` -> `BusElementManager.scan()` -> full device list rebuild and diff), with retry-by-timeout
(`INCOMPLETE` 10 s, `TOO_COMPLEX` above 128 elements with 5 s retry), `devicesAdded/Removed` only after a correct scan, an extra `beforeDeviceScan` event and an artificial 128-element limit.

The original target was a persistent incremental push-based graph (`BusTopology`/`DeviceTopology` with merge/split, events from place/remove/neighbor/chunk load, no `scan()` in `tick()`,
`BusState` reduced to OK/"neighbor chunk not loaded", multi-controller conflict detection by union-find, `beforeDeviceScan` removed). Decision of section 43: instead of a persistent graph
(itself a source of desync bugs of the same class), use a lazy in-memory BFS invalidated by events, with no persistent network state in the save. Note: section 38 (item Sh1) found that an
event-driven scan with a dirty flag and an O(1) early exit already existed, so a full push-based graph is not required. The unchecked stage list of the old plan (research call sites, `DeviceTopology`,
driver events, remove `scan()` from `tick()`, migrate controllers, `DeviceTopologyTest`) is dropped.

## 43. Cable system rebuild from scratch

Design: [docs/CABLE-SYSTEM.md](../CABLE-SYSTEM.md).

Maintainer decision: the plan "remove `NetworkConnector`, hub and switch and merge everything into one cable" is SUPERSEDED by [section 48](#48-network-overhaul-wires-towers-tablet-create-radars):
`NetworkConnector` is kept and reworked; hub/switch are re-evaluated later; a unified single cable is dropped for now, the bus cable and the connector stay separate.

Trigger: bugs (phantom cables, energy stops at distance, lag on turns, breaks on break+replace, energy/data conflict in one wire), all traced to one cause: a neighborhood/network cache without a
single source of truth, desync between client and server and after chunk reload.

Key decisions that remain valid: auto-connect by adjacency; no persistent network objects in the save (lazy in-memory BFS from the requester, invalidated by events place/break/onLoad/wrench, zero
topology work in `tick()`); device ownership is computed deterministically on every rebuild (not stored) and must be visible to the player ("occupied by Computer #N"), not silent; energy is a
receive-only `IEnergyStorage` on faces (accepting from foreign mods is almost free) with a buffer in each cable, not in the network.

Implemented part (device bus lane: `NetworkResolver`, `OwnerResolver`, `FaceOverride`, `maxBusElements`, shared bus ownership, GameTest `twoComputersShareOneBus`) moved to
[done.md](done.md#43-implemented-device-bus-part).

Open list (stays open):
- [ ] Receive-only face energy (a separate energy model on faces).
- [ ] "Occupied by Computer #N" tooltip on the block and in the guest RPC API.
- [ ] Chunk-load event instead of the 10 s `INCOMPLETE` timer retry.
- [ ] Wrench UX: moved to [section 48](#48-network-overhaul-wires-towers-tablet-create-radars) (wrench tool tag `c:wrenches` style, the mechanic itself later).
- [ ] World migration of existing builds with `BusCable`/`NetworkConnector`; device sharing beyond the ownership model.

## 48. Network overhaul: wires, towers, tablet, Create Radars

Decision (supersedes the removal plan in section 43): `NetworkConnector` stays and is reworked. Hub and switch are re-evaluated after the new node model exists. Reference code lives in `ref/`
(gitignored): `ref/createaddition` (Create Crafts & Additions 1.20.1, wire nodes, spool, wire renderer), `ref/Create-Radar` (Create: Radars 1.20.1, official repo),
`ref/Create-radars-port-1.21.1-neoforge` (1.21.1 NeoForge port, has a ComputerCraft peripheral layer to mirror).

- [x] Wire node model (partly): a link is dropped on load when the other end does not confirm it. Relative positions and a saved peer port index are not done.
  if the other end does not confirm it.
- [x] Multiple ports per connector: config `networkConnectorPorts` (default 4, range 2 to 8).
- [x] Wire spool: two clicks link two connectors (result message plus sound for every outcome); using the cable on two already linked connectors removes the link and returns the owner's cable. Result enum has `DISCONNECTED`.
- [ ] Wire tiers were tried and removed: the plan is a single network cable (improve the existing one, blue). Revisit only if requested.
- [x] Wire renderer (needs in-game check): the existing hanging ribbon is now blue (matching the cable item), its sag and segment count follow the length (`WireGeometry` in core, unit tested), and the culling box covers the sag. Rendering is client only and was not run.
- [ ] DEFERRED (far backlog, needs a block model and a design agreement first): Towers: antenna block with a range that depends on height and surroundings. Nearby cards join the tower network without wires. Towers relay to each other by wire or radio.
- [ ] DEFERRED with towers: Signal model: bandwidth and latency depend on distance; the guest can read signal strength over RPC.
- [ ] Wrench UX (moved here from section 43).
- [ ] Tablet item family with two modes sharing one item base: the UART tablet (see [section 16](devices-storage.md#16-uart-tablet-item): wire-minigame link to a computer, UART mirror terminal) and
  the tower/radio tablet (screen, terminal and tower link). Both descriptions are kept in their sections.
- [ ] Create Radars compatibility: soft dependency, RPC device exposing radar tracks, mirroring the peripherals in the port's `compat/computercraft`. Check first that a NeoForge 1.21.1 build exists
  and which Create version it needs.
- [ ] Hub/switch re-evaluation after the node model exists; `NetworkConnector` rework details.
- [ ] Docs: split todo into `docs/roadmap/` (done) and translate the docs to English (docs language rule: docs are English).
