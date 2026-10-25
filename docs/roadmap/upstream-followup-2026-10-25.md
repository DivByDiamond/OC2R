# Upstream follow-up — 2026-10-25

Follow-up to [the upstream review](upstream-2026-10-23.md), against the Fabric
26.x implementation at `77207e94`. Keep these checks separate from the Fabric
release: an upstream change is not itself evidence of a local defect.

## Cable lifecycle

The reciprocal saved-endpoint check from upstream `81049e0` already exists in
`NetworkConnectorConnectionManager.resolveConnectedInterface`: a loaded peer
that does not list the owner causes `disconnectFrom`, before it can enter the
live routing map. The previous `staleOneSidedLinkIsDroppedOnResolve` test broke
the peer block before resolving, so it covered the missing-block branch rather
than this branch.

Added shared server regressions:

- `livePeerWithoutReciprocalLinkIsDroppedOnResolve` resolves a real link, removes
  the reciprocal membership while keeping the peer alive, then checks that both
  saved membership and the live routing entry disappear on resolution.
- `chunkUnloadRetainsSavedCableMembership` resolves both directions and calls
  `onChunkUnloaded` followed by `setRemoved`. It checks that saved endpoint
  membership and cable ownership survive, while the surviving peer removes its
  live reference to the unloaded instance. This drives the lifecycle callback
  seam; it does not simulate physical chunk eviction or contraption assembly.
- The survival cable-item removal test now requires exactly one dropped cable,
  rather than accepting any positive count.

The over-distance drop still uses the endpoints' midpoint. This differs from
upstream `35c0ed5`; no relocation fixture was run here, so it remains a follow-up
rather than a completed moving-construction fix. Cable positions are saved in
world coordinates, making transforms and stale peer ownership relevant to that
fixture. Do not replace this subsystem wholesale with upstream's implementation.

## Movement lifecycle and interaction distances

`ModBlockEntity.setRemoved` still treats removal without a preceding chunk
unload as destruction. `ComputerBlockEntity.unloadServer(true)` stops the VM,
whereas the unload path suspends it; both then dispose it. Connector destruction
also tears down resolved links. This confirms the distinction matters in the
local implementation, but does not establish which callback a particular
Create/Aeronautics/Sable version invokes during assembly.

`MessageUtils.withNearbyServerBlockEntity` still uses
`pos.closerToCenterThan(player.position(), 8)`. This does not dispatch through
`player.distanceToSqr`, so an integration that transforms the latter method
cannot affect this check. The vanilla bound is strictly less than eight blocks;
any later replacement must preserve that strict boundary and the existing
loaded-chunk lookup. No installed moving-world integration fixture has been
identified in the acceptance runtime, so neither movement case is marked passed.

Concrete remaining acceptance sequence, per supported integration/version:

1. Start a computer with a RAM-only nonce and writable-root marker; connect two
   network endpoints and a gateway. Record VM state and connector membership.
2. Assemble, translate and disassemble the construction, including a peer in an
   unloaded chunk. Verify nonce, root marker, device identities and both cable
   endpoints; count cable drops on break and over-distance disconnection.
3. Open the computer UI and send block interactions at the moved location.
   Confirm near interactions succeed and interactions at/above the eight-block
   authorization boundary are rejected without loading another chunk.

## Fabric server configs and Velocity

The absence of an OC2R `ServerConfigMessage` is **not evidence of missing config
sync**. OC2R registers `AsyncConfig.SERVER_SPEC` as `ModConfig.Type.SERVER` through
Forge Config API Port. Inspection of the actual pinned dependency JARs with
`javap -c -p` confirmed:

| Fabric target | Pinned artifact | Config sync implementation |
| --- | --- | --- |
| 1.21.1 | `N5qzq0XV` | `ConfigSync.syncConfigs` creates server-config payloads; received names are resolved through `ModConfigs.getFileMap` before `ConfigTracker.acceptSyncedConfig` |
| 26.1.2 | `jUe0ucoE` | `syncAllConfigs` sends configuration-phase payloads; `syncPendingConfigs` supports play-phase pending updates; received names use the registered file map |
| 26.2 | `rSd3GiG8` | Same inspected entry points and registered-filename lookup as 26.1.2 |

Modern JAR SHA-256 values inspected:

- `jUe0ucoE`: `271ead0c8191902aa9e13a6f79a065633ed6bf7e13be024e82c8c0abc4da4775`
- `rSd3GiG8`: `1b4b17fc4150481447201152604688c74726d6631c9a626dd21f2c5e2b3df1ac`

This is dependency-bytecode evidence, not a proxy integration test. Existing
Fabric hook tests prove config registration/loading, not a real network login
or backend switch. Do not add a duplicate OC2R config channel without reproducing
a failure in the existing path.

Remaining proxy fixture: use a real Fabric client through Velocity and two
backends with different `oc2r-server.toml` values. Check first join, A-to-B and
B-to-A switches, config reload, disconnect/default restoration, and an unknown
config filename. Record the exact Velocity, forwarding-mod and Config API Port
versions. No proxy/client socket run was performed in this follow-up.

## Verification status

The dependency-bytecode inspection above completed locally. The added shared
GameTests must be included in fresh server verification; their results are
recorded in the branch closeout report rather than inferred from compilation.
No production implementation was changed by this follow-up.
