# Tablet UI and tower radio network

Status: owner-approved direction, 2026-11-03. Sections 16, 48, 51 and 52 refer to this plan.
Only checked items are implemented. The first tower Ethernet bridge is implemented in the working branch; the Minecraft tablet item and live tablet service are not implemented yet.

## 1. Current baseline

- [x] GPS tower recipe: iron bars/copper/iron bars, circuit board/CPU III/circuit board, iron/iron/iron.
- [x] Default tower hop radius: 45 blocks, configurable with `gameplay.gps_tower_range`.
- [x] Circular coverage in X/Z; Y does not limit connectivity. Separate dimensions do not connect.
- [x] Relative-coordinate networks have explicit membership and a selected reference tower.
- [x] Tower membership survives chunk unloading, and removed towers invalidate routes.
- [x] Coordinate results retain X, Y and Z even though connectivity ignores Y.

The existing GPS topology is a starting point, not an implemented Ethernet radio bridge.
The reference tower defines the coordinate origin; ordinary data routes must not require visiting it.

## 2. Network hardware and compatibility

- [ ] The robot dock station (the existing charger block) is its own short-range access point (it attaches nearby clients and reaches the wider network through the nearest tower; it does not relay like a tower): it provides a 10-block horizontal client attachment radius for the robots and devices around it. There is no separate station block.
- [ ] A tower provides a 45-block horizontal client attachment radius and 45-block tower hops by default.
- [ ] Use circular X/Z coverage without height limits for both access-point types (dock station 10 blocks, tower 45 blocks).
- [ ] Connect a PC's existing Ethernet network to a dock station or tower using the existing blue network cable and compatible connectors.
- [ ] Authorized robots and tablets attach wirelessly to an access point in range, with no new mandatory radio card/module item.
- [ ] Clients roam between reachable access points of their own network; radii are not added together to extend attachment range.
- [ ] Towers relay between each other. Walls do not attenuate the first implementation.
- [ ] Reuse the existing Ethernet frame and guest TCP/UDP paths. Keep ordinary IP networking compatible.
- [x] Radio data transport works independently of the GPS relative-coordinate reporting switch.
- [x] Route snapshots may contain unloaded relay towers and must not force-load their chunks.
- [x] Unloaded PCs and robots do not execute programs or accept successful command completion.
- [x] Reconcile saved relay nodes when their chunks load; loaded destruction removes a relay immediately.

The approved cylindrical coverage replaces older plans based on tower height or surroundings.
Reusing one physical tower does not combine device-bus ownership, power routing and packet networking into one protocol.

## 3. Short radio addresses

User-facing name: **ORA (OC2R Radio Address)**. Example: `7K3F:02Q9`, with an optional device alias such as `warehouse`.
See the [Russian/English explanation](../RADIO-NETWORK.md). This is a custom radio address, not a new IPv4/IPv6 wire format. Ordinary IP remains available underneath the compatibility bridge.

- [x] Two groups of four base32 characters: network prefix and device number, 20 bits each.
- [x] Canonical alphabet: `0123456789ABCDEFGHJKMNPQRSTVWXYZ`; define input normalization and invalid-input errors.
- [x] A saved server registry randomly allocates free nonzero network prefixes and device numbers, excludes occupied and retired values, and reports exhaustion.
- [x] Keep full network/device UUIDs as authoritative identities; display addresses and aliases are not credentials.
- [ ] Persist addresses across restarts and access-point roaming. Moving to another network requires explicit enrollment.
- [ ] Reserve `0000` values; do not immediately reuse revoked addresses, and reject credentials from prior enrollment generations.
- [ ] Resolve an address only inside the authenticated radio-network namespace and dimension.
- [ ] Custom addresses have no automatic Internet, cross-dimension or cross-network route. Any future gateway is explicit and permission controlled.
- [ ] A service directory maps a radio address plus service name to an authorized endpoint and its compatible transport.
- [x] Validate length, alphabet, namespace, lifecycle and malformed addresses before scheduling any traffic.

The ORA codec and registry are implemented for tower bridge endpoints. The public send-to-ORA service and robot/tablet endpoint integration remain pending.

