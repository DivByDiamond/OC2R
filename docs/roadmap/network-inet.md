# Roadmap: network and internet card

TCP/IP stack (`common/inet/`), internet card, VXLAN, security hardening and network performance. The larger wire/tower rework is in
[network-overhaul.md](network-overhaul.md). Closed items are in [done.md](done.md).

## 27. Mod subsystem audit

Goal: systematically look over the remaining OC2R subsystems (what works, what is off, what to fix). Checklist without results, one line each:

- [ ] Network: `NetworkSwitch`, `NetworkConnector`, `InternetCard`, `VXLAN Hub`, TCP/IP stack; multiplayer stability.
- [ ] `inet/` TCP/IP stack: `StreamSessionImpl`, `SessionManager`, retransmission quality.
- [ ] PCI Card Cage: `PciCardCageDevice` (16 MB window), does it extend slots correctly.
- [ ] Redstone Interface: `RedstoneInterfaceBlockEntity`, `BundledRedstoneCallbacks` (edges, weak signal).
- [ ] Block energy: `FixedEnergyStorage`, charger, `consumeEnergy` (see section 13 in [done.md](done.md#13-wire-rewrite-fe-and-eu-energy-connection-fix)).
- [ ] GUI/containers: `ComputerInventoryContainer`, `AbstractMachineInventoryScreen` slot sync bugs.
- Robot audit: done, moved to [robot.md](robot.md#27-audit-rows-robot); world sync audit: done, see [29](cables-bus-energy.md#29-world-sync-fixes).

### Findings of the inet/ audit (2026-08-21, from issue #13)

A reporter could not get internet from the VM (ping 1.1.1.1, DNS via 8.8.8.8 silently fail). Verified on branch work, HEAD 6518289: all 5 code findings confirmed.
Likely root cause: items 3+4 combined (ICMP fallback `isReachable()` false negative without CAP_NET_RAW, DNS failing on silently dropped fragmented UDP replies,
EDNS0 > 512 bytes), plus possible misconfiguration: no DHCP, the guest must assign itself an IP (the card is a point-to-point link).

Fixed (2026-08-21): `assert false;` in `DefaultSessionLayer.java:112` removed; ICMP Destination Unreachable / Time Exceeded on dropped packets (`DefaultNetworkLayer.queueIcmpError`,
type 3 code 4 for fragments with MTU 1500, type 11 code 0 for TTL=1, delivered on the next receive poll; `deniedHosts` stays a silent drop); fragmented IP packets no longer silently dropped
(no reassembly, ICMP frag-needed only); one-time WARN on the first failed ICMP fallback (`EchoHandler.java`); `InternetManagerImpl.initialize()` warns "internet card is enabled but VXLAN is disabled".
Bugs found while writing tests (all fixed): `MacAddressUtils` sign extension (`MacAddressUtilsTest`); `InetUtils.quickICMPBody` `put()` instead of `get()`; `IcmpHandler.reject` source
address 0.0.0.0 (documented by `IcmpHandlerTest`); Mockito 4.3.1 -> 5.17.0 (byte-buddy 1.12.7 does not support Java 21). Total +29 unit tests (152 in total, green).

- [ ] **`IcmpHandler.reject` source address 0.0.0.0**: still needs a fix: pass the unreachable host address as src (behavior is only documented by the test).
- [ ] **User documentation**: no DHCP, the card is a point-to-point link (the guest assigns any IP, the card answers ARP). Write it in the README/docs with a `deniedHosts` warning.
- [ ] Answer issue #13 after the ping/DNS fixes and attach the instructions from the previous item.

## 38. Performance audit: network, monitors, bus

Audit 2026-08-25. Symptom from a player: "Network performance is really bad still". Eight bottlenecks found. Internet card ceiling was about 13 to 30 KB/s with collapse on losses;
after fixes 1 to 3 hundreds of KB/s up to MB/s are reachable. Almost everything is closed: see [done.md](done.md#38-closed-items). Video items are in
[video-gpu.md](video-gpu.md#38-video-items).

Open network/sync items:
- [ ] Monitor/projector `hasEnergy` lives ONLY in `*StateMessage` (`isMounted` is duplicated as LIT + tag, cleaning would touch render/container): low priority.
- [ ] VS2-fallback of `sendToClientsTrackingChunk` broadcasts to all players in all dimensions. A naive dimension filter is NOT possible (players are physically not in the ship world -> 0 recipients);
  needs a ship-chunk -> parent-level mapping through the VS2 API or a radius heuristic: a separate decision. (Related: L12 of section 47, closed for the non-VS2 path.)
- [ ] Multi-buffer receiver for the internet card (`processQueue` early exit is intentional: one session/buffer per `receiveSession` call, draining several sessions would lose data). Further gain only this way.
- [ ] Optional: speed up the checksum with a `getLong` pass (low priority).

Implementation priority list of the audit (historical): P1+P2+P3, then V1+V2, then Sh1+Sh2, then T1 and sync duplicates, then Sh3-Sh7 and V4-V9, then P4-P7, V10, Sh8.

## 39. Security hardening of inet, linter configs, libraries

Audit 2026-08-25. Lint stages of this section are in [quality.md](quality.md#39-lint-stages); closed items are in [done.md](done.md#39-closed-items).

### Security items (open)

- [ ] **C1: VXLAN, unauthenticated incoming UDP -> frame injection into the world** `[vxlan/TunnelManager.java:85-121]`: the socket is not connected to remoteHost, the source is not checked; port 4789 is
  standard VXLAN. Anyone able to send UDP to the server port (bind 0.0.0.0 = internet) injects arbitrary Ethernet frames into any computer's virtual network. Fix: `socket.connect()` + check
  `packet.getAddress()` + pre-shared key/HMAC; VNI is not the only id.
- [ ] **C2: VXLAN, vti=1000 hard-coded for all blocks and loaded from NBT without checks** `[VxlanBlockEntity.java:32,109-111]`: all hubs of a server register one VNI, `tunnels.put()` overwrites another tunnel
  (player isolation broken); forged NBT joins someone else's network. Fix: random vti at creation (from the item UUID), range validation on load.
- [ ] **C3: guest spoofing of srcIpAddress** `[DefaultNetworkLayer.java:117-137]`, `SendHandler.java:69-105`: only dst is checked; the guest assigns any IP (`DefaultLinkLocalLayer.java:162`). Through the host's real
  internet: spoofing attacks / server IP impersonation. Fix: compare src with the ARP-learned card address, drop on mismatch. Note: C3 is claimed closed by the ARP-claim filter in C8 (see
  [done.md](done.md#39-closed-items)) but this needs confirmation.
- [ ] **C4: `deniedHosts` lacks 169.254.0.0/16** (`InternetCardSpec.java:62-68`): on a public VPS the guest reads cloud metadata (IAM tokens) via 169.254.169.254. Also add 0.0.0.0/8, 255.255.255.255/32, TEST-NET
  ranges. Hostname entries are resolved once at startup (`Ipv4Space.java:131-146`), so DNS rebinding of the config applies: document and recommend CIDR.
- [ ] Resource limits missing: bandwidth per card/tick, number of cards per player (`InternetManagerImpl.connect:76`), rate limit for ICMP/PCM/framebuffer (PCM and C2S limits done, see done.md).

### Libraries

Keep ceres/sedna/sedna-buildroot/markdownmanual(+architectury) (used deeply: sedna in 54 files, ceres 20, markdownmanual = all in-game docs `client/manual/`). Native oc2rnet: no sources in the repo
(binaries 112 KB x 8 platforms are downloaded); needed for exactly one method `sendICMP` (TCP/UDP are already pure Java NIO), fallback `isReachable` exists; keep, optionally remove in about a day if the goal is a
binary-free repo. jcodec recommendation is implemented via [section 40](video-gpu.md#40-removing-jcodec-full-usage-list).
- [ ] Version drift: `gradle.properties` `sedna_buildroot_version=0.0.70` vs `download-libs.sh` downloads 0.0.72-oc2r1 (needed for `CONFIG_9P_FS` / issue #17): raise the property (5 minutes).

### 47 network items

Closed network items of the 2026-09-17 audit (B2 InternetManagerImpl.tasks, B4 Ipv4Space/IntegerSpace unsigned ranges, B5 FrameChunker, L1/L2, L8, L9, L12 and the JSON deserializer fix L11) are in
[done.md](done.md#47-closed-items). Open items of the audit are in [quality.md](quality.md#47-audit-2026-09-17).
Open network-related audit items (P3 SocketManager, KB-4 forged sender test) are tracked in quality.md.
