# New peripherals — owner decisions, 2026-11-02

The wireless access point and environment sensor are implemented; the other entries remain planning. The new-device list contains only the eight directions selected by the owner; no additional devices are scheduled. Wireless access points are first; the remaining order is provisional. Mechanics below distinguish the owner's requested direction from suggested details. See [PERIPHERALS.md](../PERIPHERALS.md) for the implemented hardware and RPC.

## 1. Wireless access point

Initial planning proposed: an access point bridges the existing wired Ethernet network to nearby access points in the same dimension. Computers retain their existing network card and Linux TCP/IP tools; this first block does not require a new guest driver. Connect a network connector to each access point to bridge two wired segments.

Owner-approved update, 2026-11-03: [tablet-radio.md](tablet-radio.md) is the canonical radio/tablet scope and acceptance plan. It supersedes the previous 64-block/shared-key/direct-link-only MVP and any height bonus.

The robot dock station (existing charger) is a short-range access point covering 10 blocks, with no separate station block, and towers cover 45 blocks in a circular X/Z radius with unrestricted Y. Towers relay over radio, including saved unloaded relay nodes. Per-device approved enrollment, short radio addresses, scoped permissions, tick-scheduled latency, hop TTL and TLS-capable application streams are separate work items. Existing Ethernet and TCP/IP compatibility remain required.

Acceptance includes authorized wired endpoints exchanging frames over radio, isolation by network/dimension, bounded loops and queues, revocation, radio range boundaries, chunk unload/removal/reload reconciliation, and save/reload of stable identities. Full details and implementation order are in the canonical plan.

## 2. Printer

Proposed design, subject to agreement before implementation. Text first: paper + black dye produce a titled page item. A program opens a page, writes text at a bounded cursor position, then commits it. An unfinished page survives world reload; committing consumes materials once and only when the output slot can accept the result. Inventory automation feeds paper/dye and extracts pages. Pages can be collected into a readable book while retaining order. No screenshot/raster printing in the first version.

Use one shared page/job backend for RPC and the native interface described in devices-storage §50. Define page dimensions, Unicode policy and ink cost before implementing. Test full output, interrupted jobs, save/reload and repeated commit requests.

## 3. Positioning beacon (OC2R navigation)

A physical reference station advertises its ID, dimension, configured site name and known world coordinates, and answers range requests over the OC2R wireless network. A navigation program gathers multiple stations and computes position; one station can provide a named destination and distance without pretending it determines a unique 3D location. Provide a reference Linux service/client and robot navigation examples. Do not require four full computers just to host four beacons. No administrator omniscient fallback in the normal survival mechanic. Define coordinate frames before supporting ships/contraptions.

## 4. Environment sensor

Implemented as `oc2r:environment_sensor`: a wall-mounted bus peripheral reports local biome, light, time, weather and temperature data with documented units and a 20-tick sampling interval. Only query loaded surroundings. Events report threshold changes; avoid a per-tick scan of every attached sensor. It is not a replacement for the robot scanner module.

## 5. Document reader

An inventory peripheral reads books and printed pages into guest text, retaining page order and title/author where present. Start with Minecraft book components and the printer's page format. No OCR or arbitrary client filesystem access. Feed documents using normal inventory automation.

## 6. Media player and writable music discs

The owner-approved workflow is PC file import (for example MP3) -> record a physical custom music disc -> play it in the media player. Retain ordinary Minecraft disc playback too. Reuse the existing speaker/sound path for custom audio. Specify bounded import duration/size, decoding, world-local audio blob persistence, multiplayer synchronization and disc copying/deletion semantics before implementation. A disc stores a stable media reference, not megabytes of audio in its item data. Imported audio must survive world/server restart. This is a separate feature from projector jcodec removal.

## 7. Long-range radio antenna

The existing GPS tower is the selected long-range radio hardware for the first implementation. Its recipe and 45-block circular X/Z range are implemented for GPS connectivity; data relaying is pending. Height and open sky do not affect coverage. See [tablet-radio.md](tablet-radio.md). No additional antenna block or new cable tiers are required by this decision.

## 8. Creative command chip

Use an installable computer chip/card, not a separate command computer block. It enables a documented command API only while installed. The item is creative/admin-only, has no survival recipe and is disabled by default in server configuration. Server-side authorization and an explicit command permission ceiling remain mandatory: inserting an item must not turn an arbitrary creative player or guest VM into a server operator. Specify execution identity, audit logging, rate limits and revocation on removal/reload.

## Existing hardware — avoid duplicates

- **Robot charger already exists:** `ChargerBlockEntity` charges neighboring blocks and nearby entities. The charger doubles as the robot dock station and gains a 10-block radio access point (see tablet-radio.md section 2); no separate dock block is scheduled.
- **Redstone relay already exists in substance:** the redstone interface block exposes analog/bundled input and output and change events over the device bus. Reuse and document it instead of adding a second block with the same purpose.

Status: the wireless access point and environment sensor have been implemented following the 2026-11-09 request. The document reader, writable audio discs/media player, navigation extensions and creative command chip remain planned. The existing tower remains the selected long-range hardware. No release tag or platform upload is included.

The 2026-11-03 radio/tablet decisions and initial Onyx widget implementation supersede earlier planning-only and unfinalized radio-default statements above. Other peripheral proposals retain their existing status.

2026-11-09 implementation: `wireless_access_point` uses the supplied modem model with a bottom-only Ethernet port, owner-approved enrollment, 10-block client coverage and an own-network tower uplink. `environment_sensor` uses the supplied wall model and emits bounded environment-change events. See [PERIPHERALS.md](../PERIPHERALS.md).