## 4. Enrollment, identity and permissions

- [x] Network owner approves enrollment from a PC via the tower RPC (`listPendingEnrollments`/`approveEnrollment`/`denyEnrollment`, approval-gated invites, bounded and expiring requests); proximity alone and knowledge of a UUID do not grant access. A graphical list remains future work.
- [ ] Issue per-device credentials using a cryptographically secure source; authenticate sessions against server-owned enrollment records.
- [x] Support separate telemetry-read, command-send and network-admin permissions, scoped to devices/services (`RadioRegistry.Permission`/`Scope`; the broker rechecks them on send, delivery, subscription and command completion).
- [ ] Enforce network membership/routing and host operations on the server; the service endpoint separately authorizes subscriptions and commands before execution. Encrypted relays need not inspect application payloads.
- [ ] Allow individual revocation, reconnect, re-enrollment and key rotation without resetting other clients.
- [ ] Define item cloning, transfer, drop, death, logout and dimension-change behavior. Duplicated item data must not create a second authenticated identity.
- [ ] Keep secrets out of logs, widget definitions, public discovery, addresses and telemetry payloads.
- [ ] Rate-limit enrollment and failed authorization; return bounded, explicit errors.

## 5. Latency, TTL and TLS are separate features

### Server delivery schedule and ping

- [x] Schedule delivery on the authoritative server tick queue, never by blocking or sleeping a tick thread.
- [x] Configurable base delay of one tick per radio hop, capped by `radioMaxDeliveryTicks` (default 2 ticks) so distance does not add latency.
- [x] Per-sender link bandwidth (`radioLinkBytesPerTick`, default 1024 B/tick, about 20 KB/s) for Ethernet frames and ORA messages.
- [ ] Account for ingress/egress radio legs, relay hops and packet size under a configured link bandwidth budget.
- [ ] Preserve packet ordering within a reliable stream; drop expired packets and cancel stale scheduled deliveries after topology/auth changes.
- [ ] Display hop count, estimated one-way delivery time and measured ping RTT as separate values.
- [ ] One tick is nominally 50 ms at 20 TPS. Label estimates accordingly; measure actual RTT with a monotonic clock.
- [ ] Do not claim millisecond delivery precision from a tick scheduler or mix wall-clock deadlines with tick ordering.
- [x] Use bounded per-device, per-network and global queues, finite payload/frame size and backpressure.
- [ ] Expire or reject commands to offline recipients. Never replay an old movement command automatically when a robot reconnects.

### Hop TTL and duplicate suppression

- [x] Give each packet an identifier and bounded hop TTL, and decrement it at each relay.
- [ ] Suppress duplicate delivery, looped broadcasts and duplicate command execution with bounded expiring state.
- [x] Keep a separate packet lifetime/deadline; hop TTL is not time in milliseconds.
- [ ] Bound service discovery and broadcasts so a ring of towers cannot multiply traffic without limit.
- [ ] Define route-change/retry behavior and idempotent request IDs across reconnections.

### TLS

- [ ] Use a maintained TLS 1.3 implementation for reliable application streams when the chosen endpoints support it.
- [ ] Authenticate the intended endpoint using enrolled trust information; confidentiality is not a substitute for permissions.
- [ ] Use unique device identities or scoped PSKs rather than a shared network-wide impersonation key.
- [ ] Do not implement custom encryption or call server-side permission checks alone TLS.
- [ ] Identify the cryptographic RNG, trust provisioning, credential storage and host/guest endpoint boundaries before implementation.
- [ ] Disable 0-RTT for commands with side effects; check replay and reconnect behavior.
- [ ] Keep public Internet certificate/DNS assumptions out of local radio enrollment.
- [ ] Existing Onyx ohttp/obrowse report no TLS support. Audit guest TLS/entropy/build compatibility before promising encrypted Onyx connections.

