# Roadmap: cables, device bus, energy

Bus cable, energy transfer, world sync of cable blocks. Wire/tower rework: [network-overhaul.md](network-overhaul.md). Design: [docs/CABLE-SYSTEM.md](../CABLE-SYSTEM.md).

Section 13 (wire rewrite: FE + EU energy, connection fix) is closed: [done.md](done.md#13-wire-rewrite-fe-and-eu-energy-connection-fix).

## 29. World sync fixes

Audit result: 3 real bugs (one broke multiplayer) plus much duplicated synchronization. The closed bugs (`MultipartMessage` cache key `d59ab0a`, `ServerCanceledImportFileMessage` cast,
`MonitorStateManager` save/load, `InternetGateWayBlockEntity.notifyPlayers`, `ExportedFileMessage` size and multipart) are in [done.md](done.md#29-closed-items).

### Duplication (bytes on the wire), open status after section 38

Groups closed by section 38 (facade x3 -> x1, connectors x2 -> x1, interface names tag-only, floppy/flash x2 -> x1, chunk==null): see [done.md](done.md#38-closed-items). Still open:
- [ ] Monitor/projector state is sent twice: `setBlock(LIT)` + `*StateMessage` (hasEnergy lives only in `*StateMessage`; low priority).
- [ ] `sendToClientsTrackingChunk` VS2 fallback broadcasts to all players in all dimensions (the comment about "wrong dimension" does not match the code): filter by dimension needs the VS2 mapping (see
  [38 in network-inet.md](network-inet.md#38-performance-audit-network-monitors-bus)).

## 34. Manual in-game testing: wires and screens

After the cable energy fixes, block audit and monitor refactor (2026-08-19), these need in-game checking. The boxes are consolidated in
[qa-checklist.md](qa-checklist.md#from-section-34) (cable/energy chain of 3+ cables and world restart, switch chunk load, NetworkConnector, PCI Card Cage, Bundled Redstone, monitor render and multiblock, speaker texture).
The Redstone Interface side index bug is treated as fixed (commit ec49a41, "redstone FACING rotation"), so the related note and the "check all 4 faces" line are closed.

### 47 bus items

Bus items of the 2026-09-17 audit: B3 (`RPCDeviceBusAdapter` handoff `volatile`) is closed ([done.md](done.md#47-closed-items)). Open bus/VM items (P4 `BusElementManager` `scanDelay` off-by-one,
A2 package cycles `blockentity <-> bus.provider` and `vm <-> bus.device.vm`) are tracked in [quality.md](quality.md#47-audit-2026-09-17).