TLS reference: [RFC 8446](https://www.rfc-editor.org/rfc/rfc8446.html).
The first transport milestone may use an explicitly trusted, local test backend. It must report that backend's security state honestly.
A secure remote-control acceptance milestone requires the authentication and encryption work above.

## 6. Shared service and message contract

- [ ] A message carries a protocol version, source/destination identity, service/channel, request ID and bounded typed payload.
- [ ] Transport scheduling/authentication metadata comes from the server, not untrusted UI payloads.
- [ ] PCs publish named telemetry fields; clients subscribe to selected fields and receive snapshots plus ordered updates.
- [ ] Show last-update age and online/stale/offline state; stale samples must not look live.
- [ ] Commands have distinct accepted, running, completed, failed, timed-out and disconnected outcomes.
- [ ] A transport acknowledgement is not confirmation that a robot moved or an inventory operation succeeded.
- [ ] Use request IDs/idempotency handling for retries, and explicit deadlines for side effects.
- [ ] Existing Linux/Onyx applications retain TCP/UDP compatibility; the simple widget service sits above the transport.
- [ ] Define version negotiation, framing, bounds, error codes and subscription cleanup before connecting independent implementations.

## 7. Tablet: a pocket mini computer running OnyxOS

Owner decision, 2026-11-05 (replaces the earlier "lightweight host runtime, not an emulated RISC-V computer"):
the tablet is a pocket computer that runs the standard OnyxOS in a small virtual machine, like a computer or robot with
less power. The full design and the contract for software authors are in [../TABLET.md](../TABLET.md).
Widget code stays in the Onyx repositories and runs inside the guest; there is no separate Java widget runtime.

- [ ] Tablet item with a small virtual machine. Slots: one CPU and one disk/flash item. RAM and screen are fixed at crafting
  (first targets: 256 x 160 screen, 16 MiB RAM, CPU clock capped to a quarter of a PC). No energy use.
- [ ] Machine state is stored in world data under the item's UUID; the item itself stays small.
- [ ] The machine runs only while the tablet is in hand or its screen is open; otherwise it is suspended with its state kept.
  Drop, death and logout suspend without data loss. No background work in the first version.
- [ ] Small live screen in hand; right-click opens the large screen. The full QWERTY keyboard and the mouse go to the guest,
  so any program can use typing, shortcuts and clicks. Input is released with `CaptureKeyBinding.RELEASE` (Right Ctrl, rebindable);
  reuse that binding, do not add another.
- [ ] Reflashing: take the disk or flash out of the tablet, write it in the flash writer (bundled images or a file), put it back.
  The default tablet ships with a flash carrying the tablet build of OnyxOS. A tablet without a disk does not boot.
- [ ] The tablet attaches to a tower or dock station as a radio device, is admitted by an approved invitation and uses the
  guest device bus (`/dev/hvc0`) like computers do, so `ochat` and later tablet applications work unchanged.
- [ ] Developer documentation for C applications, Lua scripts and replacement operating systems stays current in docs/TABLET.md.
- [ ] A portable widget/state core separate from framebuffer, Minecraft GUI, network and clock adapters.
- [ ] Initial widgets: text, button, progress/status indicator, then bounded graphs and lists.
- [ ] Define a widget by its placement, source device/service/field and optional authorized action.
- [ ] Ready-made dashboard examples for energy, storage and robot status; reusable presets rather than protocol code per widget.
- [ ] Data binding handles updates, stale/offline states, missing fields and permission errors.
- [ ] Pointer hit testing, clipping, scaling and button press/release must work predictably.
- [ ] Add UTF-8 text and host font/fallback handling for localized dashboards; the first demo font is printable ASCII only.
- [ ] Short Lua scripts may format values and process UI events; bound instructions, memory and event/command frequency. First scope (owner, 2026-11-04): a minimal example implementation, not a full scripting platform.
- [ ] Expose a small explicit host API. No unrestricted host files, Minecraft internals or arbitrary remote code execution.
- [ ] Heavy calculations and equipment programs execute on PCs; tablet programs show connection loss instead of stale data.
- [ ] UART remote terminal remains an app/mode on this item; revisit the earlier wiring minigame only as its own interaction.

## 8. Implementation order and acceptance

1. [x] Portable widget/data-binding/pointer core in the Onyx component repositories with an injectable backend and graphical renderer boundary.
2. [x] Host/native tests and a runnable demonstration with clearly labeled mock telemetry and command results.
3. Tower data bridge and routing: two PCs exchange bounded messages over a relay, including an unloaded relay chunk.
4. Stable identities, short-address registry, enrollment, permissions and revocation.
5. Server scheduling, hop TTL, duplicate suppression, queue bounds, ping and timeout UI.
6. Authenticated TLS-capable application transport and real PC telemetry/command service.
7. Robot attachment/roaming and authoritative task results.
8. Minecraft tablet item (mini computer with OnyxOS), screen and input, reflashing, and live radio binding.
9. Sandboxed script hooks, dashboard presets, persistence and multiplayer lifecycle checks.

Test acceptance includes circular radius boundaries and extreme vertical separation; isolated networks; broken alternate routes;
chunk unload/reload; address collision/exhaustion/reassignment; spoofed identity and revoked credentials; packet loops and queue saturation;
real versus nominal RTT under reduced TPS; no repeated side effects after reconnect; stale/offline widgets; clipping and malformed UI input;
script budgets; two simultaneous players; and save/reload of identities and tablet state.

The first Onyx implementation must not be presented as finished Minecraft radio, TLS or a complete tablet OS.

## 9. First implementation result

OnyxApps commit `fcf5e93` adds `apps/otablet` as the first portable C99 increment:
labels, buttons, progress/status, bounded data binding, pixel pointer hit testing,
freshness/offline state, clipped RGB565 rendering, an injected mock backend and a persistent demo.
Native tests, sanitizer checks, OnyxCC target build and RV64 onx-run execution passed;
native and RV64 PPM output matched. Core storage is fixed-size and the demo text font is ASCII.

The demo produces graphical PPM frames and offers an optional Python/Tk mouse preview.
The Tk window was not exercised on this workstation because `libtk8.6.so` is missing.
There is no real Onyx framebuffer/mouse frontend, Minecraft item/GUI adapter, Lua binding,
live telemetry connection, radio transport or TLS in this commit. Those remain unchecked above.

## 10. Tower bridge and graphical frontend increment

The working branch now contains portable ORA allocation/authentication, hash-only registry snapshots,
circular routing with an eight-source bounded BFS cache, and a tick delivery queue with TTL,
deadlines, duplicate detection, enrollment-generation revalidation and admission limits.
Minecraft GPS towers expose separate data-radio enrollment RPC and an Ethernet capability.
The first bridge uses trusted local plaintext; TLS and end-service permissions are not implemented.
Invitations are one-use and expire after `radioInviteLifetimeTicks`. Address allocation is dimension-scoped.
Pending packets do not survive restart. Source/target endpoints must be loaded; saved relays may be unloaded.

Validation: 9 portable radio tests, 3 SavedData round-trip/identity tests, 6 platform compile targets,
and 86 NeoForge 1.21.1 plus 88 NeoForge 26.1.2 GameTests, including delayed radio-to-cable delivery and rejected guessed enrollment.
These tests do not constitute a booted guest TCP/TLS acceptance run or the complete wireless-client milestone.

OnyxApps commit `cc0d974` adds `--device` framebuffer/mouse output, bounded declarative panel files
and a portable asynchronous session adapter with request IDs, explicit results, deadlines and disconnect cancellation.
Native tests, sanitizer checks, OnyxCC build and RV64 snapshot/portable tests passed.
The device ABI is tested with mock system calls; actual framebuffer/mouse hardware execution remains unverified.
No Lua, real telemetry connection, Minecraft item or TLS adapter is present yet.

Owner clarification: ORA prefixes and device numbers are random, not sequential. Existing addresses remain stable; legacy allocation history and persisted reservations prevent reuse after revocation or re-enrollment.

Random-allocation follow-up: 15 core radio tests and 6 persistence tests on each of NeoForge 1.21.1 and 26.1.2 passed. Snapshot storage now writes UTF-8 byte arrays while reading legacy NBT strings; an actual binary NBT round trip above 64 KiB protects large reservation histories.
